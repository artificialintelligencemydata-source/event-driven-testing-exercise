package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.ScenarioStateContext;

import java.util.Optional;

public interface ScenarioContextPort {
    Optional<ScenarioStateContext> findByScenarioName(String name);
    ScenarioStateContext save(ScenarioStateContext state);
    void delete(String scenarioName);
    void deleteAll();
}

