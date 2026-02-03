package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.ScenarioStateContextEntities;

import java.util.Optional;

public interface ScenarioContextPort {
    // Existing methods
    Optional<ScenarioStateContextEntities> findByScenarioName(String name);
    Optional<ScenarioStateContextEntities> findByScenarioKey(String scenarioKey);
    ScenarioStateContextEntities save(ScenarioStateContextEntities state);

    void deleteByScenarioKey(String scenarioKey);

    void delete(String scenarioName);
    void deleteAll();

    // Find by different criteria
//    Optional<ScenarioStateContextEntities> findByScenarioKey(String scenarioKey);
//    Optional<ScenarioStateContextEntities> findByTestCaseId(String testCaseId);
//    List<ScenarioStateContextEntities> findByScenarioStatus(String status);
//    List<ScenarioStateContextEntities> findByExampleId(String exampleId);

    // Existence checks
//    boolean existsByScenarioName(String scenarioName);
//    boolean existsByScenarioKey(String scenarioKey);

    // Bulk operations
//    List<ScenarioStateContextEntities> saveAll(List<ScenarioStateContextEntities> states);
//    void deleteByScenarioKeys(List<String> scenarioKeys);
//    List<ScenarioStateContextEntities> findAll();

    // Status updates (avoid full object fetch-modify-save)
//    void updateScenarioStatus(String scenarioKey, String newStatus);
//    void updateStepStatus(String scenarioKey, Map<String, String> stepStatus);

    // Query by date/time
//    List<ScenarioStateContextEntities> findByLastUpdatedAfter(Instant timestamp);
//    List<ScenarioStateContextEntities> findByLastUpdatedBefore(Instant timestamp);
//    List<ScenarioStateContextEntities> findByLastUpdatedBetween(Instant start, Instant end);

    // Count operations
//    long count();
//    long countByScenarioStatus(String status);
//    long countByExampleId(String exampleId);

    // Step-specific queries
//    List<ScenarioStateContextEntities> findByStepStatusContaining(String stepName, String status);
//    List<ScenarioStateContextEntities> findFailedScenarios(); // where stepStatus contains "FAILED"
//    List<ScenarioStateContextEntities> findPendingScenarios(); // where scenarioStatus is "PENDING"

    // Cleanup operations
//    void deleteByScenarioStatus(String status);
//    void deleteOlderThan(Instant timestamp);
//    int deleteStaleScenarios(Duration olderThan); // returns count deleted

    // Pagination support
    //Page<ScenarioStateContextEntities> findAll(Pageable pageable);
    //Page<ScenarioStateContextEntities> findByScenarioStatus(String status, Pageable pageable);
}
