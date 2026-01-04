package com.acuver.autwit.adapter.postgres.scenario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostgresScenarioContextRepository
        extends JpaRepository<PostgresScenarioContextEntity, String> {
}