package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.ApiContextEntities.HttpMethod;
import com.acuver.autwit.core.domain.StorageException;
import com.acuver.autwit.core.ports.ApiContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL adapter for API context persistence.
 *
 * <h2>ACTIVATION</h2>
 * Activated when: autwit.database.type=postgresql
 *
 * <h2>ARCHITECTURE</h2>
 * Implements {@link ApiContextPort} to provide PostgreSQL-backed storage
 * for {@link ApiContextEntities} domain objects.
 *
 * <h2>DEPENDENCIES</h2>
 * Requires PostgreSQL connection configuration in application.yml
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(name = "autwit.database.type", havingValue = "postgresql")
@Transactional
public class PostgresApiContextAdapter implements ApiContextPort {

    private static final Logger logger = LogManager.getLogger(PostgresApiContextAdapter.class);

    private final PostgresApiContextRepository repository;

    public PostgresApiContextAdapter(PostgresApiContextRepository repository) {
        this.repository = repository;
        logger.info("PostgreSQL API Context Adapter initialized");
    }

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        try {
            PostgresApiContextEntity entity = toEntity(apiContext);
            PostgresApiContextEntity saved = repository.save(entity);
            logger.debug("Saved API context to PostgreSQL: {}", apiContext.getApiName());
            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Failed to save API context: {}", apiContext.getApiName(), e);
            throw new StorageException("Failed to save API context to PostgreSQL", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiContextEntities> findByApiName(String apiName) {
        return repository.findByApiName(apiName)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiContextEntities> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiContextEntities> findByServiceName(String serviceName) {
        return repository.findByServiceName(serviceName).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiContextEntities> findServicesOnly() {
        return repository.findByIsServiceTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiContextEntities> findByHttpMethod(HttpMethod httpMethod) {
        return repository.findByHttpMethod(httpMethod.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        return repository.findByDataRepresentation(dataRepresentation).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByApiName(String apiName) {
        repository.findByApiName(apiName)
                .ifPresent(repository::delete);
        logger.debug("Deleted API context: {}", apiName);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByApiName(String apiName) {
        return repository.existsByApiName(apiName);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
        logger.warn("Deleted all API contexts from PostgreSQL");
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    // ==================== Entity Mapping ====================

    /**
     * Convert domain object to JPA entity.
     */
    private PostgresApiContextEntity toEntity(ApiContextEntities domain) {
        return PostgresApiContextEntity.builder()
                .id(domain.getId())
                .apiName(domain.getApiName())
                .httpMethod(domain.getHttpMethod() != null ? domain.getHttpMethod().name() : null)
                .apiTemplate(domain.getApiTemplate())
                .dataRepresentation(domain.getDataRepresentation())
                .requestPayload(domain.getRequestPayload())
                .responsePayload(domain.getResponsePayload())
                .isService(domain.getIsService())
                .serviceName(domain.getServiceName())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * Convert JPA entity to domain object.
     */
    private ApiContextEntities toDomain(PostgresApiContextEntity entity) {
        return ApiContextEntities.builder()
                .id(entity.getId())
                .apiName(entity.getApiName())
                .httpMethod(entity.getHttpMethod() != null ? HttpMethod.valueOf(entity.getHttpMethod()) : null)
                .apiTemplate(entity.getApiTemplate())
                .dataRepresentation(entity.getDataRepresentation())
                .requestPayload(entity.getRequestPayload())
                .responsePayload(entity.getResponsePayload())
                .isService(entity.getIsService())
                .serviceName(entity.getServiceName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}