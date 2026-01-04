package com.acuver.autwit.adapter.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MongoEventContextRepository extends MongoRepository<MongoEventContextEntity, String> {
    Optional<MongoEventContextEntity>
    findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(String orderId, String eventType);

    Optional<MongoEventContextEntity>
    findByCanonicalKey(String canonicalKey);

    boolean existsByCanonicalKeyAndResumeReadyTrue(String canonicalKey);

    List<MongoEventContextEntity> findByOrderId(String orderId);
    List<MongoEventContextEntity> findByPausedTrue();
}
