package com.acuver.autwit.adapter.h2;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
public class H2EventContextAdapter implements EventContextPort {

    private final H2EventContextRepository repo;

    // =====================================================================
    // SAVE
    // =====================================================================
    @Override
    public void save(EventContextEntities ctx) {
        repo.save(toEntity(ctx));
    }

    // =====================================================================
    // FIND LATEST EVENT BY ORDER + TYPE
    // =====================================================================
    @Override
    public Optional<EventContextEntities> findLatest(String orderId, String eventType) {
        return repo.findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(orderId, eventType)
                .map(this::toDomain);
    }

    // =====================================================================
    // FIND BY CANONICAL KEY
    // =====================================================================
    @Override
    public Optional<EventContextEntities> findByCanonicalKey(String key) {
        return repo.findByCanonicalKey(key).map(this::toDomain);
    }

    // =====================================================================
    // MARK PAUSED
    // =====================================================================
    @Override
    public void markPaused(EventContextEntities ctx) {

        repo.findByCanonicalKey(ctx.getCanonicalKey()).ifPresentOrElse(existing -> {

            // Update existing entity using toBuilder()
            H2EventContextEntity updated = existing.toBuilder()
                    .paused(true)
                    .firstPausedAt(existing.getFirstPausedAt() == 0L
                            ? System.currentTimeMillis()
                            : existing.getFirstPausedAt())
                    .lastRetryAt(System.currentTimeMillis())
                    .build();

            repo.save(updated);

        }, () -> {
            // Create new paused record
            H2EventContextEntity saved = H2EventContextEntity.builder()
                    .canonicalKey(ctx.getCanonicalKey())
                    .orderId(ctx.getOrderId())
                    .eventType(ctx.getEventType())
                    .eventTimestamp(ctx.getEventTimestamp())
                    .kafkaPayload(ctx.getKafkaPayload())
                    .paused(true)
                    .resumeReady(false)
                    .retryCount(1)
                    .firstPausedAt(System.currentTimeMillis())
                    .lastRetryAt(System.currentTimeMillis())
                    .status(ctx.getStatus())
                    .timestamp(System.currentTimeMillis())
                    .build();

            repo.save(saved);
        });
    }

    // =====================================================================
    // MARK RESUME READY
    // =====================================================================
    @Override
    public void markResumeReady(String canonicalKey) {
        repo.findByCanonicalKey(canonicalKey).ifPresent(existing -> {
            H2EventContextEntity updated = existing.toBuilder()
                    .resumeReady(true)
                    .build();
            repo.save(updated);
        });
    }

    // =====================================================================
    // IS RESUME READY?
    // =====================================================================
    @Override
    public boolean isResumeReady(String canonicalKey) {
        return repo.findByCanonicalKey(canonicalKey)
                .map(H2EventContextEntity::isResumeReady)
                .orElse(false);
    }

    // =====================================================================
    // FIND ALL EVENTS FOR ORDER
    // =====================================================================
    @Override
    public List<EventContextEntities> findByOrderId(String orderId) {
        return repo.findByOrderId(orderId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // =====================================================================
    // FIND ALL PAUSED EVENTS
    // =====================================================================
    @Override
    public List<EventContextEntities> findPaused() {
        return repo.findByPausedTrue()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // =====================================================================
    // MAPPING: DOMAIN → ENTITY
    // =====================================================================
    private H2EventContextEntity toEntity(EventContextEntities ctx) {
        return H2EventContextEntity.builder()
                .canonicalKey(ctx.getCanonicalKey())
                .orderId(ctx.getOrderId())
                .eventType(ctx.getEventType())
                .eventTimestamp(ctx.getEventTimestamp())
                .kafkaPayload(ctx.getKafkaPayload())

                .paused(ctx.isPaused())
                .resumeReady(ctx.isResumeReady())
                .retryCount(ctx.getRetryCount())
                .firstPausedAt(ctx.getFirstPausedAt())
                .lastRetryAt(ctx.getLastRetryAt())

                .status(ctx.getStatus())
                .timestamp(ctx.getTimestamp())
                .build();
    }

    // =====================================================================
    // MAPPING: ENTITY → DOMAIN
    // =====================================================================
    private EventContextEntities toDomain(H2EventContextEntity e) {
        return EventContextEntities.builder()
                .canonicalKey(e.getCanonicalKey())
                .orderId(e.getOrderId())
                .eventType(e.getEventType())
                .eventTimestamp(e.getEventTimestamp())
                .kafkaPayload(e.getKafkaPayload())

                .paused(e.isPaused())
                .resumeReady(e.isResumeReady())
                .retryCount(e.getRetryCount())
                .firstPausedAt(e.getFirstPausedAt())
                .lastRetryAt(e.getLastRetryAt())

                .status(e.getStatus())
                .timestamp(e.getTimestamp())
                .build();
    }
}
