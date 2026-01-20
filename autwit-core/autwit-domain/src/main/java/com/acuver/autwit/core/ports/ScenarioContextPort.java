package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.ScenarioStateContext;

import java.awt.print.Pageable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScenarioContextPort {
    // Existing methods
    Optional<ScenarioStateContext> findByScenarioName(String name);
    ScenarioStateContext save(ScenarioStateContext state);
    void delete(String scenarioName);
    void deleteAll();

    // Find by different criteria
//    Optional<ScenarioStateContext> findByScenarioKey(String scenarioKey);
//    Optional<ScenarioStateContext> findByTestCaseId(String testCaseId);
//    List<ScenarioStateContext> findByScenarioStatus(String status);
//    List<ScenarioStateContext> findByExampleId(String exampleId);

    // Existence checks
//    boolean existsByScenarioName(String scenarioName);
//    boolean existsByScenarioKey(String scenarioKey);

    // Bulk operations
//    List<ScenarioStateContext> saveAll(List<ScenarioStateContext> states);
//    void deleteByScenarioKeys(List<String> scenarioKeys);
//    List<ScenarioStateContext> findAll();

    // Status updates (avoid full object fetch-modify-save)
//    void updateScenarioStatus(String scenarioKey, String newStatus);
//    void updateStepStatus(String scenarioKey, Map<String, String> stepStatus);

    // Query by date/time
//    List<ScenarioStateContext> findByLastUpdatedAfter(Instant timestamp);
//    List<ScenarioStateContext> findByLastUpdatedBefore(Instant timestamp);
//    List<ScenarioStateContext> findByLastUpdatedBetween(Instant start, Instant end);

    // Count operations
//    long count();
//    long countByScenarioStatus(String status);
//    long countByExampleId(String exampleId);

    // Step-specific queries
//    List<ScenarioStateContext> findByStepStatusContaining(String stepName, String status);
//    List<ScenarioStateContext> findFailedScenarios(); // where stepStatus contains "FAILED"
//    List<ScenarioStateContext> findPendingScenarios(); // where scenarioStatus is "PENDING"

    // Cleanup operations
//    void deleteByScenarioStatus(String status);
//    void deleteOlderThan(Instant timestamp);
//    int deleteStaleScenarios(Duration olderThan); // returns count deleted

    // Pagination support
    //Page<ScenarioStateContext> findAll(Pageable pageable);
    //Page<ScenarioStateContext> findByScenarioStatus(String status, Pageable pageable);
}
