package com.acuver.autwit.adapter.mongo.scenario;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for MongoApiContextEntity.
 *
 * <h2>VISIBILITY</h2>
 * Package-private - internal to the MongoDB adapter.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Repository
@ConditionalOnProperty(name = "autwit.database.type", havingValue = "mongodb")
interface MongoApiContextRepository extends MongoRepository<MongoApiContextEntity, String> {

    Optional<MongoApiContextEntity> findByApiName(String apiName);

    List<MongoApiContextEntity> findByIsServiceTrue();

    List<MongoApiContextEntity> findByServiceName(String serviceName);

    List<MongoApiContextEntity> findByHttpMethod(String httpMethod);

    List<MongoApiContextEntity> findByDataRepresentation(String dataRepresentation);

    boolean existsByApiName(String apiName);
}