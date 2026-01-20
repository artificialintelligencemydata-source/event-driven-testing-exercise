package com.acuver.autwit.internal.resume;

import com.acuver.autwit.internal.listeners.TestNGListenerNew;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResumeExecutor - Programmatically executes resumed scenarios via TestNG.
 *
 * <h2>RESPONSIBILITY</h2>
 * <ul>
 *   <li>Build dynamic TestNG configuration for specific scenarios</li>
 *   <li>Execute tests programmatically using TestNG API</li>
 *   <li>Report execution results with metrics</li>
 * </ul>
 *
 * <h2>HARDENED FEATURES</h2>
 * <ul>
 *   <li>Exception handling around TestNG execution</li>
 *   <li>Empty scenario list guard</li>
 *   <li>Runner class validation</li>
 *   <li>Execution metrics and logging</li>
 *   <li>Partial failure detection</li>
 * </ul>
 *
 * <h2>CONFIGURATION</h2>
 * <pre>
 * autwit:
 *   runner:
 *     class: com.bjs.tests.runner.ClientCucumberRunner
 *   resume:
 *     parallel: false
 *     thread-count: 1
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
@Component
public class ResumeExecutor {

    private static final Logger log = LogManager.getLogger(ResumeExecutor.class);

    @Value("${autwit.runner.class:com.bjs.tests.runner.ClientCucumberRunner}")
    private String runnerClass;

    @Value("${autwit.resume.parallel:false}")
    private boolean parallelResume;

    @Value("${autwit.resume.thread-count:1}")
    private int threadCount;

    /**
     * Execute specific scenarios by their keys.
     *
     * <p>Builds a dynamic TestNG suite and executes the specified scenarios.
     * Returns detailed execution results including success/failure metrics.</p>
     *
     * @param scenarioKeys List of scenario keys to execute
     * @return ExecutionResult with detailed metrics
     */
    public ExecutionResult execute(List<String> scenarioKeys) {
        // Guard: null or empty list
        if (scenarioKeys == null || scenarioKeys.isEmpty()) {
            log.debug("No scenarios to execute - returning empty result");
            return ExecutionResult.empty();
        }

        // Remove duplicates and nulls
        List<String> cleanedKeys = scenarioKeys.stream()
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .toList();

        if (cleanedKeys.isEmpty()) {
            log.debug("All scenario keys were null/blank - returning empty result");
            return ExecutionResult.empty();
        }

        log.info("📋 Preparing to execute {} scenario(s)", cleanedKeys.size());
        if (log.isDebugEnabled()) {
            log.debug("Scenarios to execute:");
            cleanedKeys.forEach(k -> log.debug("   - {}", k));
        }

        long startTime = System.currentTimeMillis();
        ExecutionResult result = new ExecutionResult();
        result.setTotalScenarios(cleanedKeys.size());
        result.setScenarioKeys(new ArrayList<>(cleanedKeys));

        try {
            // Validate runner class exists
            if (!isRunnerClassValid()) {
                String error = "Runner class not found: " + runnerClass;
                log.error("❌ {}", error);
                result.setSuccess(false);
                result.setErrorMessage(error);
                result.setDurationMs(System.currentTimeMillis() - startTime);
                return result;
            }

            // Build dynamic TestNG suite
            XmlSuite suite = buildSuite(cleanedKeys);

            if (log.isDebugEnabled()) {
                log.debug("Generated TestNG XML:\n{}", suite.toXml());
            }

            // Configure and execute TestNG
            TestNG testng = new TestNG();
            testng.setXmlSuites(List.of(suite));
            testng.setUseDefaultListeners(true);
            testng.addListener(new TestNGListenerNew());

            log.info("▶️ Starting resume execution...");

            testng.run();

            // Capture results
            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setSuccess(!testng.hasFailure() && !testng.hasSkip());
            result.setHasFailures(testng.hasFailure());
            result.setHasSkipped(testng.hasSkip());

            // Log appropriate message
            if (testng.hasFailure() && testng.hasSkip()) {
                log.warn("⚠️ Resume execution completed with failures AND skipped tests ({}ms)", duration);
                result.setErrorMessage("Some scenarios failed and some are still skipped");
            } else if (testng.hasFailure()) {
                log.warn("⚠️ Resume execution completed with failures ({}ms)", duration);
                result.setErrorMessage("Some scenarios failed during execution");
            } else if (testng.hasSkip()) {
                log.info("⏸️ Resume execution completed but some scenarios still skipped ({}ms)", duration);
                result.setErrorMessage("Some scenarios are still waiting for events");
            } else {
                log.info("✅ Resume execution completed successfully ({}ms)", duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Resume execution failed after {}ms: {}", duration, e.getMessage(), e);

            result.setSuccess(false);
            result.setDurationMs(duration);
            result.setErrorMessage(e.getMessage());
            result.setException(e);
        }

        return result;
    }

    /**
     * Build TestNG XmlSuite for the specified scenarios.
     *
     * @param scenarioKeys List of scenario keys
     * @return Configured XmlSuite
     */
    private XmlSuite buildSuite(List<String> scenarioKeys) {
        XmlSuite suite = new XmlSuite();
        suite.setName("AUTWIT-Resume-Suite-" + System.currentTimeMillis());

        // Configure parallel execution
        if (parallelResume && threadCount > 1) {
            suite.setParallel(XmlSuite.ParallelMode.METHODS);
            suite.setThreadCount(threadCount);
            log.debug("Configured parallel execution with {} threads", threadCount);
        } else {
            suite.setParallel(XmlSuite.ParallelMode.NONE);
            log.debug("Configured sequential execution");
        }

        // Create test
        XmlTest test = new XmlTest(suite);
        test.setName("Resume-Test");

        // Pass scenario keys as parameter for DataProvider filtering
        Map<String, String> params = new HashMap<>();
        params.put("scenariosToRun", String.join(",", scenarioKeys));
        params.put("resumeMode", "true");  // Indicator for runner
        test.setParameters(params);

        // Add runner class with filteredScenarios method
        XmlClass xmlClass = new XmlClass(runnerClass);
        XmlInclude include = new XmlInclude("filteredScenarios");
        xmlClass.setIncludedMethods(List.of(include));
        test.setXmlClasses(List.of(xmlClass));

        return suite;
    }

    /**
     * Execute a single scenario by key.
     *
     * @param scenarioKey Scenario key to execute
     * @return ExecutionResult with detailed metrics
     */
    public ExecutionResult executeSingle(String scenarioKey) {
        if (scenarioKey == null || scenarioKey.isBlank()) {
            log.warn("Cannot execute null/empty scenario key");
            return ExecutionResult.empty();
        }
        return execute(List.of(scenarioKey));
    }

    /**
     * Validate that the runner class exists and can be loaded.
     *
     * @return true if runner class is valid
     */
    public boolean isRunnerClassValid() {
        try {
            Class<?> clazz = Class.forName(runnerClass);
            log.trace("Runner class validated: {}", clazz.getName());
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Runner class not found: {} - {}", runnerClass, e.getMessage());
            return false;
        }
    }

    /**
     * Get the configured runner class name.
     *
     * @return Fully qualified runner class name
     */
    public String getRunnerClass() {
        return runnerClass;
    }

    /**
     * Check if parallel execution is enabled.
     *
     * @return true if parallel resume is enabled
     */
    public boolean isParallelEnabled() {
        return parallelResume;
    }

    /**
     * Get configured thread count for parallel execution.
     *
     * @return Thread count
     */
    public int getThreadCount() {
        return threadCount;
    }

    // =========================================================================
    // EXECUTION RESULT
    // =========================================================================

    /**
     * Execution result holder with detailed metrics.
     */
    public static class ExecutionResult {
        private boolean success;
        private int totalScenarios;
        private List<String> scenarioKeys;
        private long durationMs;
        private boolean hasFailures;
        private boolean hasSkipped;
        private String errorMessage;
        private Exception exception;

        /**
         * Create an empty result (for no-op executions).
         */
        public static ExecutionResult empty() {
            ExecutionResult result = new ExecutionResult();
            result.success = true;
            result.totalScenarios = 0;
            result.scenarioKeys = List.of();
            result.durationMs = 0;
            result.hasFailures = false;
            result.hasSkipped = false;
            return result;
        }

        /**
         * Check if execution was fully successful (no failures, no skipped).
         */
        public boolean isFullySuccessful() {
            return success && !hasFailures && !hasSkipped;
        }

        /**
         * Check if there were any problems (failures or skipped).
         */
        public boolean hasProblems() {
            return hasFailures || hasSkipped;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getTotalScenarios() { return totalScenarios; }
        public void setTotalScenarios(int totalScenarios) { this.totalScenarios = totalScenarios; }

        public List<String> getScenarioKeys() { return scenarioKeys; }
        public void setScenarioKeys(List<String> scenarioKeys) { this.scenarioKeys = scenarioKeys; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public boolean isHasFailures() { return hasFailures; }
        public void setHasFailures(boolean hasFailures) { this.hasFailures = hasFailures; }

        public boolean isHasSkipped() { return hasSkipped; }
        public void setHasSkipped(boolean hasSkipped) { this.hasSkipped = hasSkipped; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Exception getException() { return exception; }
        public void setException(Exception exception) { this.exception = exception; }

        @Override
        public String toString() {
            return String.format(
                    "ExecutionResult{success=%s, scenarios=%d, duration=%dms, failures=%s, skipped=%s, error=%s}",
                    success, totalScenarios, durationMs, hasFailures, hasSkipped,
                    errorMessage != null ? "'" + errorMessage + "'" : "none"
            );
        }
    }
}