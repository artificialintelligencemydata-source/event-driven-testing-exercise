package com.acuver.autwit.adapter.h2.scenario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scenario_context", indexes = {
        @Index(name = "idx_scenario_name", columnList = "scenario_name"),
        @Index(name = "idx_example_id", columnList = "example_id"),
        @Index(name = "idx_scenario_status", columnList = "scenario_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class H2ScenarioContextEntity {

    /**
     * Primary key - Auto-generated UUID
     *
     * FIX: Added @GeneratedValue(strategy = GenerationType.UUID)
     * This tells JPA to auto-generate the ID if it's null when persisting.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "example_id", length = 255)
    private String exampleId;

    @Column(name = "test_case_id", length = 255)
    private String testCaseId;

    @Column(name = "scenario_name", nullable = false, length = 255)
    private String scenarioName;

    /**
     * Map<String, String> - stores step status (step name -> status)
     * Converted to JSON string in database
     */
    @Convert(converter = MapStringStringConverter.class)
    @Column(name = "step_status", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();

    /**
     * Map<String, Map<String, String>> - stores step data (step name -> data map)
     * Converted to JSON string in database
     */
    @Convert(converter = NestedMapConverter.class)
    @Column(name = "step_data", columnDefinition = "TEXT")
    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();

    @Column(name = "last_updated")
    private Long lastUpdated;

    @Column(name = "scenario_status", length = 100)
    private String scenarioStatus;
}