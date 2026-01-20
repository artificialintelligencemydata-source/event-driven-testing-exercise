package com.acuver.autwit.adapter.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostgresScenarioContextRepository
        extends JpaRepository<PostgresScenarioContextEntity, UUID> {
    Optional<PostgresScenarioContextEntity> findByScenarioName(String scenarioName);
    Optional<PostgresScenarioContextEntity> findByScenarioKey(String scenarioKey);
    boolean existsByScenarioKey(String scenarioKey);
    List<PostgresScenarioContextEntity> findByScenarioStatus(String status);
    void deleteByScenarioKey(String scenarioKey);
    void deleteByLastUpdatedBefore(Long timestamp);
}