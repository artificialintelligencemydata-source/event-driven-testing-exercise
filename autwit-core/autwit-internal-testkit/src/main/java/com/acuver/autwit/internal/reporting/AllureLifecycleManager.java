package com.acuver.autwit.internal.reporting;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AllureLifecycleManager - Centralized Allure lifecycle management for AUTWIT.
 *
 * <h2>RESPONSIBILITY</h2>
 * <ul>
 *   <li>Synchronize Allure test lifecycle with Cucumber scenario lifecycle</li>
 *   <li>Provide safe attachment methods that verify test context exists</li>
 *   <li>Ensure proper start/stop of test cases for accurate reporting</li>
 *   <li>Cleanup orphan test contexts on shutdown</li>
 * </ul>
 *
 * <h2>USAGE</h2>
 * <pre>
 * // In Hooks.java @Before(order = 0)
 * allureLifecycle.startTestCase(scenarioName, scenarioId);
 *
 * // In step definitions (via Autwit facade or directly)
 * allureLifecycle.attachSafely("Response", jsonContent, "application/json");
 *
 * // In Hooks.java @After(order = 0)
 * allureLifecycle.stopTestCase(Status.PASSED);
 * </pre>
 *
 * <h2>THREAD SAFETY</h2>
 * Uses ThreadLocal for test UUID and ConcurrentHashMap for active test tracking.
 * Safe for parallel Cucumber execution with proper isolation.
 *
 * <h2>CLEANUP STRATEGY</h2>
 * Implements @PreDestroy to clean up orphan test contexts that weren't properly
 * stopped due to abnormal termination.
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
@Component
public class AllureLifecycleManager {

    private static final Logger log = LogManager.getLogger(AllureLifecycleManager.class);

    /** Thread-local storage for current test UUID */
    private static final ThreadLocal<String> currentTestUuid = new ThreadLocal<>();

    /** Track active test cases for verification and cleanup (thread-safe) */
    private static final ConcurrentHashMap<String, TestInfo> activeTests = new ConcurrentHashMap<>();

    // =========================================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================================

    /**
     * Start Allure test case - call from @Before hook (order = 0).
     *
     * <p>This method MUST be called before any attachments can be added.
     * It schedules and starts the Allure test case lifecycle.</p>
     *
     * @param scenarioName Human-readable scenario name
     * @param scenarioId Unique scenario identifier (for history tracking)
     */
    public void startTestCase(String scenarioName, String scenarioId) {
        // Check for existing test in this thread (shouldn't happen, but guard)
        String existingUuid = currentTestUuid.get();
        if (existingUuid != null) {
            log.warn("⚠️ Test already running in this thread (uuid={}), stopping it first", existingUuid);
            stopTestCase(Status.BROKEN);
        }

        String uuid = UUID.randomUUID().toString();
        currentTestUuid.set(uuid);

        try {
            AllureLifecycle lifecycle = Allure.getLifecycle();

            TestResult result = new TestResult()
                    .setUuid(uuid)
                    .setName(scenarioName)
                    .setFullName(scenarioId)
                    .setHistoryId(scenarioId);

            lifecycle.scheduleTestCase(result);
            lifecycle.startTestCase(uuid);

            activeTests.put(uuid, new TestInfo(
                    scenarioName,
                    scenarioId,
                    Thread.currentThread().getName(),
                    System.currentTimeMillis()
            ));

            log.debug("✅ Allure test case STARTED: {} (uuid={})", scenarioName, uuid);

        } catch (Exception e) {
            log.warn("⚠️ Failed to start Allure test case '{}': {}", scenarioName, e.getMessage());
            currentTestUuid.remove();
        }
    }

    /**
     * Stop Allure test case - call from @After hook (order = 0, runs LAST).
     *
     * <p>This method finalizes the test case with the given status and writes
     * the result to allure-results directory.</p>
     *
     * @param status Final test status (PASSED, FAILED, SKIPPED, BROKEN)
     */
    public void stopTestCase(Status status) {
        String uuid = currentTestUuid.get();

        if (uuid == null) {
            log.trace("No active Allure test case to stop (uuid is null)");
            return;
        }

        try {
            AllureLifecycle lifecycle = Allure.getLifecycle();

            lifecycle.updateTestCase(uuid, testResult -> {
                testResult.setStatus(status);
            });

            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);

            TestInfo info = activeTests.remove(uuid);
            if (info != null) {
                long duration = System.currentTimeMillis() - info.startTime();
                log.debug("✅ Allure test case STOPPED: {} (uuid={}, status={}, duration={}ms)",
                        info.scenarioName(), uuid, status, duration);
            }

        } catch (Exception e) {
            log.warn("⚠️ Failed to stop Allure test case (uuid={}): {}", uuid, e.getMessage());
        } finally {
            currentTestUuid.remove();
        }
    }

    /**
     * Check if a test is currently running in this thread.
     *
     * @return true if Allure test context is active
     */
    public boolean isTestRunning() {
        String uuid = currentTestUuid.get();
        return uuid != null && activeTests.containsKey(uuid);
    }

    // =========================================================================
    // ATTACHMENT METHODS
    // =========================================================================

    /**
     * Safely attach content to current test.
     *
     * <p>If no test is running, logs a warning but does NOT throw exception.
     * This ensures attachments never break test execution.</p>
     *
     * @param name Attachment name (displayed in Allure report)
     * @param content Attachment content
     * @param mimeType MIME type (e.g., "text/plain", "application/json", "text/html")
     */
    public void attachSafely(String name, String content, String mimeType) {
        if (!isTestRunning()) {
            log.warn("⚠️ Cannot attach '{}' - no Allure test running in current thread", name);
            return;
        }

        if (content == null) {
            log.trace("Skipping null attachment: {}", name);
            return;
        }

        try {
            Allure.addAttachment(name, mimeType, content);
            log.trace("📎 Attached: {} ({} bytes, type={})", name, content.length(), mimeType);
        } catch (Exception e) {
            log.warn("⚠️ Failed to attach '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Convenience method for plain text attachments.
     */
    public void attachText(String name, String content) {
        attachSafely(name, content, "text/plain");
    }

    /**
     * Convenience method for JSON attachments.
     */
    public void attachJson(String name, String jsonContent) {
        attachSafely(name, jsonContent, "application/json");
    }

    /**
     * Convenience method for XML attachments.
     */
    public void attachXml(String name, String xmlContent) {
        attachSafely(name, xmlContent, "application/xml");
    }

    /**
     * Convenience method for HTML attachments.
     */
    public void attachHtml(String name, String htmlContent) {
        attachSafely(name, htmlContent, "text/html");
    }

    /**
     * Attach content with automatic MIME type detection.
     */
    public void attachAuto(String name, String content) {
        if (content == null) return;

        String trimmed = content.trim();
        String mimeType;

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            mimeType = "application/json";
        } else if (trimmed.startsWith("<")) {
            mimeType = "application/xml";
        } else {
            mimeType = "text/plain";
        }

        attachSafely(name, content, mimeType);
    }

    // =========================================================================
    // STEP MANAGEMENT
    // =========================================================================

    /**
     * Add a named step within the current test.
     *
     * <p>If no test is running, executes the action directly without Allure step wrapper.</p>
     *
     * @param stepName Step name (displayed in Allure report)
     * @param action Action to execute within the step
     */
    public void step(String stepName, Runnable action) {
        if (!isTestRunning()) {
            log.debug("⚠️ Cannot add Allure step '{}' - no test running, executing directly", stepName);
            if (action != null) {
                action.run();
            }
            return;
        }

        Allure.step(stepName, () -> {
            if (action != null) {
                action.run();
            }
        });
    }

    /**
     * Update test case description.
     *
     * @param description Test description (supports Markdown)
     */
    public void setDescription(String description) {
        String uuid = currentTestUuid.get();
        if (uuid == null) {
            return;
        }

        try {
            Allure.getLifecycle().updateTestCase(uuid, testResult -> {
                testResult.setDescription(description);
            });
        } catch (Exception e) {
            log.warn("⚠️ Failed to set description: {}", e.getMessage());
        }
    }

    /**
     * Add a link to the test case.
     *
     * @param name Link name
     * @param url Link URL
     */
    public void addLink(String name, String url) {
        if (!isTestRunning()) {
            return;
        }

        try {
            Allure.link(name, url);
        } catch (Exception e) {
            log.warn("⚠️ Failed to add link '{}': {}", name, e.getMessage());
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Get current test UUID for advanced usage.
     *
     * @return Optional containing UUID if test is running
     */
    public Optional<String> getCurrentTestUuid() {
        return Optional.ofNullable(currentTestUuid.get());
    }

    /**
     * Get count of active tests (for monitoring).
     *
     * @return Number of currently active test contexts
     */
    public int getActiveTestCount() {
        return activeTests.size();
    }

    /**
     * Convert Cucumber scenario status to Allure status.
     *
     * @param cucumberStatus Cucumber Status enum
     * @return Allure Status enum
     */
    public static Status toAllureStatus(io.cucumber.plugin.event.Status cucumberStatus) {
        if (cucumberStatus == null) {
            return Status.BROKEN;
        }

        return switch (cucumberStatus) {
            case PASSED -> Status.PASSED;
            case FAILED -> Status.FAILED;
            case SKIPPED -> Status.SKIPPED;
            case PENDING -> Status.SKIPPED;
            case UNDEFINED -> Status.BROKEN;
            case AMBIGUOUS -> Status.BROKEN;
            default -> Status.BROKEN;
        };
    }

    /**
     * Convert boolean pass/fail to Allure status.
     *
     * @param passed true if test passed
     * @return PASSED or FAILED status
     */
    public static Status toAllureStatus(boolean passed) {
        return passed ? Status.PASSED : Status.FAILED;
    }

    /**
     * Convert scenario flags to Allure status.
     *
     * @param isFailed true if scenario failed
     * @param isSkipped true if scenario was skipped
     * @return Appropriate Allure status
     */
    public static Status toAllureStatus(boolean isFailed, boolean isSkipped) {
        if (isSkipped) {
            return Status.SKIPPED;
        }
        return isFailed ? Status.FAILED : Status.PASSED;
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    /**
     * Cleanup orphan test contexts on shutdown.
     *
     * <p>Called automatically by Spring during application context shutdown.
     * Ensures no memory leaks from tests that didn't properly stop
     * (e.g., due to abnormal termination, uncaught exceptions, etc.).</p>
     */
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Cleaning up AllureLifecycleManager...");

        int orphanCount = activeTests.size();
        if (orphanCount > 0) {
            log.warn("Found {} orphan test context(s) - cleaning up", orphanCount);

            for (String uuid : activeTests.keySet()) {
                TestInfo info = activeTests.get(uuid);
                log.warn("   Orphan test: {} (thread={}, started={}ms ago)",
                        info.scenarioName(),
                        info.threadName(),
                        System.currentTimeMillis() - info.startTime());

                try {
                    AllureLifecycle lifecycle = Allure.getLifecycle();
                    lifecycle.updateTestCase(uuid, tr -> {
                        tr.setStatus(Status.BROKEN);
                        tr.setDescription("Test did not complete normally - marked as BROKEN during cleanup");
                    });
                    lifecycle.stopTestCase(uuid);
                    lifecycle.writeTestCase(uuid);
                    log.debug("   Cleaned up orphan test: {}", uuid);
                } catch (Exception e) {
                    log.debug("   Could not clean orphan test {}: {}", uuid, e.getMessage());
                }
            }

            activeTests.clear();
        }

        // Clear thread-local (may be null if called from different thread)
        currentTestUuid.remove();

        log.info("✅ AllureLifecycleManager cleanup complete (processed {} orphan(s))", orphanCount);
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Internal record for tracking active test info.
     */
    private record TestInfo(
            String scenarioName,
            String scenarioId,
            String threadName,
            long startTime
    ) {}
}