package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.EventContext;

import java.util.List;
import java.util.Optional;

public interface EventContextPort {
    void save(EventContext ctx);

    Optional<EventContext> findLatest(String orderId, String eventType);

    Optional<EventContext> findByCanonicalKey(String key);

    void markPaused(EventContext ctx);

    void markResumeReady(String canonicalKey);

    boolean isResumeReady(String canonicalKey);

    List<EventContext> findByOrderId(String orderId);
    List<EventContext> findPaused();
}
