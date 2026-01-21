package com.acuver.autwit.adapter.mongo.scenario;

import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.ApiContextEntities.HttpMethod;
import com.acuver.autwit.core.domain.StorageException;
import com.acuver.autwit.core.ports.ApiContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB adapter for API context persistence.
 *
 * <h2>ACTIVATION</h2>
 * Activated when: autwit.database.type=mongodb
 *
 * <h2>ARCHITECTURE</h2>
 * Implements {@link ApiContextPort} to provide MongoDB-backed storage
 * for {@link ApiContextEntities} domain objects.
 *
 * <h2>DEPENDENCIES</h2>
 * Requires MongoDB connection configuration in application.yml
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(name = "autwit.database.type", havingValue = "mongodb")
public class MongoApiContextAdapter implements ApiContextPort {

    private static final Logger logger = LogManager.getLogger(MongoApiContextAdapter.class);

    private final MongoApiContextRepository repository;

    public MongoApiContextAdapter(MongoApiContextRepository repository) {
        this.repository = repository;
        logger.info("MongoDB API Context Adapter initialized");
    }

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        try {
            MongoApiContextEntity document = toDocument(apiContext);
            MongoApiContextEntity saved = repository.save(document);
            logger.debug("Saved API context to MongoDB: {}", apiContext.getApiName());
            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Failed to save API context: {}", apiContext.getApiName(), e);
            throw new StorageException("Failed to save API context to MongoDB", e);
        }
    }

    @Override
    public Optional<ApiContextEntities> findByApiName(String apiName) {
        return repository.findByApiName(apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiContextEntities> findByServiceName(String serviceName) {
        return repository.findByServiceName(serviceName).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiContextEntities> findServicesOnly() {
        return repository.findByIsServiceTrue().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiContextEntities> findByHttpMethod(HttpMethod httpMethod) {
        return repository.findByHttpMethod(httpMethod.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        return repository.findByDataRepresentation(dataRepresentation).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByApiName(String apiName) {
        repository.findByApiName(apiName)
                .ifPresent(repository::delete);
        logger.debug("Deleted API context: {}", apiName);
    }

    @Override
    public boolean existsByApiName(String apiName) {
        return repository.existsByApiName(apiName);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
        logger.warn("Deleted all API contexts from MongoDB");
    }

    @Override
    public long count() {
        return repository.count();
    }

    // ==================== Document Mapping ====================

    /**
     * Convert domain object to MongoDB document.
     */
    private MongoApiContextEntity toDocument(ApiContextEntities domain) {
        return MongoApiContextEntity.builder()
                .id(domain.getId() != null ? domain.getId().toString() : null)
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
                .additionalMetadata(domain.getAdditionalMetadata())
                .build();
    }

    /**
     * Convert MongoDB document to domain object.
     */
    private ApiContextEntities toDomain(MongoApiContextEntity document) {
        return ApiContextEntities.builder()
                .id(document.getId() != null ? Long.parseLong(document.getId()) : null)
                .apiName(document.getApiName())
                .httpMethod(document.getHttpMethod() != null ? HttpMethod.valueOf(document.getHttpMethod()) : null)
                .apiTemplate(document.getApiTemplate())
                .dataRepresentation(document.getDataRepresentation())
                .requestPayload(document.getRequestPayload())
                .responsePayload(document.getResponsePayload())
                .isService(document.getIsService())
                .serviceName(document.getServiceName())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .additionalMetadata(document.getAdditionalMetadata())
                .build();
    }
}