package com.acuver.autwit.adapter.mongo;
import com.acuver.autwit.core.domain.ApiTemplateEntities;
import com.acuver.autwit.core.ports.ApiTemplatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ApiTemplatePort.
 *
 * <h2>ACTIVATION</h2>
 * Enabled when: autwit.adapter.database=mongo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
public class MongoApiTemplateAdapter implements ApiTemplatePort {

    private final MongoApiTemplateRepository repository;

    @Override
    public Optional<ApiTemplateEntities> findByApiName(String apiName) {
        log.debug("Finding API template by name: {}", apiName);
        return repository.findByApiName(apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiTemplateEntities> findAll() {
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiTemplateEntities> findByIsService(Boolean isService) {
        return repository.findByIsService(isService).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiTemplateEntities> findByHttpMethod(String httpMethod) {
        return repository.findByHttpMethod(httpMethod).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public ApiTemplateEntities save(ApiTemplateEntities template) {
        log.info("Saving API template: {}", template.getApiName());
        MongoApiTemplateDocument document = toDocument(template);
        MongoApiTemplateDocument saved = repository.save(document);
        return toDomain(saved);
    }

    @Override
    public void deleteByApiName(String apiName) {
        log.info("Deleting API template: {}", apiName);
        repository.deleteByApiName(apiName);
    }

    @Override
    public boolean existsByApiName(String apiName) {
        return repository.existsByApiName(apiName);
    }

    @Override
    public long count() {
        return repository.count();
    }

    // Mapping methods
    private ApiTemplateEntities toDomain(MongoApiTemplateDocument doc) {
        return ApiTemplateEntities.builder()
                .id(doc.getId() != null ? Long.parseLong(doc.getId()) : null)
                .apiName(doc.getApiName())
                .httpMethod(doc.getHttpMethod())
                .endpointTemplate(doc.getEndpointTemplate())
                .requestTemplate(doc.getRequestTemplate())
                .dataRepresentation(doc.getDataRepresentation())
                .isService(doc.getIsService())
                .serviceName(doc.getServiceName())
                .description(doc.getDescription())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private MongoApiTemplateDocument toDocument(ApiTemplateEntities domain) {
        MongoApiTemplateDocument doc = new MongoApiTemplateDocument();
        doc.setId(domain.getId() != null ? String.valueOf(domain.getId()) : null);
        doc.setApiName(domain.getApiName());
        doc.setHttpMethod(domain.getHttpMethod());
        doc.setEndpointTemplate(domain.getEndpointTemplate());
        doc.setRequestTemplate(domain.getRequestTemplate());
        doc.setDataRepresentation(domain.getDataRepresentation());
        doc.setIsService(domain.getIsService());
        doc.setServiceName(domain.getServiceName());
        doc.setDescription(domain.getDescription());
        return doc;
    }
}