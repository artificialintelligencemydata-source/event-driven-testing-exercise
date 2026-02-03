package com.acuver.autwit.adapter.postgres;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
public class PostgresEventContextAdapter implements EventContextPort {

    private final PostgresEventContextRepository repo;

    // ------------------------------------------------------------
    // SAVE
    // ------------------------------------------------------------
    @Override
    public void save(EventContextEntities ctx) {
        repo.save(toEntity(ctx));
    }

    // ------------------------------------------------------------
    // FIND
    // ------------------------------------------------------------
    @Override
    public Optional<EventContextEntities> findLatest(String orderId, String eventType) {
        return repo.findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(orderId, eventType)
                .map(this::toDomain);
    }

    @Override
    public Optional<EventContextEntities> findByCanonicalKey(String key) {
        return repo.findByCanonicalKey(key).map(this::toDomain);
    }

    // ------------------------------------------------------------
    // UPDATE FLAGS
    // ------------------------------------------------------------
    @Override
    public void markPaused(EventContextEntities ctx) {
        repo.findByCanonicalKey(ctx.getCanonicalKey()).ifPresentOrElse(existing -> {

            PostgresEventContextEntity updated = existing.toBuilder()
                    .paused(true)
                    .firstPausedAt(existing.getFirstPausedAt() == 0
                            ? System.currentTimeMillis()
                            : existing.getFirstPausedAt())
                    .lastRetryAt(System.currentTimeMillis())
                    .build();

            repo.save(updated);

        }, () -> {
            // Create fresh paused entry
            PostgresEventContextEntity newEnt = toEntity(ctx).toBuilder()
                    .paused(true)
                    .firstPausedAt(System.currentTimeMillis())
                    .lastRetryAt(System.currentTimeMillis())
                    .build();

            repo.save(newEnt);
        });
    }

    @Override
    public void markResumeReady(String canonicalKey) {
        repo.findByCanonicalKey(canonicalKey).ifPresent(ent -> {
            repo.save(ent.toBuilder().resumeReady(true).build());
        });
    }

    @Override
    public boolean isResumeReady(String canonicalKey) {
        return repo.findByCanonicalKey(canonicalKey)
                .map(PostgresEventContextEntity::isResumeReady)
                .orElse(false);
    }

    // ------------------------------------------------------------
    // FIND MULTIPLE
    // ------------------------------------------------------------
    @Override
    public List<EventContextEntities> findByOrderId(String orderId) {
        return repo.findByOrderId(orderId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<EventContextEntities> findPaused() {
        return repo.findByPausedTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    // ------------------------------------------------------------
    // MAPPING
    // ------------------------------------------------------------
    private PostgresEventContextEntity toEntity(EventContextEntities ctx) {
        return PostgresEventContextEntity.builder()
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
                .createdAt(ctx.getCreatedAt())
                .build();
    }

    private EventContextEntities toDomain(PostgresEventContextEntity e) {
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
                .createdAt(e.getCreatedAt())
                .build();
    }
}
