package com.acuver.autwit.engine.resume;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ResumeEngine - SOLE AUTHORITY for marking scenarios resumeReady.
 *
 * ARCHITECTURAL RESPONSIBILITY:
 * This is the ONLY component in AUTWIT that may transition scenarios
 * from PAUSED to RESUME_READY service.
 *
 * AUTHORITY ENFORCEMENT:
 * - Pollers MUST notify ResumeEngine via accept()
 * - Adapters MUST NOT call storage.markResumeReady() directly
 * - Only ResumeEngine may invoke storage.markResumeReady()
 *
 * RESPONSIBILITIES:
 *  1. Receive event notifications from pollers or Kafka adapters
 *  2. Persist incoming events to storage
 *  3. Lookup paused scenarios matching the event's canonicalKey
 *  4. Evaluate resume conditions (matching, validation)
 *  5. Mark scenarios resumeReady=true if conditions satisfied
 *  6. Delegate to runner for actual scenario re-execution
 */
public class ResumeEngine implements Consumer<EventContextEntities> {

    private static final Logger log = LogManager.getLogger(ResumeEngine.class);
    private final EventContextPort storagePort;

    public ResumeEngine(EventContextPort storagePort) {
        this.storagePort = storagePort;
    }

    /**
     * Entry point for event notifications.
     *
     * Called by:
     * - Kafka adapters when events arrive from external systems
     * - DB pollers when they detect matching events in periodic scans
     * - Any component that detects an event arrival
     *
     * @param event The event that arrived
     */
    @Override
    public void accept(EventContextEntities event) {
        if (event == null) {
            log.warn("ResumeEngine: Received null event, ignoring");
            return;
        }
        onEvent(event);
    }

    /**
     * Core resume-engine logic.
     *
     * FLOW:
     * 1. Persist the incoming event
     * 2. Look up paused scenarios with matching canonicalKey
     * 3. Evaluate if resume conditions are satisfied
     * 4. Mark resumeReady=true (ONLY DONE HERE)
     * 5. Runner detects resumeReady and re-executes scenario
     */
    private void onEvent(EventContextEntities event) {

        final String canonicalKey = event.getCanonicalKey();

        log.debug("ResumeEngine: Processing event → {}", canonicalKey);

        // 1️⃣ Persist incoming event to DB
        // This ensures the event is available for immediate DB lookup
        // when the scenario resumes and calls matchOrPause()
        try {
            storagePort.save(event);
            log.debug("ResumeEngine: Event persisted → {}", canonicalKey);
        } catch (Exception e) {
            log.error("ResumeEngine: Failed to persist event {}: {}", canonicalKey, e.getMessage());
            // Continue anyway - maybe it's already persisted
        }

        // 2️⃣ Look up paused scenarios with matching canonicalKey
        // The storage should return the PAUSED SCENARIO CONTEXT,
        // not the event itself (though they share the same canonicalKey)
        List<EventContextEntities> pausedScenarios = findPausedScenarios(canonicalKey);

        if (pausedScenarios.isEmpty()) {
            log.debug("ResumeEngine: No paused scenarios for key {}", canonicalKey);
            return;
        }

        log.info("ResumeEngine: Found {} paused scenario(s) for key {}", pausedScenarios.size(), canonicalKey);

        // 3️⃣ Process each paused scenario
        for (EventContextEntities paused : pausedScenarios) {
            try {
                processResume(paused, event);
            } catch (Exception e) {
                log.error("ResumeEngine: Error processing paused scenario {}: {}",
                        paused.getCanonicalKey(), e.getMessage());
            }
        }
    }

    /**
     * Find paused scenarios matching the canonical key.
     *
     * Note: findByCanonicalKey() may return either:
     * - The paused scenario context (if it exists)
     * - The event record (if scenario hasn't paused yet)
     *
     * We need to filter for actually paused scenarios.
     */
    private List<EventContextEntities> findPausedScenarios(String canonicalKey) {
        try {
            Optional<EventContextEntities> found = storagePort.findByCanonicalKey(canonicalKey);

            if (found.isPresent() && found.get().isPaused()) {
                return List.of(found.get());
            }

            return List.of();

        } catch (Exception e) {
            log.error("ResumeEngine: DB lookup failed for {}: {}", canonicalKey, e.getMessage());
            return List.of();
        }
    }

    /**
     * Evaluate and execute resume for a single paused scenario.
     */
    private void processResume(EventContextEntities paused, EventContextEntities event) {

        // 3️⃣ Evaluate resume conditions
        if (!shouldResume(paused, event)) {
            log.debug("ResumeEngine: Resume condition not satisfied for {}", paused.getCanonicalKey());
            return;
        }

        // 4️⃣ Mark resumeReady=true
        // ⚠️ THIS IS THE ONLY PLACE IN AUTWIT WHERE THIS HAPPENS ⚠️
        try {
            storagePort.markResumeReady(paused.getCanonicalKey());
            log.info("⚡ ResumeEngine: Marked resumeReady for {}", paused.getCanonicalKey());

        } catch (Exception e) {
            log.error("ResumeEngine: Failed to mark resumeReady for {}: {}",
                    paused.getCanonicalKey(), e.getMessage());
        }
    }

    /**
     * Determine if a paused scenario should resume given an arrived event.
     *
     * CURRENT RULES:
     * - Canonical key must match (orderId + eventType)
     * - Event type must match (if specified)
     *
     * FUTURE ENHANCEMENTS:
     * - Stage validation (e.g., don't resume if in wrong workflow stage)
     * - Payload validation (e.g., ensure event has required fields)
     * - Business rules (e.g., order must be in certain status)
     * - Time-based rules (e.g., minimum pause duration)
     */
    private boolean shouldResume(EventContextEntities paused, EventContextEntities event) {

        // Rule 1: Canonical key must match
        if (!paused.getCanonicalKey().equals(event.getCanonicalKey())) {
            log.debug("ResumeEngine: Canonical key mismatch - paused: {}, event: {}",
                    paused.getCanonicalKey(), event.getCanonicalKey());
            return false;
        }

        // Rule 2: Event type must match (if specified in paused context)
        if (paused.getEventType() != null &&
                !paused.getEventType().equals(event.getEventType())) {
            log.debug("ResumeEngine: Event type mismatch - paused: {}, event: {}",
                    paused.getEventType(), event.getEventType());
            return false;
        }

        // All conditions satisfied
        return true;
    }
}