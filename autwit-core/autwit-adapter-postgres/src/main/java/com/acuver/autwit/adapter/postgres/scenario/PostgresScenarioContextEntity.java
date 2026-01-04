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


    @Id
    @Column(name = "scenario_name")
    private String scenarioName;

    // stepName -> "success"/"skipped"
    @Lob
    @Column(name = "step_status", columnDefinition = "TEXT")
    private Map<String, String> stepStatus;

    // stepName -> Map<field,value>
    @Lob
    @Column(name = "step_data", columnDefinition = "TEXT")
    private Map<String, Map<String, String>> stepData;

    @Column(name = "last_updated")
    private long lastUpdated;

//    ⚠ Nested map works
//    Postgres supports JSON serialization via Hibernate map handlers.
//    If required later, we can convert stepData to a JSON string.
}
