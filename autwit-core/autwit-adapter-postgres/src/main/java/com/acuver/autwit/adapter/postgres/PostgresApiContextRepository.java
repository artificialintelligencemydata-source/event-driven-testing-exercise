package com.acuver.autwit.adapter.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
public interface PostgresApiContextRep extends JpaRepository<PostgresApiContextEntity, Long> {
}
