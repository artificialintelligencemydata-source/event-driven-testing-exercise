package com.acuver.autwit.adapter.mongo.scenario;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for MongoApiContextEntity.
 *
 * <h2>VISIBILITY</h2>
 * Package-private - internal to the MongoDB adapter.
 *
 * <h2>QUERY ORGANIZATION</h2>
 * - Step-level queries (PRIMARY - for step isolation)
 * - Scenario-level queries (LEGACY SUPPORT - for backward compatibility)
 * - Business entity queries (for order correlation)
 * - Analytics queries (cross-scenario)
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Repository
@ConditionalOnProperty(name = "autwit.database.type", havingValue = "mongodb")
interface MongoApiContextRepository extends MongoRepository<MongoApiContextEntity, String> {

    // ==========================================================================
    // ✅ NEW: STEP-LEVEL QUERIES (PRIMARY)
    // ==========================================================================

    /**
     * Find last API call from specific step.
     * Used by: BaseActionsNew.getLastResponseFromCurrentStep()
     *
     * @param stepKey Step execution identifier
     * @param apiName API name
     * @return Latest API call from this step
     */
    Optional<MongoApiContextEntity> findFirstByStepKeyAndApiNameOrderByCallIndexDesc(
            String stepKey, String apiName);

    /**
     * Find all API calls from specific step.
     *
     * @param stepKey Step execution identifier
     * @return List of all API calls from this step
     */
    List<MongoApiContextEntity> findByStepKeyOrderByCreatedAtAsc(String stepKey);

    /**
     * Find latest execution of specific step by name.
     * Used for retrieving data from previous steps.
     *
     * @param scenarioKey    Scenario identifier
     * @param stepName       Human-readable step name
     * @param apiName        API name
     * @return Latest execution of this step
     */
    Optional<MongoApiContextEntity> findFirstByScenarioKeyAndStepNameAndApiNameOrderByStepExecutionIndexDescCreatedAtDesc(
            String scenarioKey, String stepName, String apiName);

    /**
     * Find all executions of specific step (including reruns).
     *
     * @param scenarioKey Scenario identifier
     * @param stepName    Human-readable step name
     * @return List of all executions ordered by execution index
     */
    List<MongoApiContextEntity> findByScenarioKeyAndStepNameOrderByStepExecutionIndexAscCreatedAtAsc(
            String scenarioKey, String stepName);

    /**
     * Delete all API calls from specific step.
     *
     * @param stepKey Step execution identifier
     */
    void deleteByStepKey(String stepKey);

    // ==========================================================================
    // SCENARIO-LEVEL QUERIES (LEGACY SUPPORT - Retained from original)
    // ==========================================================================

    /**
     * Find specific API call by composite key.
     */
    Optional<MongoApiContextEntity> findByScenarioKeyAndApiNameAndCallIndex(
            String scenarioKey, String apiName, int callIndex);

    /**
     * Find first API call to specific API in scenario.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return First API call (oldest)
     */
    Optional<MongoApiContextEntity> findFirstByScenarioKeyAndApiNameOrderByCreatedAtAsc(
            String scenarioKey, String apiName);

    /**
     * Find all calls to same API within scenario, ordered by call index.
     */
    List<MongoApiContextEntity> findByScenarioKeyAndApiNameOrderByCallIndexAsc(
            String scenarioKey, String apiName);

    /**
     * Find all calls to same API within scenario, ordered by creation time.
     */
    List<MongoApiContextEntity> findByScenarioKeyAndApiNameOrderByCreatedAtAsc(
            String scenarioKey, String apiName);

    /**
     * Find all API contexts for scenario, ordered by creation time.
     */
    List<MongoApiContextEntity> findByScenarioKeyOrderByCreatedAtAsc(String scenarioKey);

    /**
     * Check if API context exists for scenario and API name.
     */
    boolean existsByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Count API contexts for scenario.
     */
    long countByScenarioKey(String scenarioKey);

    /**
     * Delete specific API call.
     */
    void deleteByScenarioKeyAndApiNameAndCallIndex(String scenarioKey, String apiName, int callIndex);

    /**
     * Delete all calls to API within scenario.
     */
    void deleteByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Delete all API contexts for scenario.
     */
    void deleteByScenarioKey(String scenarioKey);

    // ==========================================================================
    // ✅ NEW: BUSINESS ENTITY CORRELATION QUERIES
    // ==========================================================================

    /**
     * Find first API call for specific order.
     * Used for order progression tracking.
     *
     * @param orderNo Order number
     * @return First API call for this order
     */
    Optional<MongoApiContextEntity> findFirstByOrderNoOrderByCreatedAtAsc(String orderNo);

    /**
     * Find all API calls for specific order.
     * Used for order lifecycle analysis.
     *
     * @param orderNo Order number
     * @return All API calls for this order
     */
    List<MongoApiContextEntity> findByOrderNoOrderByCreatedAtAsc(String orderNo);

    /**
     * Find first API call by order header key.
     *
     * @param orderHeaderKey Order header key
     * @return First API call with this order header key
     */
    Optional<MongoApiContextEntity> findFirstByOrderHeaderKeyOrderByCreatedAtAsc(String orderHeaderKey);

    /**
     * Find all API calls by order header key.
     *
     * @param orderHeaderKey Order header key
     * @return All API calls with this order header key
     */
    List<MongoApiContextEntity> findByOrderHeaderKeyOrderByCreatedAtAsc(String orderHeaderKey);

    // ==========================================================================
    // GLOBAL QUERIES (Analytics Only - Retained from original)
    // ==========================================================================

    /**
     * Find by API name across all scenarios (analytics only).
     */
    Optional<MongoApiContextEntity> findByApiName(String apiName);

    /**
     * Check if API exists by name (analytics only).
     */
    boolean existsByApiName(String apiName);

    /**
     * Find by service name across all scenarios (analytics only).
     */
    List<MongoApiContextEntity> findByServiceName(String serviceName);

    /**
     * Find by HTTP method across all scenarios (analytics only).
     */
    List<MongoApiContextEntity> findByHttpMethod(String httpMethod);

    /**
     * Find by data representation across all scenarios (analytics only).
     */
    List<MongoApiContextEntity> findByDataRepresentation(String dataRepresentation);

    /**
     * Find all service API calls (analytics only).
     */
    List<MongoApiContextEntity> findByIsServiceTrue();
}