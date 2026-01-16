package com.acuver.autwit.adapter.postgres.scenario;

import com.acuver.autwit.adapter.postgres.MapStringStringConverter;
import com.acuver.autwit.adapter.postgres.NestedMapConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "scenario_context")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostgresScenarioContextEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;  // This will be the scenario name

    @Column(name = "example_id", length = 255)
    private String exampleId;

    @Column(name = "test_case_id", length = 255)
    private String testCaseId;

    @Column(name = "scenario_name", nullable = false, length = 255)
    private String scenarioName;

    /**
     * OPTION 1: Using JPA Converters (Works with any PostgreSQL version)
     * Stores as JSONB in database, converts to Map in Java
     */
    @Convert(converter = MapStringStringConverter.class)
    @Column(name = "step_status", columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();

    @Convert(converter = NestedMapConverter.class)
    @Column(name = "step_data", columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();

    /**
     * OPTION 2: Using Hibernate 6.2+ @JdbcTypeCode (Alternative approach)
     * Uncomment below and comment out OPTION 1 if you prefer this approach
     */
    /*
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_status", columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, String> stepStatus = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_data", columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, Map<String, String>> stepData = new HashMap<>();
    */

    @Column(name = "last_updated")
    private Long lastUpdated;

    @Column(name = "scenario_status", length = 100)
    private String scenarioStatus;
}