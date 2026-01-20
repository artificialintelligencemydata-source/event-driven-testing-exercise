package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Scenario Step Status Implementation.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class ScenarioStepStatusImpl implements Autwit.ScenarioStepStatus {

    private final ScenarioStatePort scenarioStatePort;
    private final RuntimeContextPort runtimeContext;

    ScenarioStepStatusImpl(ScenarioStatePort scenarioState, RuntimeContextPort runtimeContext) {
        this.scenarioStatePort = scenarioState;
        this.runtimeContext = runtimeContext;
    }

    @Override
    public void markStepSuccess() {
        String scenario = currentScenario();
        String step = currentStep();
        scenarioStatePort.markStep(scenario, step, "success", Collections.emptyMap());
    }

    @Override
    public void markStepFailed(String reason) {
        String scenario = currentScenario();
        String step = currentStep();
        Map<String, String> data = new HashMap<>();
        data.put("reason", reason);
        scenarioStatePort.markStep(scenario, step, "FAILED", data);
    }

    @Override
    public void markStepSkipped(String reason) {
        String scenario = currentScenario();
        String step = currentStep();
        Map<String, String> data = new HashMap<>();
        data.put("reason", reason);
        scenarioStatePort.markStep(scenario, step, "skipped", data);
    }

    @Override
    public boolean skipIfAlreadySuccessful() {
        String scenarioKey = currentScenario();
        String stepName = currentStep();

        if (!scenarioStatePort.isStepAlreadySuccessful(scenarioKey, stepName)) {
            return false;
        }
        Map<String, String> stepData = scenarioStatePort.getStepData(scenarioKey, stepName);

        if (stepData == null || stepData.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : stepData.entrySet()) {
            runtimeContext.set(entry.getKey(), entry.getValue());
        }
        return true;
    }

    @Override
    public Map<String, String> getStepData() {
        return scenarioStatePort.getStepData(currentScenario(), currentStep());
    }

    private String currentScenario() {
        String scenarioName = runtimeContext.get("scenarioName");
        if (scenarioName == null) {
            throw new IllegalStateException("scenarioName not set in RuntimeContext");
        }
        return scenarioName;
    }

    private String currentStep() {
        String step = runtimeContext.get("currentStep");
        if (step == null) {
            throw new IllegalStateException("currentStep not set - ensure StepContextPlugin is configured");
        }
        return step;
    }
}