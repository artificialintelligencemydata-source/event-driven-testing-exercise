package com.acuver.autwit.internal.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.*;
import org.testng.xml.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Clean Enterprise Retry Executor.
 * - Retries SKIPPED test methods only
 * - Integrates with TestNGListener + DB ResumeEngine
 * - No duplicated methods
 */
public class RetrySkippedTestsExecutor implements IExecutionListener {

    private static final Logger LOG = LogManager.getLogger(RetrySkippedTestsExecutor.class);

    private static final int MAX_RETRIES = 3;
    private static final Map<String, Integer> retryCount = new ConcurrentHashMap<>();

    @Override
    public void onExecutionFinish() {

        LOG.info("🔁 Starting RetrySkippedTestsExecutor...");

        List<ITestResult> skipped =
                TestNGListener.skippedTests.stream()
                        .filter(r -> r.getStatus() == ITestResult.SKIP)
                        .collect(Collectors.toList());

        if (skipped.isEmpty()) {
            LOG.info("No skipped tests → no retry needed.");
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            Map<Class<?>, List<ITestNGMethod>> grouped = groupRetryMethods(skipped, attempt);

            if (grouped.isEmpty()) {
                LOG.info("No more retryable methods (attempt={})", attempt);
                break;
            }

            LOG.info("🔁 RETRY attempt {} — Methods: {}", attempt, grouped.size());
            runRetrySuite(attempt, grouped);

            // Refresh skipped list
            skipped = TestNGListener.skippedTests.stream()
                    .filter(r -> r.getStatus() == ITestResult.SKIP)
                    .collect(Collectors.toList());

            if (skipped.isEmpty()) {
                LOG.info("🎉 All skipped tests passed after retry");
                break;
            }
        }

        LOG.info("Retry phase completed.");
    }

    /** Group skipped test methods by class for retry execution */
    private Map<Class<?>, List<ITestNGMethod>> groupRetryMethods(List<ITestResult> skipped, int attempt) {

        Map<Class<?>, List<ITestNGMethod>> grouped = new HashMap<>();

        for (ITestResult r : skipped) {
            String key = TestNGListener.getTestKey(r);

            int count = retryCount.getOrDefault(key, 0);
            if (count >= MAX_RETRIES) continue;

            retryCount.put(key, count + 1);

            grouped.computeIfAbsent(
                            r.getMethod().getTestClass().getRealClass(),
                            c -> new ArrayList<>())
                    .add(r.getMethod());
        }
        return grouped;
    }

    /** Executes retry XML suite */
    private void runRetrySuite(int attempt, Map<Class<?>, List<ITestNGMethod>> grouped) {

        XmlSuite suite = new XmlSuite();
        suite.setName("RetrySuite-Attempt-" + attempt);
        suite.setParallel(XmlSuite.ParallelMode.METHODS);
        suite.setThreadCount(4);

        XmlTest test = new XmlTest(suite);
        test.setName("RetryTests-Attempt-" + attempt);

        List<XmlClass> classes = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<ITestNGMethod> methods = entry.getValue();

            XmlClass xmlClass = new XmlClass(clazz.getName());

            List<XmlInclude> includes =
                    methods.stream()
                            .map(m -> new XmlInclude(m.getMethodName()))
                            .collect(Collectors.toList());

            xmlClass.setIncludedMethods(includes);
            classes.add(xmlClass);
        }

        test.setXmlClasses(classes);

        LOG.info("▶ Running retry suite:\n{}", suite.toXml());

        TestNG ng = new TestNG();
        ng.setXmlSuites(Collections.singletonList(suite));
        ng.addListener(new TestNGListener());
        ng.setUseDefaultListeners(true);
        ng.run();
    }
    public void executeRetries() {
        onExecutionFinish();
    }
}
