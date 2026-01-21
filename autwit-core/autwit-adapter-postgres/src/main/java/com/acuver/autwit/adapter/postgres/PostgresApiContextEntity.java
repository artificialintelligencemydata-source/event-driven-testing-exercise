package com.acuver.autwit.adapter.postgres;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for API context persistence.
 *
 * <h2>VISIBILITY</h2>
 * Package-private - internal to the PostgreSQL adapter.
 * Not exposed to domain layer or other modules.
 *
 * <h2>MAPPING</h2>
 * Maps to table: api_context
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Entity
@Table(name = "api_context", indexes = {
        @Index(name = "idx_api_name", columnList = "api_name", unique = true),
        @Index(name = "idx_service_name", columnList = "service_name"),
        @Index(name = "idx_is_service", columnList = "is_service"),
        @Index(name = "idx_http_method", columnList = "http_method"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostgresApiContextEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", nullable = false, unique = true, length = 255)
    private String apiName;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "api_template", nullable = false, length = 2000)
    private String apiTemplate;

    @Column(name = "data_representation", nullable = false, length = 50)
    private String dataRepresentation;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "is_service", nullable = false)
    private Boolean isService;

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
