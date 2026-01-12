package com.acuver.autwit.internal;

import com.acuver.autwit.core.domain.ScenarioStateContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
import com.acuver.autwit.internal.reporting.AllureLifecycleManager;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.model.Status;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * AUTWIT Cucumber Hooks - Lifecycle management for scenarios.
 *
 * <h2>HOOK ORDER</h2>
 * <pre>
 * @Before(order = 0)  → Start Allure lifecycle (FIRST)
 * @Before(order = 1)  → Setup ScenarioContext, MDC
 * @Before(order = 2)  → Setup API and other objects
 *
 * @After(order = 2)   → Cleanup scenario objects (FIRST in @After)
 * @After(order = 1)   → Cleanup ScenarioContext, MDC
 * @After(order = 0)   → Stop Allure lifecycle (LAST - runs last)
 * </pre>
 *
 * <h2>IMPORTANT</h2>
 * Allure lifecycle MUST start before any attachments and stop AFTER all cleanup.
 * The order values ensure this sequence.
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    private final ScenarioContextPort scenarioContextPort;
    private final ScenarioStatePort scenarioStateTracker;
    private final AllureLifecycleManager allureLifecycle;

    // ==============================================================
    // BEFORE HOOKS (order 0 runs first)
    // ==============================================================

    /**
     * Start Allure test case - MUST run first.
     *
     * <p>This ensures Allure lifecycle is active before any attachments.</p>
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
     * Setup scenario context and MDC for logging.
     */
    @Before(order = 1)
    public void setupScenarioContext(Scenario scenario) {
        String scenarioName = sanitize(scenario.getName());
        String scenarioId = sanitize(scenario.getId());
        int hash = Math.abs((scenarioName + scenarioId).hashCode() % 9999);
        String exampleKey = "ex" + hash;
        String testCaseId = "TC" + hash;
        String scenarioKey = scenarioName + "_" + exampleKey;

        // Save in ThreadLocal ScenarioContext
        ScenarioContext.set("scenarioName", scenarioName);
        ScenarioContext.set("scenarioId", scenarioId);
        ScenarioContext.set("exampleKey", exampleKey);
        ScenarioContext.set("testCaseId", testCaseId);
        ScenarioContext.set("scenarioKey", scenarioKey);

        // Set MDC for Log4j2 routing appender
        ScenarioMDC.setScenario(scenarioKey);
        ScenarioMDC.setScenarioName(scenarioName);
        ScenarioMDC.setScenarioId(scenarioId);
        ScenarioMDC.setThreadId(String.valueOf(Thread.currentThread().threadId()));

        log.info("▶ Starting Scenario: {} | Example={} | Key={}", scenarioName, exampleKey, scenarioKey);

        // Safe attachment - now Allure lifecycle is active
        allureLifecycle.attachText("Scenario Initialized", scenarioKey);
    }

    /**
     * Initialize API client and other scenario objects.
     */
    @Before(order = 2)
    public void setupScenarioObjects(Scenario scenario) {
        // Clear any previous soft assertions
        SoftAssertUtils.clearSoftAssert();

        // Initialize API client
        ScenarioContext.initApi(new com.acuver.autwit.internal.api.ApiCalls());

        log.debug("⚙️ ScenarioContext ready: {} ({})",
                ScenarioContext.get("scenarioName"),
                ScenarioContext.get("scenarioKey"));
    }

    // ==============================================================
    // AFTER STEP
    // ==============================================================

    /**
     * Cleanup after each step.
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        ScenarioContext.remove("currentStep");
    }

    // ==============================================================
    // AFTER HOOKS (order 0 runs LAST in @After)
    // ==============================================================

    /**
     * Cleanup scenario objects and evaluate soft assertions.
     */
    @After(order = 2)
    public void cleanupScenarioObjects(Scenario scenario) {
        // Evaluate soft assertions if any
        try {
            SoftAssertUtils.assertAll();
        } catch (AssertionError e) {
            log.error("Soft assertion failures: {}", e.getMessage());
            // Attach soft assertion failures to Allure
            allureLifecycle.attachText("Soft Assertion Failures", e.getMessage());
        }
    }

    /**
     * Cleanup scenario context and MDC.
     */
    @After(order = 1)
    public void cleanupScenarioContext(Scenario scenario) {
        String scenarioName = ScenarioContext.get("scenarioName");
        String scenarioKey = ScenarioContext.get("scenarioKey");

        String status = scenario.isFailed() ? "FAILED" :
                (scenario.getStatus().name().equals("SKIPPED") ? "SKIPPED" : "PASSED");

        log.info("⏹ Finished Scenario: {} | Status={} | Key={}",
                scenarioName, status, scenarioKey);

        // Attach final scenario summary
        String summary = String.format(
                "Scenario: %s%nKey: %s%nStatus: %s%nDuration: N/A",
                scenarioName, scenarioKey, status
        );
        allureLifecycle.attachText("Scenario Summary", summary);

        // Clear contexts
        ScenarioContext.clear();
        ScenarioMDC.clear();
    }

    /**
     * Stop Allure test case - MUST run last.
     *
     * <p>This ensures all attachments are added before Allure lifecycle ends.</p>
     */
    @After(order = 0)
    public void stopAllureTestCase(Scenario scenario) {
        Status allureStatus = determineAllureStatus(scenario);

        allureLifecycle.stopTestCase(allureStatus);

        log.trace("Allure test case stopped with status: {}", allureStatus);
    }

    // ==============================================================
    // PRIVATE HELPERS
    // ==============================================================

    /**
     * Sanitize string for use as filename and routing key.
     * Removes special characters that cause issues on Windows or in Log4j2 routing.
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

        try {
            io.cucumber.plugin.event.Status cucumberStatus = scenario.getStatus();
            return AllureLifecycleManager.toAllureStatus(cucumberStatus);
        } catch (Exception e) {
            // Fallback if status conversion fails
            return scenario.isFailed() ? Status.FAILED : Status.PASSED;
        }
    }

    /**
     * Restore scenario context from persisted state.
     * Used during scenario resume.
     */
    private void restoreScenarioContext(ScenarioStateContext savedContext) {
        Map<String, String> stepStatus = savedContext.getStepStatus();
        Map<String, Map<String, String>> allStepData = savedContext.getStepData();

        if (stepStatus == null || stepStatus.isEmpty()) {
            log.warn("No step status found in saved context, treating as fresh run");
            return;
        }

        if (allStepData == null || allStepData.isEmpty()) {
            log.warn("No step data found in saved context, treating as fresh run");
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

        if (firstSuccessfulStep == null) {
            log.warn("No successful steps found in saved context, treating as fresh run");
            return;
        }

        Map<String, String> firstStepData = allStepData.get(firstSuccessfulStep);
        if (firstStepData == null || firstStepData.isEmpty()) {
            log.warn("No data for first successful step, treating as fresh run");
            return;
        }

        // Restore all step data to context
        for (Map.Entry<String, String> entry : firstStepData.entrySet()) {
            ScenarioContext.set(entry.getKey(), entry.getValue());

            // Also set orderId in MDC if present
            if ("orderId".equals(entry.getKey())) {
                ScenarioMDC.setOrderId(entry.getValue());
            }
        }

        log.info("✅ Restored context from step '{}': orderId={}",
                firstSuccessfulStep, firstStepData.get("orderId"));

        // Attach restoration info to Allure
        allureLifecycle.attachText("Context Restored",
                "Restored from step: " + firstSuccessfulStep + "\n" +
                        "Data: " + firstStepData);
    }

    /**
     * Restore specific step data to context.
     */
    private void restoreStepData(Map<String, String> stepData) {
        if (stepData == null || stepData.isEmpty()) {
            log.debug("No step data to restore");
            return;
        }

        for (Map.Entry<String, String> entry : stepData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            ScenarioContext.set(key, value);

            if ("orderId".equals(key)) {
                ScenarioMDC.setOrderId(value);
                log.debug("Restored orderId={} to MDC", value);
            }
        }

        log.debug("Restored {} context variables from step data", stepData.size());
    }
}