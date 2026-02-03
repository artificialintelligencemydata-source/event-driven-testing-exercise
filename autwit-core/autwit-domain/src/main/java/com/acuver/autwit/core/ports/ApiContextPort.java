package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.ApiCallStatistics;
import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.domain.ApiContextEntities.HttpMethod;

import java.util.List;
import java.util.Optional;

/**
 * ApiContextPort - Step-isolated API context persistence.
 *
 * <h2>CRITICAL: STEP-LEVEL ISOLATION</h2>
 * All operations are step-aware using composite key (stepKey + apiName + callIndex).
 * This prevents cross-step data leaks in parallel execution and supports step reruns.
 *
 * <h2>IDENTITY MODEL</h2>
 * Primary identity: (stepKey, apiName, callIndex)
 * - stepKey format: {scenarioKey}_s{stepHash4}_{execIdx}
 * - scenarioKey format: {scenarioName}_{exampleId}_T{threadId}_{uuid8}_{timestamp}
 * - stepExecutionIndex: Tracks step reruns (0, 1, 2...)
 * - callIndex: Supports multiple calls to same API within one step
 *
 * <h2>BACKWARD COMPATIBILITY</h2>
 * Scenario-level methods retained for legacy support.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public interface ApiContextPort {

    // ==========================================================================
    // ✅ NEW: STEP-LEVEL ISOLATION (Primary Methods)
    // ==========================================================================

    /**
     * Find last API call from current step.
     * Used by: BaseActionsNew.getLastResponseFromCurrentStep()
     *
     * <h3>USE CASE</h3>
     * When a step calls the same API multiple times, this returns the most recent call.
     *
     * @param stepKey Step execution identifier
     * @param apiName API name
     * @return Latest API call from this step
     */
    Optional<ApiContextEntities> findLastByStepKeyAndApiName(String stepKey, String apiName);

    /**
     * Find all API calls from current step.
     *
     * @param stepKey Step execution identifier
     * @return All API calls from this step (ordered by creation time)
     */
    List<ApiContextEntities> findAllByStepKey(String stepKey);

    /**
     * Find last response from a previous step by step name.
     * Used by: BaseActionsNew.getResponseFromStep()
     *
     * <h3>USE CASE</h3>
     * Retrieve data from "I create an order" step in current "I check order status" step.
     * Returns the latest execution if the step was rerun multiple times.
     *
     * @param scenarioKey Scenario identifier
     * @param stepName    Human-readable step name (e.g., "I create an order")
     * @param apiName     API name
     * @return Latest response from that step
     */
    Optional<ApiContextEntities> findLastByScenarioKeyAndStepNameAndApiName(
            String scenarioKey, String stepName, String apiName);

    /**
     * Find all executions of a step (including reruns).
     *
     * <h3>USE CASE</h3>
     * Analyze step behavior across reruns (step_execution_index: 0, 1, 2...).
     *
     * @param scenarioKey Scenario identifier
     * @param stepName    Human-readable step name
     * @return All executions ordered by execution index
     */
    List<ApiContextEntities> findAllByScenarioKeyAndStepName(String scenarioKey, String stepName);

    /**
     * Delete all API calls from specific step.
     *
     * @param stepKey Step execution identifier
     */
    void deleteByStepKey(String stepKey);

    // ==========================================================================
    // ✅ NEW: BUSINESS ENTITY CORRELATION
    // ==========================================================================

    /**
     * Find API response by order number.
     * Used for cross-scenario order tracking.
     *
     * <h3>USE CASE</h3>
     * Find the original createOrder API call for order "ORD-12345"
     * even if it was created in a different scenario.
     *
     * @param orderNo Order number
     * @return First API call with this order number
     */
    Optional<ApiContextEntities> findByOrderNo(String orderNo);

    /**
     * Find all API calls for specific order.
     * Useful for order lifecycle analysis.
     *
     * @param orderNo Order number
     * @return All API calls for this order (across scenarios)
     */
    List<ApiContextEntities> findAllByOrderNo(String orderNo);

    // ==========================================================================
    // SCENARIO-ISOLATED CRUD (Legacy Support - Retained)
    // ==========================================================================

    /**
     * Save or update API context with step isolation.
     *
     * <h3>THREAD SAFETY</h3>
     * Uses composite key (stepKey + apiName + callIndex) to prevent collisions.
     *
     * <h3>VALIDATION</h3>
     * - stepKey REQUIRED (throws IllegalArgumentException if missing)
     * - stepName REQUIRED
     * - scenarioKey REQUIRED
     * - apiName REQUIRED
     * - httpMethod REQUIRED
     *
     * @param apiContext API context to save
     * @return Saved entity with generated ID
     * @throws IllegalArgumentException if required fields are missing
     */
    ApiContextEntities save(ApiContextEntities apiContext);

    /**
     * Find API context by scenario and API name.
     * Returns the FIRST call found (use findAllByScenarioKeyAndApiName for all).
     *
     * @param scenarioKey Unique scenario identifier
     * @param apiName     API name
     * @return API context if found
     */
    Optional<ApiContextEntities> findByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Find ALL calls to the same API within a scenario.
     * Useful when an API is called multiple times across different steps.
     *
     * @param scenarioKey Unique scenario identifier
     * @param apiName     API name
     * @return List of all calls (ordered by creation time)
     */
    List<ApiContextEntities> findAllByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Find specific API call by scenario, API name, and call index.
     *
     * @param scenarioKey Unique scenario identifier
     * @param apiName     API name
     * @param callIndex   Call index (0-based)
     * @return API context if found
     * @deprecated Use step-level queries instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default Optional<ApiContextEntities> findByScenarioKeyAndApiNameAndCallIndex(
            String scenarioKey, String apiName, int callIndex) {
        // Default implementation for backward compatibility
        return Optional.empty();
    }

    /**
     * Find all API contexts for a specific scenario.
     *
     * @param scenarioKey Scenario identifier
     * @return List of all API contexts for this scenario
     */
    List<ApiContextEntities> findByScenarioKey(String scenarioKey);

    /**
     * Check if API context exists for scenario and API name.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return true if exists
     * @deprecated Use step-level queries instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default boolean existsByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        return findByScenarioKeyAndApiName(scenarioKey, apiName).isPresent();
    }

    /**
     * Delete specific API call.
     *
     * @deprecated Use deleteByStepKey instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default void deleteByScenarioKeyAndApiNameAndCallIndex(String scenarioKey, String apiName, int callIndex) {
        // Default empty implementation for backward compatibility
    }

    /**
     * Delete all calls to an API within a scenario.
     *
     * @deprecated Use deleteByStepKey instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default void deleteByScenarioKeyAndApiName(String scenarioKey, String apiName) {
        // Default empty implementation for backward compatibility
    }

    /**
     * Delete all API contexts for a scenario.
     *
     * @param scenarioKey Scenario identifier
     */
    void deleteByScenarioKey(String scenarioKey);

    /**
     * Count API calls for a scenario.
     *
     * @param scenarioKey Scenario identifier
     * @return Call count
     */
    long countByScenarioKey(String scenarioKey);

    // ==========================================================================
    // GLOBAL QUERIES (Analytics Only - Unsafe for Retrieval)
    // ==========================================================================

    /**
     * Find all API contexts across ALL scenarios.
     * <p>
     * ⚠️ WARNING: This is a global query and should ONLY be used for analytics.
     * Never use this for retrieving scenario-specific data.
     *
     * @return All API contexts
     */
    List<ApiContextEntities> findAll();

    /**
     * Find by service name across all scenarios (analytics only).
     *
     * @deprecated Use scenario-aware queries instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default List<ApiContextEntities> findByServiceName(String serviceName) {
        throw new UnsupportedOperationException("Use scenario-aware queries");
    }

    /**
     * Find by HTTP method across all scenarios (analytics only).
     *
     * @deprecated Use scenario-aware queries instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default List<ApiContextEntities> findByHttpMethod(HttpMethod httpMethod) {
        throw new UnsupportedOperationException("Use scenario-aware queries");
    }

    /**
     * Find by data representation across all scenarios (analytics only).
     *
     * @deprecated Use scenario-aware queries instead
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    default List<ApiContextEntities> findByDataRepresentation(String dataRepresentation) {
        throw new UnsupportedOperationException("Use scenario-aware queries");
    }

    /**
     * Delete all API contexts across all scenarios.
     * <p>
     * ⚠️ WARNING: This is destructive. Use with extreme caution.
     */
    void deleteAll();

    /**
     * Count all API contexts across all scenarios.
     */
    long count();

    // ==========================================================================
    // STATISTICS (Service Layer Only)
    // ==========================================================================

    /**
     * Get statistics across all scenarios.
     *
     * @throws UnsupportedOperationException if called on storage adapter
     */
    default ApiCallStatistics getStatistics() {
        throw new UnsupportedOperationException(
                "Statistics only available from service layer (ApiContextServiceImpl)");
    }

    /**
     * Get statistics for a specific scenario.
     *
     * @param scenarioKey Scenario identifier
     * @throws UnsupportedOperationException if called on storage adapter
     */
    default ApiCallStatistics getStatisticsByScenarioKey(String scenarioKey) {
        throw new UnsupportedOperationException(
                "Statistics only available from service layer (ApiContextServiceImpl)");
    }

}