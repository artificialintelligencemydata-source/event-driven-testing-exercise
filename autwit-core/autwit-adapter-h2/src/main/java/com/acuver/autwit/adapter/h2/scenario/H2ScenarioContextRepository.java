package com.acuver.autwit.adapter.h2.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface H2ScenarioContextRepository extends JpaRepository<H2ScenarioContextEntity, UUID> {

    /**
     * Find by scenario name (for non-parameterized scenarios)
     */
    Optional<H2ScenarioContextEntity> findByScenarioName(String scenarioName);

    /**
     * Find by scenario name and example ID (business key for parameterized scenarios)
     */
    Optional<H2ScenarioContextEntity> findByScenarioNameAndExampleId(String scenarioName, String exampleId);

    /**
     * Find all by scenario status
     */
    List<H2ScenarioContextEntity> findByScenarioStatus(String scenarioStatus);

    /**
     * Check if exists by business key
     */
    boolean existsByScenarioNameAndExampleId(String scenarioName, String exampleId);

    /**
     * Delete by scenario name
     */
    void deleteByScenarioName(String scenarioName);

    Optional<H2ScenarioContextEntity> findByScenarioKey(String scenarioKey);

    void deleteByScenarioKey(String scenarioKey);
}