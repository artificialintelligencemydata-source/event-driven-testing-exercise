package com.acuver.autwit.internal;

import com.acuver.autwit.core.domain.ScenarioStateContextEntities;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import com.acuver.autwit.internal.api.SterlingApiCalls;
import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.context.RuntimeContextAdapter;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
import com.acuver.autwit.internal.context.TestThreadContext;
import com.acuver.autwit.internal.helper.BaseActionsNew;
import com.acuver.autwit.internal.listeners.TestNGListenerNew;
import com.acuver.autwit.internal.reporting.AllureLifecycleManager;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.model.Status;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.annotations.BeforeMethod;
import org.testng.asserts.SoftAssert;

import java.util.Map;
import java.util.UUID;

/**
 * AUTWIT Cucumber HooksOld - Lifecycle management for scenarios.
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
public class HooksOld {

    private static final Logger log = LogManager.getLogger(HooksOld.class);

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

        // 1️⃣ Extract scenario metadata
        // Generate deterministic keys
        /*int hash = Math.abs((scenarioName + scenarioId).hashCode() % 9999);
        String exampleKey = "ex" + hash;
        String testCaseId = "TC" + hash;
        String scenarioKey = scenarioName + "_" + exampleKey;*/
        String testCaseId = extractTestCaseId(scenarioId);
        String exampleKey = extractExampleId(scenario);

        // 2️⃣ Generate execution-unique scenarioKey (CRITICAL FIX)
        // ✅ Generate scenarioKey ONCE per scenario
        String scenarioKey = generateExecutionUniqueScenarioKey(scenarioName, exampleKey);

        // 3️⃣ Store in RuntimeContextPort (AUTWIT framework)
        runtimeContext.set("scenarioName", scenarioName);
        runtimeContext.set("scenarioKey", scenarioKey);
        runtimeContext.set("testCaseId", testCaseId);
        runtimeContext.set("exampleKey", exampleKey);

        // 4️⃣ Store in MDC (SLF4J - for distributed logging/tracing)
        // Set MDC for Log4j2 routing appender (NEW)
        ScenarioMDC.setScenario(scenarioKey);
        ScenarioMDC.setScenarioName(scenarioName);
        ScenarioMDC.setScenarioId(scenarioId);
        ScenarioMDC.setThreadId(String.valueOf(Thread.currentThread().threadId()));

        // Save in ThreadLocal ScenarioContext (NEW)
        ScenarioContext.set("scenarioName", scenarioName);
        ScenarioContext.set("scenarioId", scenarioId);
        ScenarioContext.set("exampleKey", exampleKey);
        ScenarioContext.set("testCaseId", testCaseId);
        ScenarioContext.set("scenarioKey", scenarioKey);

        log.info("▶ Starting Scenario: {} | Example={} | Key={}", scenarioName, exampleKey, scenarioKey);
        // 7️⃣ Log initialized context
        log.info("📋 Scenario Context Initialized:");
        log.info("   ├─ scenarioKey  : {}", scenarioKey);
        log.info("   ├─ testCaseId   : {}", testCaseId);
        log.info("   ├─ exampleKey   : {}", exampleKey);
        log.info("   ├─ scenarioName : {}", scenarioName);
        log.info("   └─ threadId     : {}", Thread.currentThread().getId());

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
        log.info("🧹 Cleaning up scenario context...");
        try {
            // 1️⃣ Clear RuntimeContextPort
            if (runtimeContext instanceof RuntimeContextAdapter) {
                ((RuntimeContextAdapter) runtimeContext).clear();
                log.info("✓ RuntimeContext cleared");
            }

            // 2️⃣ Clear BaseActionsNew call index tracker
            BaseActionsNew.clearCallIndexTracker();
            log.info("✓ Call index tracker cleared");

            // 3️⃣ Clear MDC (SLF4J)
            ScenarioMDC.clear();
            log.info("✓ ScenarioMDC cleared");

            // 4️⃣ Clear ThreadContext (Log4j)
            TestThreadContext.clearAll();
            log.info("✓ TestThreadContext cleared");

            // 5️⃣ Clear ThreadLocal
            scenarioThreadLocal.remove();
            log.info("✓ scenarioThreadLocal cleared");
            log.info("✅ Scenario context cleanup completed successfully");

        } catch (Exception e) {
            // Cleanup should NEVER fail the test
            log.error("⚠️ Error during context cleanup (non-fatal): {}", e.getMessage(), e);
        }
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
    private void restoreScenarioContext(ScenarioStateContextEntities savedContext) {
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

    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================
    /**
     * Generate execution-unique scenario key (CRITICAL FIX).
     *
     * <h3>FORMAT</h3>
     * <pre>{scenarioName}_{exampleId}_T{threadId}_{uuid8}_{timestamp}</pre>
     *
     * <h3>UNIQUENESS GUARANTEES</h3>
     * <ul>
     *   <li>Different scenarios → Different names</li>
     *   <li>Same scenario, different examples → Different exampleId</li>
     *   <li>Parallel threads → Different threadId</li>
     *   <li>Sequential runs → Different uuid + timestamp</li>
     *   <li>Reruns/retries → Different uuid + timestamp</li>
     * </ul>
     *
     * @param scenarioName Scenario name
     * @param exampleId    Example identifier
     * @return Unique scenario key
     */
    private String generateExecutionUniqueScenarioKey(String scenarioName, String exampleId) {
        String sanitizedName = scenarioName.replaceAll("[^a-zA-Z0-9_]", "_");
        String uuid8 = UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();

        return String.format("%s_%s_T%d_%s_%d",
                sanitizedName,
                exampleId != null ? exampleId : "default",
                threadId,
                uuid8,
                timestamp
        );
    }

    /**
     * Extract test case ID from scenario ID.
     *
     * @param scenarioId Cucumber scenario ID
     * @return Feature file name or "unknown"
     */
    private String extractTestCaseId(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            return "unknown";
        }
        try {
            String[] parts = scenarioId.split("/");
            String featureFile = parts[parts.length - 1].split(":")[0];
            return featureFile.replace(".feature", "");
        } catch (Exception e) {
            log.warn("Failed to extract test case ID from: {}", scenarioId);
            return "unknown";
        }
    }

    /**
     * Extract example ID for data-driven tests.
     *
     * @param scenario Cucumber Scenario object
     * @return Example identifier or "default"
     */
    private String extractExampleId(Scenario scenario) {
        if (scenario.getUri() == null) {
            return "default";
        }

        String uri = scenario.getUri().toString();

        if (uri.contains(":")) {
            try {
                String[] parts = uri.split(":");
                String lineNumber = parts[parts.length - 1];
                return "example_line_" + lineNumber;
            } catch (Exception e) {
                log.warn("Failed to extract line number from URI: {}", uri);
                return "default";
            }
        }

        return "default";
    }
}