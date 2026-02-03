package com.acuver.autwit.adapter.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgresApiContextRepository - Spring Data JPA repository.
 *
 * <h2>INTERNAL USE ONLY</h2>
 * Used only by PostgresApiContextAdapter.
 *
 * <h2>NAMING CONVENTION</h2>
 * Spring Data JPA uses method names to generate queries automatically.
 * Pattern: findBy{Field}And{Field}OrderBy{Field}Asc/Desc
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
interface PostgresApiContextRepository extends JpaRepository<PostgresApiContextEntity, Long> {

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
    Optional<PostgresApiContextEntity> findFirstByStepKeyAndApiNameOrderByCallIndexDesc(
            String stepKey, String apiName);

    /**
     * Find all API calls from specific step.
     *
     * @param stepKey Step execution identifier
     * @return List of all API calls from this step
     */
    List<PostgresApiContextEntity> findByStepKeyOrderByCreatedAtAsc(String stepKey);

    /**
     * Find latest execution of specific step by name.
     * Used for retrieving data from previous steps.
     *
     * @param scenarioKey    Scenario identifier
     * @param stepName       Human-readable step name
     * @param apiName        API name
     * @return Latest execution of this step
     */
    Optional<PostgresApiContextEntity> findFirstByScenarioKeyAndStepNameAndApiNameOrderByStepExecutionIndexDescCreatedAtDesc(
            String scenarioKey, String stepName, String apiName);

    /**
     * Find all executions of specific step (including reruns).
     *
     * @param scenarioKey Scenario identifier
     * @param stepName    Human-readable step name
     * @return List of all executions ordered by execution index
     */
    List<PostgresApiContextEntity> findByScenarioKeyAndStepNameOrderByStepExecutionIndexAscCreatedAtAsc(
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
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @param callIndex   Call index
     * @return API context entity if found
     */
    Optional<PostgresApiContextEntity> findByScenarioKeyAndApiNameAndCallIndex(
            String scenarioKey, String apiName, int callIndex);

    /**
     * Find first API call to specific API in scenario.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return First API call (oldest)
     */
    Optional<PostgresApiContextEntity> findFirstByScenarioKeyAndApiNameOrderByCreatedAtAsc(
            String scenarioKey, String apiName);

    /**
     * Find all calls to same API within scenario, ordered by call index.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return List of entities ordered by callIndex ASC
     */
    List<PostgresApiContextEntity> findByScenarioKeyAndApiNameOrderByCallIndexAsc(
            String scenarioKey, String apiName);

    /**
     * Find all calls to same API within scenario, ordered by creation time.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return List of entities ordered by createdAt ASC
     */
    List<PostgresApiContextEntity> findByScenarioKeyAndApiNameOrderByCreatedAtAsc(
            String scenarioKey, String apiName);

    /**
     * Find all API contexts for scenario, ordered by creation time.
     *
     * @param scenarioKey Scenario identifier
     * @return List of entities ordered by createdAt ASC
     */
    List<PostgresApiContextEntity> findByScenarioKeyOrderByCreatedAtAsc(String scenarioKey);

    /**
     * Check if API context exists for scenario and API name.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @return true if exists (any callIndex)
     */
    boolean existsByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Count API contexts for scenario.
     *
     * @param scenarioKey Scenario identifier
     * @return Count
     */
    long countByScenarioKey(String scenarioKey);

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
    Optional<PostgresApiContextEntity> findFirstByOrderNoOrderByCreatedAtAsc(String orderNo);

    /**
     * Find all API calls for specific order.
     * Used for order lifecycle analysis.
     *
     * @param orderNo Order number
     * @return All API calls for this order
     */
    List<PostgresApiContextEntity> findByOrderNoOrderByCreatedAtAsc(String orderNo);

    /**
     * Find first API call by order header key.
     *
     * @param orderHeaderKey Order header key
     * @return First API call with this order header key
     */
    Optional<PostgresApiContextEntity> findFirstByOrderHeaderKeyOrderByCreatedAtAsc(String orderHeaderKey);

    /**
     * Find all API calls by order header key.
     *
     * @param orderHeaderKey Order header key
     * @return All API calls with this order header key
     */
    List<PostgresApiContextEntity> findByOrderHeaderKeyOrderByCreatedAtAsc(String orderHeaderKey);

    // ==========================================================================
    // DELETE OPERATIONS (Retained from original)
    // ==========================================================================

    /**
     * Delete specific API call.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     * @param callIndex   Call index
     */
    void deleteByScenarioKeyAndApiNameAndCallIndex(String scenarioKey, String apiName, int callIndex);

    /**
     * Delete all calls to API within scenario.
     *
     * @param scenarioKey Scenario identifier
     * @param apiName     API name
     */
    void deleteByScenarioKeyAndApiName(String scenarioKey, String apiName);

    /**
     * Delete all API contexts for scenario.
     *
     * @param scenarioKey Scenario identifier
     */
    void deleteByScenarioKey(String scenarioKey);

    // ==========================================================================
    // GLOBAL QUERIES (Analytics Only - Retained from original)
    // ==========================================================================

    /**
     * Find by service name across all scenarios.
     * ⚠️ For analytics only - not scenario-isolated.
     *
     * @param serviceName Service name
     * @return List of entities
     */
    List<PostgresApiContextEntity> findByServiceName(String serviceName);

    /**
     * Find by HTTP method across all scenarios.
     * ⚠️ For analytics only - not scenario-isolated.
     *
     * @param httpMethod HTTP method (GET, POST, etc.)
     * @return List of entities
     */
    List<PostgresApiContextEntity> findByHttpMethod(String httpMethod);

    /**
     * Find by data representation across all scenarios.
     * ⚠️ For analytics only - not scenario-isolated.
     *
     * @param dataRepresentation Data format (XML, JSON)
     * @return List of entities
     */
    List<PostgresApiContextEntity> findByDataRepresentation(String dataRepresentation);
}