package com.acuver.autwit.internal.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.*;
import org.testng.xml.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RetrySkippedTestsExecutor - Handles in-memory skipped test retries ONLY.
 *
 * <h2>RESPONSIBILITY</h2>
 * <ul>
 *   <li>Retry SKIPPED tests tracked in-memory by TestNGListenerNew</li>
 *   <li>Execute retries via programmatic TestNG</li>
 * </ul>
 *
 * <h2>IMPORTANT: NO DB RESUME LOGIC</h2>
 * <p>DB-based resume is handled EXCLUSIVELY by {@code ResumeScheduler}.
 * This component does NOT query the database for resumeReady scenarios.</p>
 *
 * <h2>WHY SEPARATED</h2>
 * <ul>
 *   <li>Single responsibility: in-memory retries only</li>
 *   <li>Avoids duplicate resume triggers</li>
 *   <li>Prevents race conditions with ResumeScheduler</li>
 *   <li>Clear ownership boundaries</li>
 * </ul>
 *
 * <h2>CONFIGURATION</h2>
 * <pre>
 * autwit:
 *   retry:
 *     enabled: true       # Enable in-memory retry
 *     max-retries: 3      # Maximum retry attempts
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 * @see ResumeScheduler for DB-based resume orchestration
 */
@Component
public class RetrySkippedTestsExecutor implements IExecutionListener {

    private static final Logger log = LogManager.getLogger(RetrySkippedTestsExecutor.class);

    @Value("${autwit.retry.max-retries:3}")
    private int maxRetries;

    @Value("${autwit.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${autwit.retry.thread-count:4}")
    private int threadCount;

    /** Track retry counts per test key - thread-safe */
    private static final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    /**
     * Default constructor for TestNG listener instantiation.
     * Values will be overridden by Spring if component-scanned.
     */
    public RetrySkippedTestsExecutor() {
        this.maxRetries = 3;
        this.retryEnabled = true;
        this.threadCount = 4;
    }

    @Override
    public void onExecutionStart() {
        log.debug("TestNG execution started - clearing retry counts");
        retryCount.clear();
    }

    @Override
    public void onExecutionFinish() {
        if (!retryEnabled) {
            log.debug("Retry is disabled via configuration - skipping");
            return;
        }

        log.info("🔁 Starting RetrySkippedTestsExecutor (in-memory only)...");
        log.info("   ℹ️  Note: DB-based resume is handled by ResumeScheduler, not here.");

        // ONLY get skipped tests from in-memory tracking
        // NO DB resume logic - that's ResumeScheduler's exclusive responsibility
        List<ITestResult> skippedFromMemory = getSkippedFromMemory();

        if (skippedFromMemory.isEmpty()) {
            log.info("✅ No skipped tests in memory to retry");
            return;
        }

        log.info("📋 Found {} skipped test(s) in memory", skippedFromMemory.size());

        // Execute retry loop
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Map<Class<?>, List<ITestNGMethod>> grouped = groupRetryMethods(skippedFromMemory, attempt);

            if (grouped.isEmpty()) {
                log.info("No more retryable methods (attempt {}/{})", attempt, maxRetries);
                break;
            }

            int methodCount = grouped.values().stream().mapToInt(List::size).sum();
            log.info("🔁 RETRY attempt {}/{} — {} method(s) across {} class(es)",
                    attempt, maxRetries, methodCount, grouped.size());

            runRetrySuite(attempt, grouped);

            // Refresh skipped list for next iteration
            skippedFromMemory = getSkippedFromMemory();

            if (skippedFromMemory.isEmpty()) {
                log.info("🎉 All skipped tests passed after {} retry attempt(s)", attempt);
                break;
            }

            log.info("   Still {} skipped test(s) remaining after attempt {}",
                    skippedFromMemory.size(), attempt);
        }

        // Final status
        List<ITestResult> remaining = getSkippedFromMemory();
        if (remaining.isEmpty()) {
            log.info("✅ Retry phase completed - all tests passed");
        } else {
            log.warn("⚠️ Retry phase completed - {} test(s) still skipped after {} attempts",
                    remaining.size(), maxRetries);
            for (ITestResult r : remaining) {
                log.warn("   - {}", TestNGListenerNew.getTestKey(r));
            }
        }
    }

    /**
     * Get skipped tests from in-memory TestNGListenerNew tracking.
     *
     * <p><b>NOTE:</b> This does NOT check the database. DB resume is handled
     * exclusively by ResumeScheduler to avoid duplicate triggers and race conditions.</p>
     *
     * @return List of skipped test results from memory
     */
    private List<ITestResult> getSkippedFromMemory() {
        return TestNGListenerNew.skippedTests.stream()
                .filter(r -> r.getStatus() == ITestResult.SKIP)
                .filter(r -> !TestNGListenerNew.passedTestKeys.contains(TestNGListenerNew.getTestKey(r)))
                .filter(r -> !TestNGListenerNew.failedTestKeys.contains(TestNGListenerNew.getTestKey(r)))
                .collect(Collectors.toList());
    }

    /**
     * Group skipped test methods by class for retry execution.
     * Respects max retry limit per test.
     */
    private Map<Class<?>, List<ITestNGMethod>> groupRetryMethods(List<ITestResult> skipped, int attempt) {
        Map<Class<?>, List<ITestNGMethod>> grouped = new HashMap<>();

        for (ITestResult r : skipped) {
            String key = TestNGListenerNew.getTestKey(r);

            int count = retryCount.getOrDefault(key, 0);
            if (count >= maxRetries) {
                log.debug("Max retries ({}) reached for: {}", maxRetries, key);
                continue;
            }

            retryCount.put(key, count + 1);

            Class<?> testClass = r.getMethod().getTestClass().getRealClass();
            grouped.computeIfAbsent(testClass, c -> new ArrayList<>()).add(r.getMethod());
        }

        return grouped;
    }

    /**
     * Execute retry XML suite programmatically.
     */
    private void runRetrySuite(int attempt, Map<Class<?>, List<ITestNGMethod>> grouped) {
        XmlSuite suite = new XmlSuite();
        suite.setName("AUTWIT-RetrySuite-Attempt-" + attempt);
        suite.setParallel(XmlSuite.ParallelMode.METHODS);
        suite.setThreadCount(threadCount);

        XmlTest test = new XmlTest(suite);
        test.setName("RetryTests-Attempt-" + attempt);

        List<XmlClass> classes = new ArrayList<>();

        for (Map.Entry<Class<?>, List<ITestNGMethod>> entry : grouped.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<ITestNGMethod> methods = entry.getValue();

            XmlClass xmlClass = new XmlClass(clazz.getName());

            List<XmlInclude> includes = methods.stream()
                    .map(m -> new XmlInclude(m.getMethodName()))
                    .distinct()
                    .collect(Collectors.toList());

            xmlClass.setIncludedMethods(includes);
            classes.add(xmlClass);
        }

        test.setXmlClasses(classes);

        log.debug("▶ Running retry suite:\n{}", suite.toXml());

        try {
            TestNG ng = new TestNG();
            ng.setXmlSuites(Collections.singletonList(suite));
            ng.addListener(new TestNGListenerNew());
            ng.setUseDefaultListeners(true);
            ng.run();
        } catch (Exception e) {
            log.error("❌ Error during retry execution: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for retry execution.
     * Can be called programmatically to force immediate retry.
     */
    public void executeRetries() {
        log.info("Manual retry execution triggered");
        onExecutionFinish();
    }

    /**
     * Get current retry count for a test key.
     *
     * @param testKey Test key (from TestNGListenerNew.getTestKey())
     * @return Current retry count (0 if not retried yet)
     */
    public int getRetryCount(String testKey) {
        return retryCount.getOrDefault(testKey, 0);
    }

    /**
     * Check if a test has exceeded max retries.
     *
     * @param testKey Test key
     * @return true if max retries reached
     */
    public boolean hasExceededMaxRetries(String testKey) {
        return retryCount.getOrDefault(testKey, 0) >= maxRetries;
    }

    /**
     * Reset all retry counts.
     * Useful for testing or manual intervention.
     */
    public void resetRetryCounts() {
        int size = retryCount.size();
        retryCount.clear();
        log.info("Reset {} retry count entries", size);
    }

    /**
     * Get summary of retry counts.
     *
     * @return Map of test key to retry count
     */
    public Map<String, Integer> getRetryCountSummary() {
        return new HashMap<>(retryCount);
    }
}