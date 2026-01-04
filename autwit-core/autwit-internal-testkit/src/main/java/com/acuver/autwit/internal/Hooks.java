package com.acuver.autwit.internal;

import com.acuver.autwit.internal.api.ApiCalls;
import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Hooks {

    private static final Logger log = LogManager.getLogger(Hooks.class);

    /** Safe filenames for Windows & routing */
    private String sanitize(String input) {
        if (input == null) return "unknown";
        String s = input.replaceAll("[^a-zA-Z0-9-_\\.]", "_")
                .replaceAll("_+", "_")
                .replaceAll("[\\._]+$", "")
                .replaceAll("^\\.+", "");
        return s;
    }

    // ==============================================================
    // BEFORE SCENARIO
    // ==============================================================
    @Before(order = 0)
    public void beforeScenario(Scenario scenario) {

        String name = sanitize(scenario.getName());
        String id = sanitize(scenario.getId());

        int hash = Math.abs((name + id).hashCode() % 9999);
        String exampleKey = "TC" + hash;

        String scenarioKey = name + "_" + exampleKey;

        // Save in ThreadLocal
        ScenarioContext.set("scenarioName", name);
        ScenarioContext.set("scenarioId", id);
        ScenarioContext.set("exampleKey", exampleKey);
        ScenarioContext.set("scenarioKey", scenarioKey);

        // Set MDC — this drives routing appender
        ScenarioMDC.setScenario(scenarioKey);  // routing key
        ScenarioMDC.setScenarioName(name);     // human-friendly
        ScenarioMDC.setScenarioId(id);         // metadata

        log.info("▶ Starting Scenario: {} | Example={} | Key={}", name, exampleKey, scenarioKey);

        Allure.addAttachment("Scenario Initialized", scenarioKey);
    }

    // ==============================================================
    // INIT API PER SCENARIO
    // ==============================================================
    @Before(order = 1)
    public void setupScenarioObjects(Scenario scenario) {

        SoftAssertUtils.clearSoftAssert();

        // Each scenario gets its own ApiCalls instance
        ScenarioContext.initApi(new ApiCalls());

        log.info("⚙️ ScenarioContext ready: {} ({})",
                ScenarioContext.get("scenarioName"),
                ScenarioContext.get("scenarioKey"));
    }

    // ==============================================================
    // AFTER SCENARIO
    // ==============================================================
    @After
    public void afterScenario(Scenario scenario) {

        String scenarioName = ScenarioContext.get("scenarioName");
        String scenarioKey = ScenarioContext.get("scenarioKey");

        String status = scenario.isFailed() ? "FAILED" : "PASSED";

        log.info("⏹ Finished Scenario: {} | Status={} | Key={}",
                scenarioName, status, scenarioKey);

        // Optional:
        // SoftAssertUtils.assertAll();

        // Cleanup everything
        ScenarioContext.clear();
        ScenarioMDC.clear();
    }
}
