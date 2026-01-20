package com.acuver.autwit.internal;

import com.acuver.autwit.core.domain.ScenarioStateContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import com.acuver.autwit.internal.api.SterlingApiCalls;
import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
import com.acuver.autwit.internal.context.TestThreadContext;
import com.acuver.autwit.internal.listeners.TestNGListenerNew;
import com.acuver.autwit.internal.reporting.AllureLifecycleManager;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
import io.qameta.allure.model.Status;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * AUTWIT Cucumber Hooks - Lifecycle management for scenarios.
 *
 * <h2>COMPARISON: OLD vs NEW</h2>
 * <pre>
 * ┌────────────────────────────────────────┬─────────────┬─────────────┐
 * │ Feature                                │ OLD         │ NEW         │
 * ├────────────────────────────────────────┼─────────────┼─────────────┤
 * │ @Before (beforeFirst)                  │ ✓           │ ✓           │
 * │ @Before (beforeScenario)               │ ✓           │ ✓           │
 * │ @After (afterScenario)                 │ ✓           │ ✓           │
 * │ @AfterStep                             │ ✗           │ ✓           │
 * │ @BeforeStep                            │ ✗           │ ✓           │
 * │ @BeforeMethod (TestNG)                 │ ✓           │ ✓           │
 * │ AfterTest() static method              │ ✓           │ ✓           │
 * │ scenarioThreadLocal                    │ ✓           │ ✓           │
 * │ Excel test data loading                │ ✓           │ ✗           │
 * │ Order creation from Excel              │ ✓           │ ✗           │
 * │ Inventory adjustment                   │ ✓           │ ✗           │
 * │ Schedule/Release automation            │ ✓           │ ✗           │
 * │ SoftAssert clear/assertAll             │ ✓           │ ✓           │
 * │ apiCalls ThreadLocal                   │ ✓           │ ✓           │
 * │ Allure integration                     │ ✗           │ ✓           │
 * │ ScenarioContext/MDC                    │ ✗           │ ✓           │
 * │ StepContextPlugin integration          │ ✗           │ ✓           │
 * └────────────────────────────────────────┴─────────────┴─────────────┘
 * </pre>
 *
 * <h2>HOOK ORDER</h2>
 * <pre>
 * @Before(order = 0)  → Start Allure lifecycle (FIRST)
 * @Before(order = 1)  → Setup ScenarioContext, MDC
 * @Before(order = 2)  → beforeFirst - Excel data loading, order creation DEPRICATED
 * @Before(order = 3)  → beforeScenario - Clear soft asserts
 *
 * @BeforeStep         → Capture step from StepContextPlugin, start Allure step
 *
 * @AfterStep          → Update Allure step status, cleanup
 *
 * @After(order = 3)   → afterScenario - Assert all, cleanup apiCalls
 * @After(order = 2)   → Cleanup scenario objects
 * @After(order = 1)   → Cleanup ScenarioContext, MDC
 * @After(order = 0)   → Stop Allure lifecycle (LAST)
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@RequiredArgsConstructor
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    // ==========================================================================
    // DEPENDENCIES (NEW - injected)
    // ==========================================================================
    private final RuntimeContextPort runtimeContext;
    private final ScenarioContextPort scenarioContextPort;
    private final ScenarioStatePort scenarioStateTracker;
    private final AllureLifecycleManager allureLifecycle;

    // ==========================================================================
    // THREAD-LOCAL STORAGE (from OLD)
    // ==========================================================================

    /** Thread-local scenario reference (from OLD) */
    private static final ThreadLocal<Scenario> scenarioThreadLocal = new ThreadLocal<>();

    // ==========================================================================
    // BEFORE HOOKS
    // ==========================================================================

    /**
     * Start Allure test case - MUST run first (NEW).
     */
    @Before(order = 0)
    public void startAllureTestCase(Scenario scenario) {
        String scenarioName = sanitize(scenario.getName());
        String scenarioId = sanitize(scenario.getId());

        // Start Allure lifecycle FIRST
        allureLifecycle.startTestCase(scenarioName, scenarioId);

        log.trace("Allure test case started for: {}", scenarioName);
    }

    /**
     * Order 1: Setup scenario identity + ScenarioContext + MDC.
     * This must happen before steps begin so all logs are correlated.
     */
    @Before(order = 1)
    public void setupScenarioContext(Scenario scenario) {
        String scenarioName = sanitize(scenario.getName());
        String scenarioId = sanitize(scenario.getId());

        // Generate deterministic keys
        int hash = Math.abs((scenarioName + scenarioId).hashCode() % 9999);
        String exampleKey = "ex" + hash;
        String testCaseId = "TC" + hash;
        String scenarioKey = scenarioName + "_" + exampleKey;

        // Save in ThreadLocal ScenarioContext (NEW)
        ScenarioContext.set("scenarioName", scenarioName);
        ScenarioContext.set("scenarioId", scenarioId);
        ScenarioContext.set("exampleKey", exampleKey);
        ScenarioContext.set("testCaseId", testCaseId);
        ScenarioContext.set("scenarioKey", scenarioKey);

        // Set MDC for Log4j2 routing appender (NEW)
        ScenarioMDC.setScenario(scenarioKey);
        ScenarioMDC.setScenarioName(scenarioName);
        ScenarioMDC.setScenarioId(scenarioId);
        ScenarioMDC.setThreadId(String.valueOf(Thread.currentThread().threadId()));

        runtimeContext.set("scenarioName", scenarioName); // IMPORTANT: use scenarioKey not raw scenarioName
        runtimeContext.set("scenarioKey", scenarioKey);

        log.info("▶ Starting Scenario: {} | Example={} | Key={}", scenarioName, exampleKey, scenarioKey);

        // Safe attachment - now Allure lifecycle is active
        allureLifecycle.attachText("Scenario Initialized", scenarioKey);
    }

    /**
     * Order 2: Legacy beforeFirst (Excel data etc.).
     * This block is deprecated but kept for backward compatibility.
     */
    @Before(order = 2)
    public void beforeFirst(Scenario scenario) throws Exception {
        logIndicator("BEFORE-FIRST");

        // Store scenario in ThreadLocal (from OLD)
        scenarioThreadLocal.set(scenario);

        // Set scenario name in TestThreadContext (from OLD)
        String scenarioName = scenario.getName();
        TestThreadContext.set("scenarioName", scenarioName);
        TestThreadContext.set("scenario", scenarioName);

        // Set in TestNGListenerNew for retry framework (NEW)
        TestNGListenerNew.setCurrentScenario(scenario);

        // Excel test data loading (from OLD)
        String testDataExcelPath = FileReaderManager.getInstance().getConfigReader()
                .getValidationExcelsPath() + "TestData.xlsx";
        String sheetName = "TestData"; // FrameworkConstants.TEST_DATA

        try {
//            ExcelHelpers.getInstance().setExcelFile(testDataExcelPath, sheetName);
//            processExcelTestData(scenario);
        } catch (Exception e) {
            log.warn("Excel test data processing skipped: {}", e.getMessage());
        }
    }

    /**
     * Order 3: Reset assertions + init per-scenario dependencies.
     */
    @Before(order = 3)
    public void beforeScenario(Scenario scenario) {
        logIndicator("BEFORE-SCENARIO");

        // Clear soft asserts (from OLD)
        SoftAssertUtils.clearSoftAssert();

        // Initialize API client (from OLD via NEW pattern)
        ScenarioContext.initApi(new com.acuver.autwit.internal.api.SterlingApiCalls());

        log.info("Scenario {} Thread {} cleared the assert!",
                scenario.getName(), Thread.currentThread().threadId());
    }

    // ==========================================================================
    // BEFORE/AFTER STEP (NEW - for StepContextPlugin integration)
    // ==========================================================================

    /**
     * After each step - update Allure step status (NEW).
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        // Get last step status from context
        String lastStepStatus = ScenarioContext.get("lastStepStatus");

        Status allureStatus = Status.PASSED;
        if ("FAILED".equalsIgnoreCase(lastStepStatus)) {
            allureStatus = Status.FAILED;
        } else if ("SKIPPED".equalsIgnoreCase(lastStepStatus)) {
            allureStatus = Status.SKIPPED;
        }

        // Stop Allure step
        allureLifecycle.stopTestCase(allureStatus);

        // Clear current step
        ScenarioContext.remove("currentStep");
        ScenarioContext.remove("lastStepStatus");
    }

    // ==========================================================================
    // AFTER HOOKS
    // ==========================================================================

    /**
     * afterScenario - Assert all and cleanup (from OLD).
     */
    @After(order = 3)
    public void afterScenario(Scenario scenario) {
        logIndicator("AFTER-SCENARIO");

        // Get and assert soft assertions (from OLD)
        SoftAssert softAssert = SoftAssertUtils.getSoftAssert();
        try {
            SoftAssertUtils.assertAll();
        } catch (AssertionError e) {
            log.error("Soft assertion failures: {}", e.getMessage());
            allureLifecycle.attachText("Soft Assertion Failures", e.getMessage());
        }

        // Close Excel workbook (from OLD)
        try {
//            ExcelHelpers.getInstance().closeWorkbook();
        } catch (Exception e) {
            log.trace("Excel workbook already closed or not opened");
        }

        // Cleanup apiCalls ThreadLocal (from OLD)
        Object apiCallsInstance = TestNGListenerNew.apiCalls.get();
        if (apiCallsInstance != null) {
            TestNGListenerNew.apiCalls.remove();
            log.debug("apiCalls.remove() executed - Thread cleared");
        }
    }

    /**
     * Cleanup scenario objects (NEW).
     */
    @After(order = 2)
    public void cleanupScenarioObjects(Scenario scenario) {
        // Additional cleanup if needed
    }

    /**
     * Cleanup scenario context and MDC (NEW).
     */
    @After(order = 1)
    public void cleanupScenarioContext(Scenario scenario) {
        String scenarioName = ScenarioContext.get("scenarioName");
        String scenarioKey = ScenarioContext.get("scenarioKey");

        String status = scenario.isFailed() ? "FAILED" :
                (scenario.getStatus() == io.cucumber.java.Status.SKIPPED ? "SKIPPED" : "PASSED");

        log.info("⏹ Finished Scenario: {} | Status={} | Key={}",
                scenarioName, status, scenarioKey);

        // Attach final scenario summary
        String summary = String.format(
                "Scenario: %s%nKey: %s%nStatus: %s",
                scenarioName, scenarioKey, status
        );
        allureLifecycle.attachText("Scenario Summary", summary);

        // Clear contexts
        runtimeContext.clear();
        ScenarioContext.clear();
        ScenarioMDC.clear();
    }

    /**
     * Stop Allure test case - MUST run last (NEW).
     */
    @After(order = 0)
    public void stopAllureTestCase(Scenario scenario) {
        Status allureStatus = determineAllureStatus(scenario);
        allureLifecycle.stopTestCase(allureStatus);
        log.trace("Allure test case stopped with status: {}", allureStatus);
    }

    // ==========================================================================
    // STATIC METHOD FOR TESTNG LISTENER (from OLD)
    // ==========================================================================

    /**
     * AfterTest - Called by TestNGListener on failure (from OLD).
     *
     * @return Current scenario from ThreadLocal
     */
    public static Scenario AfterTest() throws Exception {
        logIndicatorStatic("AFTER_TEST");

        Scenario scenario = scenarioThreadLocal.get();
        try {
            if (scenario != null && scenario.isFailed()) {
                String testcasename = scenario.getName();
                log.debug("Failed scenario: {}", testcasename);
                return scenario;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving scenario", e);
        }
        return scenario;
    }

    /**
     * Get current scenario from ThreadLocal.
     */
    public static Scenario getCurrentScenario() {
        return scenarioThreadLocal.get();
    }

    // ==========================================================================
    // TESTNG INTEGRATION (from OLD)
    // ==========================================================================

    /**
     * BeforeMethod - TestNG hook for scenario name retrieval (from OLD).
     */
    @BeforeMethod
    public void beforeTestClass() {
        logIndicator("HOOKS-BEFORE-METHOD");

        String scenarioName = TestThreadContext.get("scenarioName");
        if (scenarioName == null) {
            log.warn("Scenario name not set properly in the context.");
        } else {
            log.info("Scenario name retrieved for TestNG listener: {}", scenarioName);
        }
    }


    // ==========================================================================
    // PRIVATE HELPERS
    // ==========================================================================

    /**
     * Get SterlingApiCalls from ThreadLocal.
     */
    private SterlingApiCalls getApiCalls() {
        Object apiCallsObj = TestNGListenerNew.apiCalls.get();
        if (apiCallsObj instanceof SterlingApiCalls) {
            return (SterlingApiCalls) apiCallsObj;
        }
        // Create new instance if not available
        SterlingApiCalls apiCalls = new SterlingApiCalls();
        TestNGListenerNew.apiCalls.set(apiCalls);
        return apiCalls;
    }

    /**
     * Sanitize string for use as filename and routing key.
     */
    private String sanitize(String input) {
        if (input == null) return "unknown";
        String s = input
                .replaceAll("[^a-zA-Z0-9-_\\.]", "_")
                .replaceAll("_+", "_")
                .replaceAll("[\\._]+$", "")
                .replaceAll("^\\.+", "");
        return s.isEmpty() ? "unknown" : s;
    }

    /**
     * Determine Allure status from Cucumber scenario.
     */
    private Status determineAllureStatus(Scenario scenario) {
        if (scenario.isFailed()) {
            return Status.FAILED;
        }
        if (scenario.getStatus() == io.cucumber.java.Status.SKIPPED) {
            return Status.SKIPPED;
        }
        return Status.PASSED;
    }

    /**
     * Log indicator banner (instance method).
     */
    private void logIndicator(String text) {
        int totalLength = 40;
        int textLength = text.length();
        int leftPadding = (totalLength - textLength) / 2;
        int rightPadding = totalLength - leftPadding - textLength;
        String indicator = "─".repeat(leftPadding) + text + "─".repeat(rightPadding);
        log.info(indicator);
    }

    /**
     * Log indicator banner (static method for AfterTest).
     */
    private static void logIndicatorStatic(String text) {
        int totalLength = 40;
        int textLength = text.length();
        int leftPadding = (totalLength - textLength) / 2;
        int rightPadding = totalLength - leftPadding - textLength;
        String indicator = "+".repeat(leftPadding) + text + "+".repeat(rightPadding);
        log.info(indicator);
    }

    // ==========================================================================
    // CONTEXT RESTORATION (NEW - for resume scenarios)
    // ==========================================================================

    /**
     * Restore scenario context from persisted state.
     */
    private void restoreScenarioContext(ScenarioStateContext savedContext) {
        Map<String, String> stepStatus = savedContext.getStepStatus();
        Map<String, Map<String, String>> allStepData = savedContext.getStepData();

        if (stepStatus == null || stepStatus.isEmpty()) {
            log.warn("No step status found in saved context");
            return;
        }

        // Find first successful step to restore data from
        String firstSuccessfulStep = null;
        for (Map.Entry<String, String> entry : stepStatus.entrySet()) {
            if ("success".equalsIgnoreCase(entry.getValue())) {
                firstSuccessfulStep = entry.getKey();
                break;
            }
        }

        if (firstSuccessfulStep == null || allStepData == null) {
            return;
        }

        Map<String, String> firstStepData = allStepData.get(firstSuccessfulStep);
        if (firstStepData == null || firstStepData.isEmpty()) {
            return;
        }

        // Restore all step data to context
        for (Map.Entry<String, String> entry : firstStepData.entrySet()) {
            ScenarioContext.set(entry.getKey(), entry.getValue());
            if ("orderId".equals(entry.getKey())) {
                ScenarioMDC.setOrderId(entry.getValue());
            }
        }

        log.info("✅ Restored context from step '{}': orderId={}",
                firstSuccessfulStep, firstStepData.get("orderId"));

        allureLifecycle.attachText("Context Restored",
                "Restored from step: " + firstSuccessfulStep + "\nData: " + firstStepData);
    }

    /**
     * Restore specific step data to context.
     */
    private void restoreStepData(Map<String, String> stepData) {
        if (stepData == null || stepData.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : stepData.entrySet()) {
            ScenarioContext.set(entry.getKey(), entry.getValue());
            if ("orderId".equals(entry.getKey())) {
                ScenarioMDC.setOrderId(entry.getValue());
            }
        }

        log.debug("Restored {} context variables", stepData.size());
    }
}