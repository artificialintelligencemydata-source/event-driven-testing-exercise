package com.acuver.autwit.adapter.mongo.scenario;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoScenarioContextRepository
        extends MongoRepository<MongoScenarioContextEntity, String> {
    Optional<MongoScenarioContextEntity> findByScenarioKey(String scenarioKey);
    void deleteByScenarioKey(String scenarioKey);
}