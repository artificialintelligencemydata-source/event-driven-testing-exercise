package com.acuver.autwit.core.domain;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioContext {

    private String scenarioName;

    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();

    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();

    private long lastUpdated;

    public ScenarioContext(String scenarioName) {
        this.scenarioName = scenarioName;
    }
}
