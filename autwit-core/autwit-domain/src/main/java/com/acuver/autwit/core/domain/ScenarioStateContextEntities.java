package com.acuver.autwit.core.domain;

import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted scenario service (database-backed).
 * 
 * This is NOT runtime context (see RuntimeScenarioContext in internal-testkit).
 * This service is persisted to support resume across JVM restarts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioStateContextEntities {
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
    public ScenarioStateContextEntities(String scenarioName) {
        this.scenarioName = scenarioName;
    }
}

