package com.acuver.autwit.adapter.mongo;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
public class MongoEventContextAdapter implements EventContextPort {

    private final MongoEventContextRepository repo;

    // ----------------------------------------------------------------------
    // SAVE
    // ----------------------------------------------------------------------
    @Override
    public void save(EventContextEntities ctx) {
        repo.save(toEntity(ctx));
    }

    // ----------------------------------------------------------------------
    // FINDERS
    // ----------------------------------------------------------------------
    @Override
    public Optional<EventContextEntities> findLatest(String orderId, String eventType) {
        return repo.findTopByOrderIdAndEventTypeOrderByEventTimestampDesc(orderId, eventType)
                .map(this::toDomain);
    }

    @Override
    public Optional<EventContextEntities> findByCanonicalKey(String key) {
        return repo.findById(key).map(this::toDomain);
    }

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

    // ----------------------------------------------------------------------
    // MUTATORS
    // ----------------------------------------------------------------------
    @Override
    public void markPaused(EventContextEntities ctx) {
        String key = ctx.getCanonicalKey();

        repo.findById(key).ifPresentOrElse(existing -> {
            // Update only pause-related fields
            MongoEventContextEntity updated = existing.toBuilder()
                    .paused(true)
                    .firstPausedAt(existing.getFirstPausedAt() == 0L
                            ? System.currentTimeMillis()
                            : existing.getFirstPausedAt())
                    .lastRetryAt(System.currentTimeMillis())
                    .build();

            repo.save(updated);

        }, () -> {
            // New entry if no previous record exists
            MongoEventContextEntity created = toEntity(ctx).toBuilder()
                    .paused(true)
                    .firstPausedAt(System.currentTimeMillis())
                    .lastRetryAt(System.currentTimeMillis())
                    .build();

            repo.save(created);
        });
    }

    @Override
    public void markResumeReady(String canonicalKey) {
        repo.findById(canonicalKey).ifPresent(e -> {
            MongoEventContextEntity updated = e.toBuilder()
                    .resumeReady(true)
                    .build();
            repo.save(updated);
        });
    }

    @Override
    public boolean isResumeReady(String canonicalKey) {
        return repo.findById(canonicalKey)
                .map(MongoEventContextEntity::isResumeReady)
                .orElse(false);
    }

    // ----------------------------------------------------------------------
    // MAPPERS — Domain ↔ Entity (now using builder)
    // ----------------------------------------------------------------------
    private MongoEventContextEntity toEntity(EventContextEntities ctx) {
        return MongoEventContextEntity.builder()
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

    private EventContextEntities toDomain(MongoEventContextEntity e) {
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
