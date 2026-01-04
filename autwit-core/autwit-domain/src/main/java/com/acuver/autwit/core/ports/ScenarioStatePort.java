package com.acuver.autwit.core.ports;
import java.util.Map;

public interface ScenarioStatePort {
    public void markStep(String scenario, String step, String status, Map<String, String> stepData);
    public boolean isStepAlreadySuccessful(String scenario, String step);
    public Map<String,String> getStepData(String scenario, String step);
}
