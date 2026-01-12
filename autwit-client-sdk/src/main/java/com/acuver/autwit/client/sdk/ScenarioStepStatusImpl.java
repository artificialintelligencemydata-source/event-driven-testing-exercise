package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ScenarioStepStatusImpl implements Autwit.ScenarioStepStatus {

    private final ScenarioStatePort scenarioStatePort;
    private final RuntimeContextPort runtimeContext;

    ScenarioStepStatusImpl(ScenarioStatePort scenarioState, RuntimeContextPort runtimeContext) {
        this.scenarioStatePort = scenarioState;
        this.runtimeContext = runtimeContext;
    }

    @Override
    public void markStepSuccess() {
        scenarioStatePort.markStep(
                currentScenario(),
                currentStep(),
                "success",
                Collections.emptyMap()
        );
    }

    @Override
    public void markStepFailed(String reason) {
        Map<String, String> data = new HashMap<>();
        data.put("reason", reason);
        scenarioStatePort.markStep(
                currentScenario(),
                currentStep(),
                "FAILED",
                data
        );
    }

    public void markStepSkipped(String reason) {
        Map<String, String> data = new HashMap<>();
        data.put("reason", reason);

        scenarioStatePort.markStep(
                currentScenario(),
                currentStep(),
                "skipped",
                data
        );
    }

    @Override
    public boolean skipIfAlreadySuccessful() {
        String scenarioKey = currentScenario();
        String stepName = currentStep();

        if (!scenarioStatePort.isStepAlreadySuccessful(scenarioKey, stepName)) {
            return false;
        }

        // 🔁 Restore step data (OLD AUTWIT BEHAVIOR)
        Map<String, String> stepData =
                scenarioStatePort.getStepData(scenarioKey, stepName);

        if (stepData == null || stepData.isEmpty()) {
            return true; // step succeeded but had no data
        }

        for (Map.Entry<String, String> entry : stepData.entrySet()) {
            runtimeContext.set(entry.getKey(), entry.getValue());
        }

        return true;
    }

    @Override
    public Map<String, String> getStepData() {
        return scenarioStatePort.getStepData(
                currentScenario(),
                currentStep()
        );
    }

    private String currentScenario() {
        String scenarioName = runtimeContext.get("scenarioName");
        if (scenarioName == null) {
            throw new IllegalStateException("scenarioName not set in RuntimeContext - Hooks.beforeScenario() may not have run");
        }
        return scenarioName;
    }

    private String currentStep() {
        String step = runtimeContext.get("currentStep");
        if (step == null) {
            throw new IllegalStateException("currentStep not set in RuntimeContext - step must call setCurrentStep() before markStepSuccess()");
        }
        return step;
    }
}