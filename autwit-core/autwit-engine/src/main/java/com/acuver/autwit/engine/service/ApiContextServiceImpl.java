package com.acuver.autwit.engine.service;

import com.acuver.autwit.core.domain.ApiCallStatistics;
import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.ApiContextEntities.HttpMethod;
import com.acuver.autwit.core.ports.ApiContextPort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ApiContextService - Business logic for step-isolated API context.
 *
 * <h2>ARCHITECTURE STYLE</h2>
 * Matches ScenarioStateServiceImpl pattern:
 * - Simple, direct delegation to storage adapter
 * - Business logic in service layer
 * - No complex error handling - let exceptions bubble up
 * - Defensive initialization where needed
 *
 * <h2>STEP-LEVEL TRACKING (v2.0)</h2>
 * - Primary: Step-level isolation (stepKey)
 * - Secondary: Scenario-level queries (backward compatibility)
 * - Business: Order correlation (cross-scenario tracking)
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
@Qualifier("apiContextService")
public class ApiContextServiceImpl implements ApiContextPort {

    private static final Logger logger = LogManager.getLogger(ApiContextServiceImpl.class);

    private final ApiContextPort storageAdapter;
    private final RuntimeContextPort runtimeContextPort;

    @Autowired
    public ApiContextServiceImpl(
            @Qualifier("storageAdapter") ApiContextPort storageAdapter,
            RuntimeContextPort runtimeContextPort) {
        this.storageAdapter = storageAdapter;
        this.runtimeContextPort = runtimeContextPort;
        logger.info("ApiContextService initialized with storage: {} (step-level tracking enabled)",
                storageAdapter.getClass().getSimpleName());
    }

    // ==========================================================================
    // CORE CRUD
    // ==========================================================================

    @Override
    public ApiContextEntities save(ApiContextEntities apiContext) {
        logger.debug("Saving API context: step={}, api={}, callIndex={}",
                apiContext.getStepKey(), apiContext.getApiName(), apiContext.getCallIndex());

        // 1️⃣ Validate required fields (fail-fast)
        validateApiContext(apiContext);

        // 2️⃣ Auto-set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (apiContext.getCreatedAt() == null) {
            apiContext.setCreatedAt(now);
        }
        apiContext.setUpdatedAt(now);

        // 3️⃣ Defensive initialization
        if (apiContext.getApiTemplate() == null || apiContext.getApiTemplate().isBlank()) {
            apiContext.setApiTemplate("N/A");
        }

        if (apiContext.getCallIndex() == null) {
            apiContext.setCallIndex(0);
        }

//        if (apiContext.getStepExecutionIndex() == null) {
//            apiContext.setStepExecutionIndex(0);
//        }

        // 4️⃣ Persist (let database handle uniqueness constraints)
        ApiContextEntities saved = storageAdapter.save(apiContext);

        logger.info("Saved API context: step={}, stepName='{}', api={}, callIndex={}, isService={}",
                saved.getStepKey(), saved.getApiName(),
                saved.getCallIndex(), saved.getIsService());

        return saved;
    }

    // ==========================================================================
    // ✅ NEW: STEP-LEVEL QUERIES (Primary)
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findLastByStepKeyAndApiName(String stepKey, String apiName) {
        logger.debug("Finding last call from step: step={}, api={}", stepKey, apiName);

        if (stepKey == null || stepKey.isBlank() || apiName == null || apiName.isBlank()) {
            logger.warn("findLastByStepKeyAndApiName called with null/empty parameters");
            return Optional.empty();
        }

        return storageAdapter.findLastByStepKeyAndApiName(stepKey, apiName);
    }

    @Override
    public List<ApiContextEntities> findAllByStepKey(String stepKey) {
        logger.debug("Finding all calls for step: {}", stepKey);

        if (stepKey == null || stepKey.isBlank()) {
            logger.warn("findAllByStepKey called with null/empty stepKey");
            return List.of();
        }

        return storageAdapter.findAllByStepKey(stepKey);
    }

    @Override
    public Optional<ApiContextEntities> findLastByScenarioKeyAndStepNameAndApiName(
            String scenarioKey, String stepName, String apiName) {
        logger.debug("Finding last from step by name: scenario={}, stepName='{}', api={}",
                scenarioKey, stepName, apiName);

        if (scenarioKey == null || scenarioKey.isBlank() ||
                stepName == null || stepName.isBlank() ||
                apiName == null || apiName.isBlank()) {
            logger.warn("findLastByScenarioKeyAndStepNameAndApiName called with null/empty parameters");
            return Optional.empty();
        }

        return storageAdapter.findLastByScenarioKeyAndStepNameAndApiName(scenarioKey, stepName, apiName);
    }

    @Override
    public List<ApiContextEntities> findAllByScenarioKeyAndStepName(String scenarioKey, String stepName) {
        logger.debug("Finding all executions of step: scenario={}, stepName='{}'", scenarioKey, stepName);

        if (scenarioKey == null || scenarioKey.isBlank() || stepName == null || stepName.isBlank()) {
            logger.warn("findAllByScenarioKeyAndStepName called with null/empty parameters");
            return List.of();
        }

        return storageAdapter.findAllByScenarioKeyAndStepName(scenarioKey, stepName);
    }

    @Override
    public void deleteByStepKey(String stepKey) {
        logger.info("Deleting all API contexts for step: {}", stepKey);

        if (stepKey == null || stepKey.isBlank()) {
            logger.warn("deleteByStepKey called with null/empty stepKey");
            return;
        }

        storageAdapter.deleteByStepKey(stepKey);
    }

    // ==========================================================================
    // ✅ NEW: BUSINESS ENTITY CORRELATION
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findByOrderNo(String orderNo) {
        logger.debug("Finding API context by order number: {}", orderNo);

        if (orderNo == null || orderNo.isBlank()) {
            logger.warn("findByOrderNo called with null/empty orderNo");
            return Optional.empty();
        }

        return storageAdapter.findByOrderNo(orderNo);
    }

    @Override
    public List<ApiContextEntities> findAllByOrderNo(String orderNo) {
        logger.debug("Finding all API contexts for order: {}", orderNo);

        if (orderNo == null || orderNo.isBlank()) {
            logger.warn("findAllByOrderNo called with null/empty orderNo");
            return List.of();
        }

        return storageAdapter.findAllByOrderNo(orderNo);
    }

    // ==========================================================================
    // SCENARIO-LEVEL QUERIES (Legacy Support)
    // ==========================================================================

    @Override
    public Optional<ApiContextEntities> findByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.debug("Finding API context: scenario={}, api={}", scenarioKey, apiName);

        if (scenarioKey == null || scenarioKey.isBlank() || apiName == null || apiName.isBlank()) {
            logger.warn("findByScenarioKeyAndApiName called with null/empty parameters");
            return Optional.empty();
        }

        return storageAdapter.findByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public List<ApiContextEntities> findAllByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.debug("Finding all calls: scenario={}, api={}", scenarioKey, apiName);

        if (scenarioKey == null || scenarioKey.isBlank() || apiName == null || apiName.isBlank()) {
            logger.warn("findAllByScenarioKeyAndApiName called with null/empty parameters");
            return List.of();
        }

        return storageAdapter.findAllByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public Optional<ApiContextEntities> findByScenarioKeyAndApiNameAndCallIndex(
            String scenarioKey, String apiName, int callIndex) {
        logger.debug("Finding specific call: scenario={}, api={}, callIndex={}",
                scenarioKey, apiName, callIndex);

        if (scenarioKey == null || scenarioKey.isBlank() || apiName == null || apiName.isBlank()) {
            logger.warn("findByScenarioKeyAndApiNameAndCallIndex called with null/empty parameters");
            return Optional.empty();
        }

        return storageAdapter.findByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, callIndex);
    }

    @Override
    public List<ApiContextEntities> findByScenarioKey(String scenarioKey) {
        logger.debug("Finding all API contexts for scenario: {}", scenarioKey);

        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.warn("findByScenarioKey called with null/empty scenarioKey");
            return List.of();
        }

        return storageAdapter.findByScenarioKey(scenarioKey);
    }

    @Override
    public boolean existsByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        if (scenarioKey == null || scenarioKey.isBlank() || apiName == null || apiName.isBlank()) {
            return false;
        }
        return storageAdapter.existsByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public void deleteByScenarioKeyAndApiNameAndCallIndex(String scenarioKey, String apiName, int callIndex) {
        logger.info("Deleting API context: scenario={}, api={}, callIndex={}",
                scenarioKey, apiName, callIndex);
        storageAdapter.deleteByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, callIndex);
    }

    @Override
    public void deleteByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        logger.info("Deleting all calls: scenario={}, api={}", scenarioKey, apiName);
        storageAdapter.deleteByScenarioKeyAndApiName(scenarioKey, apiName);
    }

    @Override
    public void deleteByScenarioKey(String scenarioKey) {
        logger.info("Deleting all API contexts for scenario: {}", scenarioKey);

        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.warn("deleteByScenarioKey called with null/empty scenarioKey");
            return;
        }

        storageAdapter.deleteByScenarioKey(scenarioKey);
    }

    @Override
    public long countByScenarioKey(String scenarioKey) {
        if (scenarioKey == null || scenarioKey.isBlank()) {
            return 0;
        }
        return storageAdapter.countByScenarioKey(scenarioKey);
    }

    // ==========================================================================
    // GLOBAL QUERIES (Analytics Only)
    // ==========================================================================

    @Override
    public List<ApiContextEntities> findAll() {
        logger.debug("Finding all API contexts (global query)");
        return storageAdapter.findAll();
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByServiceName(String serviceName) {
        logger.warn("Using deprecated global query: findByServiceName");
        return storageAdapter.findByServiceName(serviceName);
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByHttpMethod(HttpMethod httpMethod) {
        logger.warn("Using deprecated global query: findByHttpMethod");
        return storageAdapter.findByHttpMethod(httpMethod);
    }

    @Override
    @Deprecated
    public List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        logger.warn("Using deprecated global query: findByDataRepresentation");
        return storageAdapter.findByDataRepresentation(dataRepresentation);
    }

    @Override
    public void deleteAll() {
        logger.warn("Deleting ALL API contexts across all scenarios");
        storageAdapter.deleteAll();
    }

    @Override
    public long count() {
        return storageAdapter.count();
    }

    // ==========================================================================
    // STATISTICS (Business Logic)
    // ==========================================================================

    @Override
    public ApiCallStatistics getStatistics() {
        logger.debug("Calculating global API call statistics");
        return calculateStatistics(storageAdapter.findAll());
    }

    @Override
    public ApiCallStatistics getStatisticsByScenarioKey(String scenarioKey) {
        logger.debug("Calculating statistics for scenario: {}", scenarioKey);

        if (scenarioKey == null || scenarioKey.isBlank()) {
            throw new IllegalArgumentException("scenarioKey cannot be null or empty");
        }

        return calculateStatistics(storageAdapter.findByScenarioKey(scenarioKey));
    }

    /**
     * Calculate statistics for a step.
     *
     * @param stepKey Step execution identifier
     * @return Statistics for this step
     */
    public ApiCallStatistics getStatisticsByStepKey(String stepKey) {
        logger.debug("Calculating statistics for step: {}", stepKey);

        if (stepKey == null || stepKey.isBlank()) {
            throw new IllegalArgumentException("stepKey cannot be null or empty");
        }

        return calculateStatistics(storageAdapter.findAllByStepKey(stepKey));
    }

    /**
     * Calculate statistics for an order.
     *
     * @param orderNo Order number
     * @return Statistics for this order
     */
    public ApiCallStatistics getStatisticsByOrderNo(String orderNo) {
        logger.debug("Calculating statistics for order: {}", orderNo);

        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("orderNo cannot be null or empty");
        }

        return calculateStatistics(storageAdapter.findAllByOrderNo(orderNo));
    }

    private ApiCallStatistics calculateStatistics(List<ApiContextEntities> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return ApiCallStatistics.builder()
                    .totalCalls(0L)
                    .apiCalls(0L)
                    .serviceCalls(0L)
                    .callsByHttpMethod(Map.of())
                    .callsByDataRepresentation(Map.of())
                    .mostUsedApi("N/A")
                    .mostUsedService("N/A")
                    .build();
        }

        long total = contexts.size();
        long services = contexts.stream().filter(ApiContextEntities::getIsService).count();
        long apis = total - services;

        Map<String, Long> byMethod = contexts.stream()
                .filter(ctx -> ctx.getHttpMethod() != null)
                .collect(Collectors.groupingBy(
                        ctx -> ctx.getHttpMethod().name(),
                        Collectors.counting()
                ));

        Map<String, Long> byFormat = contexts.stream()
                .filter(ctx -> ctx.getDataRepresentation() != null)
                .collect(Collectors.groupingBy(
                        ApiContextEntities::getDataRepresentation,
                        Collectors.counting()
                ));

        String mostUsedApi = contexts.stream()
                .filter(ctx -> !ctx.getIsService())
                .collect(Collectors.groupingBy(ApiContextEntities::getApiName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String mostUsedService = contexts.stream()
                .filter(ApiContextEntities::getIsService)
                .filter(ctx -> ctx.getServiceName() != null)
                .collect(Collectors.groupingBy(ApiContextEntities::getServiceName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return ApiCallStatistics.builder()
                .totalCalls(total)
                .apiCalls(apis)
                .serviceCalls(services)
                .callsByHttpMethod(byMethod)
                .callsByDataRepresentation(byFormat)
                .mostUsedApi(mostUsedApi)
                .mostUsedService(mostUsedService)
                .build();
    }

    // ==========================================================================
    // VALIDATION (Private Helper)
    // ==========================================================================

    private void validateApiContext(ApiContextEntities apiContext) {
        if (apiContext == null) {
            throw new IllegalArgumentException("API context cannot be null");
        }

        // ✅ CRITICAL: Step-level fields must exist
        if (apiContext.getStepKey() == null || apiContext.getStepKey().isBlank()) {
            throw new IllegalArgumentException(
                    "stepKey is REQUIRED. Ensure it's set from RuntimeContextPort in Hooks.setupStepContext()");
        }

//        if (apiContext.getStepName() == null || apiContext.getStepName().isBlank()) {
//            throw new IllegalArgumentException(
//                    "stepName is REQUIRED. Ensure it's set from RuntimeContextPort in Hooks.setupStepContext()");
//        }

        // ✅ CRITICAL: scenarioKey must exist
        if (apiContext.getScenarioKey() == null || apiContext.getScenarioKey().isBlank()) {
            throw new IllegalArgumentException(
                    "scenarioKey is REQUIRED. Ensure RuntimeContextPort is initialized in Hooks.setupScenarioContext()");
        }

        if (apiContext.getApiName() == null || apiContext.getApiName().isBlank()) {
            throw new IllegalArgumentException("apiName is required");
        }

        if (apiContext.getHttpMethod() == null) {
            throw new IllegalArgumentException("httpMethod is required");
        }

        if (apiContext.getDataRepresentation() == null || apiContext.getDataRepresentation().isBlank()) {
            throw new IllegalArgumentException("dataRepresentation is required");
        }

        if (apiContext.getIsService() == null) {
            throw new IllegalArgumentException("isService flag is required");
        }

        if (apiContext.getIsService() &&
                (apiContext.getServiceName() == null || apiContext.getServiceName().isBlank())) {
            throw new IllegalArgumentException("serviceName required when isService=true");
        }
    }

    // ==========================================================================
    // UTILITY METHODS (Similar to ScenarioStateServiceImpl)
    // ==========================================================================

    /**
     * Clear all API contexts for a step (cleanup utility).
     */
    public void clearStep(String stepKey) {
        logger.info("Clearing all API contexts for step: {}", stepKey);

        if (stepKey == null || stepKey.isBlank()) {
            logger.warn("clearStep called with null/empty stepKey");
            return;
        }

        storageAdapter.deleteByStepKey(stepKey);
    }

    /**
     * Clear all API contexts for a scenario (cleanup utility).
     */
    public void clear(String scenarioKey) {
        logger.info("Clearing all API contexts for scenario: {}", scenarioKey);

        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.warn("clear called with null/empty scenarioKey");
            return;
        }

        storageAdapter.deleteByScenarioKey(scenarioKey);
    }

    /**
     * Clear all API contexts (cleanup utility).
     */
    public void clearAll() {
        logger.warn("Clearing ALL API contexts across all scenarios");
        storageAdapter.deleteAll();
    }

    /**
     * Get step execution count (how many times a step has run).
     *
     * @param scenarioKey Scenario identifier
     * @param stepName Step name
     * @return Number of times this step has executed
     */
//    public int getStepExecutionCount(String scenarioKey, String stepName) {
//        if (scenarioKey == null || scenarioKey.isBlank() || stepName == null || stepName.isBlank()) {
//            return 0;
//        }
//
//        List<ApiContextEntities> executions = storageAdapter.findAllByScenarioKeyAndStepName(scenarioKey, stepName);
//
//        return (int) executions.stream()
//                .map(ApiContextEntities::getStepExecutionIndex)
//                .distinct()
//                .count();
//    }
}