package com.acuver.autwit.core.domain;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted scenario scenarioStateTracker (database-backed).
 * 
 * This is NOT runtime context (see RuntimeScenarioContext in internal-testkit).
 * This scenarioStateTracker is persisted to support resume across JVM restarts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioStateContext {
    private UUID _id;
    private String exampleId;
    private String testCaseId;
    private String scenarioKey;
    private String scenarioName;
    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();
    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();
    private long lastUpdated;
    private String scenarioStatus;
    public ScenarioStateContext(String scenarioName) {
        this.scenarioName = scenarioName;
    }
}

