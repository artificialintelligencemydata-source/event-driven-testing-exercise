package com.acuver.autwit.adapter.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MongoApiTemplateRepository  extends MongoRepository<MongoApiTemplateDocument, String> {

    Optional<MongoApiTemplateDocument> findByApiName(String apiName);
    List<MongoApiTemplateDocument> findByIsService(Boolean isService);
    List<MongoApiTemplateDocument> findByHttpMethod(String httpMethod);
    void deleteByApiName(String apiName);
    boolean existsByApiName(String apiName);
}