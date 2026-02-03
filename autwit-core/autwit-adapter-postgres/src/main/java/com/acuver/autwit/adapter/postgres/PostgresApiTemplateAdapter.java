package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.ApiTemplateEntities;
import com.acuver.autwit.core.ports.ApiTemplatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PostgreSQL implementation of ApiTemplatePort.
 *
 * <h2>ACTIVATION</h2>
 * Enabled when: autwit.adapter.database=postgres
 *
 * @author AUTWIT Framework
 * @version 2.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
public class PostgresApiTemplateAdapter implements ApiTemplatePort {

    private final PostgresApiTemplateRepository repository;

    @Override
    public Optional<ApiTemplateEntities> findByApiName(String apiName) {
        log.debug("Finding API template by name: {}", apiName);
        return repository.findByApiName(apiName)
                .map(this::toDomain);
    }

    @Override
    public List<ApiTemplateEntities> findAll() {
        log.debug("Finding all API templates");
        return repository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiTemplateEntities> findByIsService(Boolean isService) {
        log.debug("Finding templates by isService: {}", isService);
        return repository.findByIsService(isService).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ApiTemplateEntities> findByHttpMethod(String httpMethod) {
        log.debug("Finding templates by HTTP method: {}", httpMethod);
        return repository.findByHttpMethod(httpMethod).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ApiTemplateEntities save(ApiTemplateEntities template) {
        log.info("Saving API template: {}", template.getApiName());
        PostgresApiTemplateEntity entity = toEntity(template);
        PostgresApiTemplateEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
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

    // ==========================================================================
    // MAPPING METHODS
    // ==========================================================================

    private ApiTemplateEntities toDomain(PostgresApiTemplateEntity entity) {
        return ApiTemplateEntities.builder()
                .id(entity.getId())
                .apiName(entity.getApiName())
                .httpMethod(entity.getHttpMethod())
                .endpointTemplate(entity.getEndpointTemplate())
                .requestTemplate(entity.getRequestTemplate())
                .dataRepresentation(entity.getDataRepresentation())
                .isService(entity.getIsService())
                .serviceName(entity.getServiceName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PostgresApiTemplateEntity toEntity(ApiTemplateEntities domain) {
        PostgresApiTemplateEntity entity = new PostgresApiTemplateEntity();
        entity.setId(domain.getId());
        entity.setApiName(domain.getApiName());
        entity.setHttpMethod(domain.getHttpMethod());
        entity.setEndpointTemplate(domain.getEndpointTemplate());
        entity.setRequestTemplate(domain.getRequestTemplate());
        entity.setDataRepresentation(domain.getDataRepresentation());
        entity.setIsService(domain.getIsService());
        entity.setServiceName(domain.getServiceName());
        entity.setDescription(domain.getDescription());
        return entity;
    }
}