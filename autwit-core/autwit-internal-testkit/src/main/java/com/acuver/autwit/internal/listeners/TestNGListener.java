package com.acuver.autwit.internal.listeners;

import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.TestThreadContext;
import com.acuver.autwit.internal.integration.CreateIssue;
import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.testng.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise TestNG Listener with Spring Integration
 * - Tracks passed/failed/skipped tests
 * - MDC-aware logging
 * - Jira ticket creation on failure
 * - Thread-safe retry framework
 */
@Component
public class TestNGListener implements ITestListener, ISuiteListener {
    private static final Logger log = LogManager.getLogger(TestNGListener.class);

    /** Thread-safe lists for retry framework */
    public static final List<ITestResult> skippedTests = new CopyOnWriteArrayList<>();
    public static final Set<String> passedTestKeys = ConcurrentHashMap.newKeySet();
    public static final Set<String> failedTestKeys = ConcurrentHashMap.newKeySet();

    /** Attribute key for caching computed test key */
    public static final String TEST_KEY_ATTR = "AUTWIT_SCENARIO_KEY";

    // Static holder for Spring context (set by CucumberSpringConfiguration)
    private static ApplicationContext applicationContext;

    /** Thread-local storage for current scenario */
    private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>();


    // Static setter for ApplicationContext (called from CucumberSpringConfiguration)
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    public void onStart(ISuite suite) {
        log.info("==== SUITE START: {} ====", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("==== SUITE FINISH: {} ====", suite.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        long threadId = Thread.currentThread().threadId();
        ScenarioContext.set("testNGThreadId", threadId);
        log.info("➡ Test START [{}] on thread {}", getTestKey(result), threadId);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        String key = getTestKey(result);
        passedTestKeys.add(key);
        skippedTests.removeIf(r -> getTestKey(r).equals(key));
        log.info("✔ PASS {}", key);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String key = getTestKey(result);
        failedTestKeys.add(key);
        skippedTests.removeIf(r -> getTestKey(r).equals(key));

        log.error("❌ FAIL {} — {}", key,
                (result.getThrowable() != null ? result.getThrowable().getMessage() : "No error"));
        // Create Jira ticket on failure
        createJiraTicket();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String key = getTestKey(result);
        String scenarioName = TestThreadContext.get("scenario");
        if (scenarioName != null) {
            result.setTestName(scenarioName);
            result.setAttribute("SCENARIO_NAME", scenarioName);
        }
        if (!passedTestKeys.contains(key)) {   // skip only if not passed earlier
            skippedTests.add(result);
            log.warn("⏩ SKIPPED {}", key);
        }
    }

    /**
     * Computes a stable unique key for each scenario.
     * Needed for retry logic and DB-resume logic.
     */
    public static String getTestKey(ITestResult result) {

        Object cached = result.getAttribute(TEST_KEY_ATTR);
        if (cached != null) return cached.toString();

        String methodName = result.getMethod().getQualifiedName();

        StringBuilder key = new StringBuilder(methodName);
        Object[] params = result.getParameters();

        if (params != null && params.length > 0) {
            key.append("::");
            for (Object p : params) {
                key.append(p == null ? "null" : p.toString()).append("|");
            }
        }

        String scenarioName = TestThreadContext.get("scenario");
        if (scenarioName != null) {
            key.append("[").append(scenarioName).append("]");
        }

        String finalKey = key.toString();
        result.setAttribute(TEST_KEY_ATTR, finalKey);
        return finalKey;
    }

    /**
     * Creates Jira ticket on test failure
     */
    private void createJiraTicket() {
        try {
            Scenario scenario = currentScenario.get();

            if (scenario == null) {
                log.warn("Cannot create Jira ticket - scenario not available");
                return;
            }

            log.info("Processing failed scenario: {}", scenario.getName());

            StringBuilder description = new StringBuilder();
            description.append("Scenario name: ").append(scenario.getName())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            // Collect soft assertion errors
            try {
                SoftAssertUtils.assertAll();
                log.debug("No soft assertion errors found");
            } catch (AssertionError e) {
                log.error("Assertion errors found: {}", e.getMessage());
                description.append("Assertion Errors:").append(System.lineSeparator())
                        .append(e.getMessage()).append(System.lineSeparator());
            }

            String summary = "Test Failed: " + scenario.getName();
            logIssueIfNeeded(summary, description.toString());

        } catch (Exception e) {
            log.error("Error creating Jira ticket", e);
        }
    }

    /**
     * Logs issue to Jira if configured
     */
    private void logIssueIfNeeded(String summary, String description) {
        if (applicationContext == null) {
            log.warn("Spring ApplicationContext not available - cannot create Jira ticket");
            return;
        }

        try {
            // Check if ticket logging is enabled
                FileReaderManager fileReaderManager = applicationContext.getBean(FileReaderManager.class);
            boolean isTicketLoggingRequired = fileReaderManager
                    .getInstance()
                    .getConfigReader()
                    .isTicketNeedsToBeLogged();

            if (!isTicketLoggingRequired) {
                log.debug("Jira ticket logging is disabled");
                return;
            }

            // Create Jira ticket
            CreateIssue createIssue = applicationContext.getBean(CreateIssue.class);
            createIssue.createIssue("Bug", summary, description);
            log.info("✅ Jira issue created successfully");

        } catch (Exception e) {
            log.error("Error creating Jira issue: {}", e.getMessage(), e);
        }
    }
}
