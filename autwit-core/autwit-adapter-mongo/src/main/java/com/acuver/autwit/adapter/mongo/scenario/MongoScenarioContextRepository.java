package com.acuver.autwit.adapter.mongo.scenario;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoScenarioContextRepository
        extends MongoRepository<MongoScenarioContextEntity, String> {
}