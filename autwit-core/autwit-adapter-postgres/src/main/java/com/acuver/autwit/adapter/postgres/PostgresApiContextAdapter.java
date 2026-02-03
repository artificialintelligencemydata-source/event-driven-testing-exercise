package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.ApiContextEntities.HttpMethod;
import com.acuver.autwit.core.domain.StorageException;
import com.acuver.autwit.core.ports.ApiContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * PostgresApiContextAdapter - Scenario-isolated PostgreSQL storage adapter.
 *
 * <h2>ARCHITECTURE</h2>
 * Implements hexagonal architecture pattern:
 * - Implements ApiContextPort (domain interface)
 * - Uses PostgresApiContextEntity (JPA entity - internal)
 * - Uses PostgresApiContextRepository (Spring Data - internal)
 * - Converts between domain model ↔ persistence model
 *
 * <h2>CRITICAL FIX: Composite Unique Constraint</h2>
 * ✅ UNIQUE(scenario_key, api_name, call_index)
 * ✅ Prevents cross-scenario data leaks
 * ✅ Supports multiple calls to same API within scenario
 *
 * <h2>THREAD SAFETY</h2>
 * - Spring singleton bean (one instance)
 * - Repository operations are thread-safe (Spring Data JPA)
 * - Composite unique key prevents data conflicts
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
@Qualifier("storageAdapter")
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
public class PostgresApiContextAdapter implements ApiContextPort {

    private static final Logger logger = LogManager.getLogger(PostgresApiContextAdapter.class);

    private final PostgresApiContextRepository repository;

    public PostgresApiContextAdapter(PostgresApiContextRepository repository) {
        this.repository = repository;
        logger.info("PostgreSQL API Context Adapter initialized");
    }

    // ==========================================================================
    // SCENARIO-ISOLATED CRUD
    // ==========================================================================

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        try {
            validateApiContext(apiContext);
            PostgresApiContextEntity entity = toEntity(apiContext);
            PostgresApiContextEntity saved = repository.save(entity);
            logger.debug("Saved to PostgreSQL: scenario={}, api={}, callIndex={}",
                    apiContext.getScenarioKey(), apiContext.getApiName(), apiContext.getCallIndex());
            return toDomain(saved);
        } catch (Exception e) {
            logger.error("Failed to save: scenario={}, api={}",
                    apiContext.getScenarioKey(), apiContext.getApiName(), e);
            throw new StorageException("Failed to save to PostgreSQL", e);
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
    // ENTITY MAPPING (Domain ↔ Persistence)
    // ==========================================================================

    /**
     * Convert domain model to JPA entity.
     * @param domain ApiContextEntities (domain model)
     * @return PostgresApiContextEntity (JPA entity)
     */
    private PostgresApiContextEntity toEntity(ApiContextEntities domain) {
        return PostgresApiContextEntity.builder()
                .id(domain.getId())
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
                .isService(domain.getIsService())
                .orderNo(domain.getOrderNo())
                .orderHeaderKey(domain.getOrderHeaderKey())
                .serviceName(domain.getServiceName())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * Convert JPA entity to domain model.
     * @param entity PostgresApiContextEntity (JPA entity)
     * @return ApiContextEntities (domain model)
     */
    private ApiContextEntities toDomain(PostgresApiContextEntity entity) {
        return ApiContextEntities.builder()
                .id(entity.getId())
                .scenarioKey(entity.getScenarioKey())
                .testCaseId(entity.getTestCaseId())
                .exampleId(entity.getExampleId())
                .apiName(entity.getApiName())
                .callIndex(entity.getCallIndex())
                .httpMethod(entity.getHttpMethod() != null ? HttpMethod.valueOf(entity.getHttpMethod()) : null)
                .apiTemplate(entity.getApiTemplate())
                .dataRepresentation(entity.getDataRepresentation())
                .requestPayload(entity.getRequestPayload())
                .responsePayload(entity.getResponsePayload())
                .isService(entity.getIsService())
                .stepKey(entity.getStepKey())
                .stepName(entity.getStepName())
                .stepExecutionIndex(entity.getStepExecutionIndex())
                .orderNo(entity.getOrderNo())
                .orderHeaderKey(entity.getOrderHeaderKey())
                .serviceName(entity.getServiceName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private void validateApiContext(ApiContextEntities apiContext) {
        if (apiContext.getStepKey() == null || apiContext.getStepKey().isBlank()) {
            throw new IllegalArgumentException("stepKey is required");
        }
        if (apiContext.getStepName() == null || apiContext.getStepName().isBlank()) {
            throw new IllegalArgumentException("stepName is required");
        }
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
