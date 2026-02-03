package com.acuver.autwit.adapter.postgres;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * JPA Entity for API Template (PostgreSQL).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "api_template")
public class PostgresApiTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", nullable = false, unique = true, length = 255)
    private String apiName;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint_template", nullable = false, length = 500)
    private String endpointTemplate;

    @Column(name = "request_template", nullable = false, columnDefinition = "TEXT")
    private String requestTemplate;

    @Column(name = "data_representation", nullable = false, length = 50)
    private String dataRepresentation = "XML";

    @Column(name = "is_service", nullable = false)
    private Boolean isService = false;

    @Column(name = "service_name", length = 255)
    private String serviceName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}