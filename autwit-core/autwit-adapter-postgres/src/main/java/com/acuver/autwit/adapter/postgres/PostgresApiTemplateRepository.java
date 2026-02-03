package com.acuver.autwit.adapter.postgres;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for API Template.
 */
@Repository
public interface PostgresApiTemplateRepository extends JpaRepository<PostgresApiTemplateEntity, Long> {

    Optional<PostgresApiTemplateEntity> findByApiName(String apiName);

    List<PostgresApiTemplateEntity> findByIsService(Boolean isService);

    List<PostgresApiTemplateEntity> findByHttpMethod(String httpMethod);

    void deleteByApiName(String apiName);

    boolean existsByApiName(String apiName);
}