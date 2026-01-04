package com.acuver.autwit.adapter.h2.scenario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scenario_context")
public class H2ScenarioContextEntity {

    @Id
    @Column(name = "scenario_name")
    private String scenarioName;

    /**
     * Map: stepName -> status
     * Stored as CLOB JSON by Hibernate.
     */
    @Lob
    @Column(name = "step_status", columnDefinition = "CLOB")
    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();

    /**
     * Map: stepName -> key/value map
     * Also stored as JSON inside CLOB.
     */
    @Lob
    @Column(name = "step_data", columnDefinition = "CLOB")
    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();

    @Column(name = "last_updated")
    private long lastUpdated;

//    ⚠️ Note: JPA does not support nested maps easily.
//            But this structure works because H2 can serialize nested values as JSON (via Hibernate Map mapping).
//
//    If needed later, we can switch stepData to @Lob String json.
}