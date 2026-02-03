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

/**
 * MongoDB adapter for API context persistence with step-level tracking.
 *
 * <h2>ACTIVATION</h2>
 * Activated when: autwit.database.type=mongodb
 *
 * <h2>ARCHITECTURE</h2>
 * Implements {@link ApiContextPort} to provide MongoDB-backed storage
 * for {@link ApiContextEntities} domain objects.
 *
 * <h2>STEP-LEVEL ISOLATION</h2>
 * ✅ Unique index on (stepKey, apiName, callIndex)
 * ✅ Supports step reruns with stepExecutionIndex
 * ✅ Tracks business entities (orderNo, orderHeaderKey)
 *
 * <h2>DEPENDENCIES</h2>
 * Requires MongoDB connection configuration in application.yml
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongodb")
public class MongoApiContextAdapter implements ApiContextPort {

    private static final Logger logger = LogManager.getLogger(MongoApiContextAdapter.class);
    private final MongoApiContextRepository repository;

    public MongoApiContextAdapter(MongoApiContextRepository repository) {
        this.repository = repository;
        logger.info("MongoDB API Context Adapter initialized with step-level tracking");
    }

    // ==========================================================================
    // CORE CRUD
    // ==========================================================================

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        try {
            // Validate required fields
            validateApiContext(apiContext);
            MongoApiContextEntity document = toDocument(apiContext);
            MongoApiContextEntity saved = repository.save(document);

            logger.debug("Saved to MongoDB: step={}, api={}, callIndex={}",
                    apiContext.getStepKey(), apiContext.getApiName(), apiContext.getCallIndex());

            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Failed to save: step={}, api={}",
                    apiContext.getStepKey(), apiContext.getApiName(), e);
            throw new StorageException("Failed to save API context to MongoDB", e);
        }
    }

    // ==========================================================================
    // ✅ NEW: STEP-LEVEL QUERIES (Primary)
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findLastByStepKeyAndApiName(String stepKey, String apiName) {
        logger.debug("Finding last call: step={}, api={}", stepKey, apiName);
        return repository.findFirstByStepKeyAndApiNameOrderByCallIndexDesc(stepKey, apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findAllByStepKey(String stepKey) {
        logger.debug("Finding all calls for step: {}", stepKey);
        return repository.findByStepKeyOrderByCreatedAtAsc(stepKey).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ApiContextEntities> findLastByScenarioKeyAndStepNameAndApiName(
            String scenarioKey, String stepName, String apiName) {
        logger.debug("Finding last from step: scenario={}, stepName={}, api={}",
                scenarioKey, stepName, apiName);
        return repository.findFirstByScenarioKeyAndStepNameAndApiNameOrderByStepExecutionIndexDescCreatedAtDesc(
                        scenarioKey, stepName, apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findAllByScenarioKeyAndStepName(String scenarioKey, String stepName) {
        logger.debug("Finding all executions: scenario={}, stepName={}", scenarioKey, stepName);
        return repository.findByScenarioKeyAndStepNameOrderByStepExecutionIndexAscCreatedAtAsc(
                        scenarioKey, stepName).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByStepKey(String stepKey) {
        logger.debug("Deleting step: {}", stepKey);
        repository.deleteByStepKey(stepKey);
    }

    // ==========================================================================
    // ✅ NEW: BUSINESS ENTITY CORRELATION
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findByOrderNo(String orderNo) {
        logger.debug("Finding by order number: {}", orderNo);
        return repository.findFirstByOrderNoOrderByCreatedAtAsc(orderNo)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findAllByOrderNo(String orderNo) {
        logger.debug("Finding all calls for order: {}", orderNo);
        return repository.findByOrderNoOrderByCreatedAtAsc(orderNo).stream()
                .map(this::toDomain)
                .toList();
    }

    // ==========================================================================
    // SCENARIO-LEVEL QUERIES (Legacy Support)
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.debug("Finding first call: scenario={}, api={}", scenarioKey, apiName);
        return repository.findFirstByScenarioKeyAndApiNameOrderByCreatedAtAsc(scenarioKey, apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findAllByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.debug("Finding all calls: scenario={}, api={}", scenarioKey, apiName);
        return repository.findByScenarioKeyAndApiNameOrderByCreatedAtAsc(scenarioKey, apiName).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ApiContextEntities> findByScenarioKeyAndApiNameAndCallIndex(
            String scenarioKey, String apiName, int callIndex) {
        logger.debug("Finding specific call: scenario={}, api={}, callIndex={}",
                scenarioKey, apiName, callIndex);
        return repository.findByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, callIndex)
                .map(this::toDomain);
    }

    @Override
    public List<ApiContextEntities> findByScenarioKey(String scenarioKey) {
        logger.debug("Finding all calls for scenario: {}", scenarioKey);
        return repository.findByScenarioKeyOrderByCreatedAtAsc(scenarioKey).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        return repository.existsByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public void deleteByScenarioKeyAndApiNameAndCallIndex(String scenarioKey, String apiName, int callIndex) {
        logger.debug("Deleting: scenario={}, api={}, callIndex={}", scenarioKey, apiName, callIndex);
        repository.deleteByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, callIndex);
    }

    @Override
    public void deleteByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.debug("Deleting all calls: scenario={}, api={}", scenarioKey, apiName);
        repository.deleteByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public void deleteByScenarioKey(String scenarioKey) {
        logger.debug("Deleting scenario: {}", scenarioKey);
        repository.deleteByScenarioKey(scenarioKey);
    }

    @Override
    public long countByScenarioKey(String scenarioKey) {
        return repository.countByScenarioKey(scenarioKey);
    }

    // ==========================================================================
    // GLOBAL QUERIES (Analytics)
    // ==========================================================================

    @Override
    public List<ApiContextEntities> findAll() {
        logger.debug("Finding all API contexts (analytics query)");
        return repository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByServiceName(String serviceName) {
        logger.debug("Finding by service name: {} (deprecated)", serviceName);
        return repository.findByServiceName(serviceName).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByHttpMethod(HttpMethod httpMethod) {
        logger.debug("Finding by HTTP method: {} (deprecated)", httpMethod);
        return repository.findByHttpMethod(httpMethod.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        logger.debug("Finding by data representation: {} (deprecated)", dataRepresentation);
        return repository.findByDataRepresentation(dataRepresentation).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteAll() {
        logger.warn("Deleting ALL API contexts");
        repository.deleteAll();
    }

    @Override
    public long count() {
        return repository.count();
    }

    // ==========================================================================
    // DOCUMENT MAPPING (Domain ↔ MongoDB)
    // ==========================================================================

    /**
     * Convert domain model to MongoDB document.
     */
    private MongoApiContextEntity toDocument(ApiContextEntities domain) {
        return MongoApiContextEntity.builder()
                .id(domain.getId() != null ? domain.getId().toString() : null)
                .scenarioKey(domain.getScenarioKey())
                .stepKey(domain.getStepKey())
                .stepName(domain.getStepName())
                .stepExecutionIndex(domain.getStepExecutionIndex())
                .testCaseId(domain.getTestCaseId())
                .exampleId(domain.getExampleId())
                .apiName(domain.getApiName())
                .callIndex(domain.getCallIndex() != null ? domain.getCallIndex() : 0)
                .httpMethod(domain.getHttpMethod() != null ? domain.getHttpMethod().name() : null)
                .apiTemplate(domain.getApiTemplate())
                .dataRepresentation(domain.getDataRepresentation())
                .requestPayload(domain.getRequestPayload())
                .responsePayload(domain.getResponsePayload())
                .isService(domain.getIsService() != null ? domain.getIsService() : false)
                .serviceName(domain.getServiceName())
                .orderNo(domain.getOrderNo())
                .orderHeaderKey(domain.getOrderHeaderKey())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * Convert MongoDB document to domain model.
     */
    private ApiContextEntities toDomain(MongoApiContextEntity document) {
        return ApiContextEntities.builder()
                .id(document.getId() != null ? Long.parseLong(document.getId()) : null)
                .scenarioKey(document.getScenarioKey())
                .testCaseId(document.getTestCaseId())
                .exampleId(document.getExampleId())
                .stepKey(document.getStepKey())
                .stepName(document.getStepName())
                .stepExecutionIndex(document.getStepExecutionIndex())
                .apiName(document.getApiName())
                .callIndex(document.getCallIndex())
                .httpMethod(document.getHttpMethod() != null ? HttpMethod.valueOf(document.getHttpMethod()) : null)
                .apiTemplate(document.getApiTemplate())
                .dataRepresentation(document.getDataRepresentation())
                .requestPayload(document.getRequestPayload())
                .responsePayload(document.getResponsePayload())
                .isService(document.getIsService())
                .serviceName(document.getServiceName())
                .orderNo(document.getOrderNo())
                .orderHeaderKey(document.getOrderHeaderKey())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    // ==========================================================================
    // VALIDATION
    // ==========================================================================

    /**
     * Validate API context before saving.
     *
     * @param apiContext API context to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateApiContext(ApiContextEntities apiContext) {
        if (apiContext.getStepKey() == null || apiContext.getStepKey().isBlank()) {
            throw new IllegalArgumentException("stepKey is required");
        }
//        if (apiContext.getStepName() == null || apiContext.getStepName().isBlank()) {
//            throw new IllegalArgumentException("stepName is required");
//        }
        if (apiContext.getScenarioKey() == null || apiContext.getScenarioKey().isBlank()) {
            throw new IllegalArgumentException("scenarioKey is required");
        }
        if (apiContext.getApiName() == null || apiContext.getApiName().isBlank()) {
            throw new IllegalArgumentException("apiName is required");
        }
        if (apiContext.getHttpMethod() == null) {
            throw new IllegalArgumentException("httpMethod is required");
        }
    }
}