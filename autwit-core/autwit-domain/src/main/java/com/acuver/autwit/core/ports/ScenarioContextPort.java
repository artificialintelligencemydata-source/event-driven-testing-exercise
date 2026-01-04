package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.ScenarioContext;

import java.util.Optional;

public interface ScenarioContextPort {
    Optional<ScenarioContext> findByScenarioName(String name);
    ScenarioContext save(ScenarioContext state);
    void delete(String scenarioName);
    void deleteAll();
}

