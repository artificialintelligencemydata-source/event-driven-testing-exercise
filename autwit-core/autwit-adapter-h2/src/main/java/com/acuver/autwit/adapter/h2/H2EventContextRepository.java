package com.acuver.autwit.adapter.h2;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface H2EventContextRepository extends JpaRepository<H2EventContextEntity, String> {

    Optional<H2EventContextEntity> findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(
            String orderId, String eventType);

    List<H2EventContextEntity> findByOrderId(String orderId);

    Optional<H2EventContextEntity> findByCanonicalKey(String canonicalKey);
    List<H2EventContextEntity> findByPausedTrue();
}