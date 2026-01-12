package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.EventContext;

import java.util.List;
import java.util.Optional;

/**
 * EventContextPort - Storage abstraction for AUTWIT event contexts.
 *
 * ARCHITECTURAL AUTHORITY RULES:
 * - markResumeReady() may ONLY be called by ResumeEngine
 * - Pollers, adapters, and client code MUST NOT call markResumeReady()
 * - Violation of this rule creates parallel resume authority (FORBIDDEN)
 */
public interface EventContextPort {

    /**
     * Persist or update an EventContext.
     */
    void save(EventContext ctx);

    /**
     * Find the most recent event matching orderId + eventType.
     */
    Optional<EventContext> findLatest(String orderId, String eventType);

    /**
     * Find event or paused context by canonical key.
     */
    Optional<EventContext> findByCanonicalKey(String key);

    /**
     * Mark a scenario as paused (waiting for event).
     * Can be called by client code when throwing SkipException.
     */
    void markPaused(EventContext ctx);

    /**
     * Mark a paused scenario as ready for resume.
     *
     * ⚠️ RESTRICTED METHOD ⚠️
     *
     * AUTHORIZATION:
     * This method may ONLY be invoked by ResumeEngine.
     *
     * ENFORCEMENT:
     * Callers SHOULD be audited via static analysis or runtime checks.
     * Unauthorized calls violate AUTWIT's single-authority principle.
     *
     * WHY RESTRICTED:
     * ResumeEngine is the SOLE decision-maker for resume eligibility.
     * If multiple components can mark resumeReady, the system has
     * non-deterministic behavior and parallel authority paths.
     *
     * @param canonicalKey The scenario to mark ready
     * @throws IllegalStateException if caller is not ResumeEngine (optional enforcement)
     */
    void markResumeReady(String canonicalKey);

    /**
     * Check if a scenario is ready for resume.
     */
    boolean isResumeReady(String canonicalKey);

    /**
     * Find all events for a given order.
     */
    List<EventContext> findByOrderId(String orderId);

    /**
     * Find all paused scenarios (for polling).
     */
    List<EventContext> findPaused();
}