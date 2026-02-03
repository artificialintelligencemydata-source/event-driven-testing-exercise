package com.acuver.autwit.adapter.postgres;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PostgresApiContextEntity - JPA entity for PostgreSQL storage.
 *
 * <h2>INTERNAL USE ONLY</h2>
 * This is an internal adapter entity. External code should use ApiContextEntities from domain.
 *
 * <h2>COMPOSITE UNIQUE CONSTRAINT</h2>
 * UNIQUE(step_key, api_name, call_index)
 *
 * <h2>STEP-LEVEL TRACKING</h2>
 * - step_key: Unique identifier for each step execution
 * - step_name: Human-readable step name (e.g., "I create an order")
 * - step_execution_index: Counter for step reruns (0, 1, 2...)
 *
 * <h2>WHY SEPARATE FROM DOMAIN?</h2>
 * - Domain model (ApiContextEntities) is clean, no DB annotations
 * - This JPA entity has PostgreSQL-specific mappings
 * - Adapter converts between domain ↔ persistence models
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Entity
@Table(name = "api_context",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_step_api_call",
                        columnNames = {"step_key", "api_name", "call_index"}
                )
        },
        indexes = {
                @Index(name = "idx_api_context_step_key", columnList = "step_key"),
                @Index(name = "idx_api_context_step_name", columnList = "step_name"),
                @Index(name = "idx_api_context_step_execution_index", columnList = "step_execution_index"),
                @Index(name = "idx_api_context_scenario_key", columnList = "scenario_key"),
                @Index(name = "idx_api_context_scenario_step", columnList = "scenario_key,step_key"),
                @Index(name = "idx_api_context_api_name", columnList = "api_name"),
                @Index(name = "idx_api_context_order_no", columnList = "order_no"),
                @Index(name = "idx_api_context_order_header_key", columnList = "order_header_key"),
                @Index(name = "idx_api_context_http_method", columnList = "http_method"),
                @Index(name = "idx_api_context_data_representation", columnList = "data_representation"),
                @Index(name = "idx_api_context_is_service", columnList = "is_service"),
                @Index(name = "idx_api_context_created_at", columnList = "created_at"),
                @Index(name = "idx_api_context_updated_at", columnList = "updated_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PostgresApiContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Scenario isolation
    @Column(name = "scenario_key", nullable = false, length = 200)
    private String scenarioKey;

    @Column(name = "test_case_id", length = 100)
    private String testCaseId;

    @Column(name = "example_id", length = 100)
    private String exampleId;

    //Step-level tracking
    @Column(name = "step_key", nullable = false, length = 250)
    private String stepKey;

    @Column(name = "step_name", nullable = false, length = 200)
    private String stepName;

    @Column(name = "step_execution_index", nullable = false)
    @Builder.Default
    private Integer stepExecutionIndex = 0;

    // API identification
    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(name = "call_index", nullable = false)
    @Builder.Default
    private Integer callIndex = 0;

    // API metadata
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "api_template", nullable = false, length = 2000)
    private String apiTemplate;

    @Column(name = "data_representation", nullable = false, length = 50)
    private String dataRepresentation;

    // Payloads
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    // Service identification
    @Column(name = "is_service", nullable = false)
    @Builder.Default
    private Boolean isService = false;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    //Business entity correlation
    @Column(name = "order_no", length = 100)
    private String orderNo;

    @Column(name = "order_header_key", length = 100)
    private String orderHeaderKey;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}