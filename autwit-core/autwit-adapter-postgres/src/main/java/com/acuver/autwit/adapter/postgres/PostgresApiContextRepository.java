package com.acuver.autwit.adapter.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ApiContextEntity.
 *
 * <h2>VISIBILITY</h2>
 * Package-private - internal to the PostgreSQL adapter.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */

@Repository
public interface PostgresApiContextRepository extends JpaRepository<PostgresApiContextEntity, Long> {
    Optional<PostgresApiContextEntity> findByApiName(String apiName);

    List<PostgresApiContextEntity> findByIsServiceTrue();

    List<PostgresApiContextEntity> findByServiceName(String serviceName);

    List<PostgresApiContextEntity> findByHttpMethod(String httpMethod);

    List<PostgresApiContextEntity> findByDataRepresentation(String dataRepresentation);

    boolean existsByApiName(String apiName);
}
