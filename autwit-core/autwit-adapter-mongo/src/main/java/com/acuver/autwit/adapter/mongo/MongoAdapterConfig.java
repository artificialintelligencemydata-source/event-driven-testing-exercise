package com.acuver.autwit.adapter.mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
@EnableMongoRepositories(basePackages = "com.acuver.autwit.adapter.mongo")
public class MongoAdapterConfig {
}