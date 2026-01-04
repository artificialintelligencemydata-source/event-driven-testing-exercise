package com.acuver.autwit.engine.state;

import com.acuver.autwit.core.domain.ScenarioContext;
import com.acuver.autwit.core.ports.ScenarioContextPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScenarioStateTracker implements ScenarioStatePort {

    private final ScenarioContextPort port;

    public ScenarioStateTracker(ScenarioContextPort port) {
        this.port = port;
    }

    public void markStep(String scenario, String step, String status, Map<String, String> stepData) {

        ScenarioContext state =
                port.findByScenarioName(scenario)
                        .orElseGet(() -> new ScenarioContext(scenario));

        // Step status
        state.getStepStatus().put(step, status);

        // Step data
        if ("success".equalsIgnoreCase(status) && stepData != null) {
            state.getStepData().put(step, stepData);
        }

        state.setLastUpdated(System.currentTimeMillis());
        port.save(state);

        System.out.println("Scenario step logged: " + scenario + " -> " + step + " = " + status);
    }

    public boolean isStepAlreadySuccessful(String scenario, String step) {
        return port.findByScenarioName(scenario)
                .map(ctx -> "success".equalsIgnoreCase(ctx.getStepStatus().get(step)))
                .orElse(false);
    }

    public Map<String,String> getStepData(String scenario, String step) {
        return port.findByScenarioName(scenario)
                .map(ctx -> ctx.getStepData().getOrDefault(step, new HashMap<>()))
                .orElse(new HashMap<>());
    }

    public void clear(String scenario) {
        port.delete(scenario);
    }

    public void clearAll() {
        port.deleteAll();
    }
}
