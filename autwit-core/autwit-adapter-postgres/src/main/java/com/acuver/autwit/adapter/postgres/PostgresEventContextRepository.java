package com.acuver.autwit.adapter.postgres;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostgresEventContextRepository
        extends JpaRepository<PostgresEventContextEntity, String> {

    Optional<PostgresEventContextEntity>
    findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(String orderId, String eventType);

    Optional<PostgresEventContextEntity> findByCanonicalKey(String canonicalKey);

    List<PostgresEventContextEntity> findByOrderId(String orderId);
    List<PostgresEventContextEntity> findByPausedTrue();

}