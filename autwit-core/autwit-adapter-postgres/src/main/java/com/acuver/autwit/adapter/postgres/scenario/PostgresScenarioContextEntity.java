package com.acuver.autwit.adapter.postgres.scenario;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scenario_context")
public class PostgresScenarioContextEntity {

    /**
     * Technical primary key.
     * Example: scenarioName::exampleId
     */
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "scenario_name", nullable = false)
    private String scenarioName;

    @Column(name = "example_id", nullable = false)
    private String exampleId;

    @Column(name = "test_case_id")
    private String testCaseId;

    /**
     * stepName -> status (success / skipped / failed)
     */
    @Lob
    @Column(name = "step_status", columnDefinition = "TEXT")
    private Map<String, String> stepStatus;

    /**
     * stepName -> key/value map
     */
    @Lob
    @Column(name = "step_data", columnDefinition = "TEXT")
    private Map<String, Map<String, String>> stepData;

    @Column(name = "last_updated")
    private long lastUpdated;

    @Column(name = "scenario_status")
    private String scenarioStatus;
//    ⚠ Nested map works
//    Postgres supports JSON serialization via Hibernate map handlers.
//    If required later, we can convert stepData to a JSON string.
}
