package com.acuver.autwit.engine.scenarioStateTracker;

import com.acuver.autwit.core.domain.ScenarioStateContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ScenarioStateTracker implements ScenarioStatePort {

    private final ScenarioContextPort scenarioContextPort;
    private final RuntimeContextPort runtimeContextPort;

    public ScenarioStateTracker(ScenarioContextPort port, RuntimeContextPort runtimeContextPort) {
        this.scenarioContextPort = port;
        this.runtimeContextPort = runtimeContextPort;
    }

    @Override
    public void markStep(String scenario,
                         String step,
                         String status,
                         Map<String, String> stepData) {

        ScenarioStateContext state = null;

        // 1️⃣ Load existing scenario state (if any)
        Optional<ScenarioStateContext> optional =
                scenarioContextPort.findByScenarioName(scenario);

        if (optional.isPresent()) {
            state = optional.get();
        }

        // 2️⃣ Create scenario state if missing (OLD behavior parity)
        if (state == null) {
            state = new ScenarioStateContext();
            state.setScenarioName(scenario);
            state.setStepStatus(new HashMap<>());
            state.setStepData(new HashMap<>());
            state.setTestCaseId(runtimeContextPort.get("testCaseId"));
            state.setExampleId(runtimeContextPort.get("exampleKey"));
            state.setScenarioKey(runtimeContextPort.get("scenarioKey"));

        }

        // ✅ Ensure scenarioKey is never missing (critical safety)
        if (state.getScenarioKey() == null || state.getScenarioKey().isBlank()) {
            state.setScenarioKey(runtimeContextPort.get("scenarioKey"));
        }

        // ✅ Ensure scenarioName exists too
        if (state.getScenarioName() == null || state.getScenarioName().isBlank()) {
            state.setScenarioName(scenario);
        }

        // 3️⃣ Defensive map initialization (CRITICAL)
        if (state.getStepStatus() == null) {
            state.setStepStatus(new HashMap<>());
        }

        if (state.getStepData() == null) {
            state.setStepData(new HashMap<>());
        }

        // 4️⃣ Update step status
        state.getStepStatus().put(step, status);

        // 5️⃣ Persist step data ONLY for success
        if ("success".equalsIgnoreCase(status) && stepData != null) {
            state.getStepData().put(step, stepData);
        }

        // 6️⃣ Update timestamp
        state.setLastUpdated(System.currentTimeMillis());


        // 7️⃣ Persist
        scenarioContextPort.save(state);

        System.out.println(
                "Marked step: scenario=" + scenario
                        + ", step=" + step
                        + ", status=" + status
        );
    }


    @Override
    public boolean isStepAlreadySuccessful(String scenario, String step) {

        ScenarioStateContext state = null;

        Optional<ScenarioStateContext> optional =
                scenarioContextPort.findByScenarioName(scenario);

        if (optional.isPresent()) {
            state = optional.get();
        }

        if (state == null) {
            return false;
        }

        if (state.getStepStatus() == null) {
            return false;
        }

        String status = state.getStepStatus().get(step);

        return "success".equalsIgnoreCase(status);
    }


    @Override
    public Map<String, String> getStepData(String scenario, String step) {

        ScenarioStateContext state = null;

        Optional<ScenarioStateContext> optional =
                scenarioContextPort.findByScenarioName(scenario);

        if (optional.isPresent()) {
            state = optional.get();
        }

        if (state == null) {
            return new HashMap<>();
        }

        if (state.getStepData() == null) {
            return new HashMap<>();
        }

        Map<String, Map<String, String>> allStepData = state.getStepData();

        if (!allStepData.containsKey(step)) {
            return new HashMap<>();
        }

        Map<String, String> stepData = allStepData.get(step);

        if (stepData == null) {
            return new HashMap<>();
        }

        return stepData;
    }


    @Override
    public void updateScenarioStatus(String scenario, String status) {

        ScenarioStateContext state = null;

        Optional<ScenarioStateContext> optional =
                scenarioContextPort.findByScenarioName(scenario);

        if (optional.isPresent()) {
            state = optional.get();
        }

        if (state == null) {
            state = new ScenarioStateContext();
            state.setScenarioName(scenario);
            state.setStepStatus(new HashMap<>());
            state.setStepData(new HashMap<>());
            state.setTestCaseId(runtimeContextPort.get("testCaseId"));
            state.setExampleId(runtimeContextPort.get("exampleKey"));
            state.setScenarioKey(runtimeContextPort.get("scenarioKey"));
        }

        state.setScenarioStatus(status);
        state.setLastUpdated(System.currentTimeMillis());

        scenarioContextPort.save(state);

        System.out.println(
                "Updated scenario status: scenario=" + scenario + ", status=" + status);
    }


    public void clear(String scenario) {
        scenarioContextPort.delete(scenario);
    }

    public void clearAll() {
        scenarioContextPort.deleteAll();
    }
}
