package com.acuver.autwit.engine.resume;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.engine.config.EngineAutoConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * ResumeEngine listens to EventContext events emitted by EventReceiverPort (EventBus).
 *
 * Responsibilities:
 *  1. Persist event into storage (event_records / test_context table)
 *  2. Lookup existing paused context using canonicalKey
 *  3. If match found → mark resumeReady=true
 *  4. Test runner (TestNG/Cucumber) will automatically rerun resumeReady tests
 */

public class ResumeEngine implements Consumer<EventContext> {
    private static final Logger log = LogManager.getLogger(ResumeEngine.class);
    private final EventContextPort storagePort;

    public ResumeEngine(EventContextPort storagePort) {
        this.storagePort = storagePort;
    }

    /**
     * Entry point called by EventBus.publish(eventContext)
     */
    @Override
    public void accept(EventContext event) {
        if (event == null) return;
        onEvent(event);
    }

    /**
     * Core resume-engine logic.
     */
    private void onEvent(EventContext event) {

        final String canonicalKey = event.getCanonicalKey();

        log.debug("ResumeEngine: Received event → {}", canonicalKey);

        // 1️⃣ Persist incoming event (Kafka/Mongo/H2/Postgres)
        storagePort.save(event);

        // 2️⃣ Check if a paused test exists for this canonicalKey
        Optional<EventContext> existing = storagePort.findByCanonicalKey(canonicalKey);

        if (existing.isEmpty()) {
            log.debug("ResumeEngine: No paused test for key {}", canonicalKey);
            return;
        }

        EventContext paused = existing.get();

        // 3️⃣ Determine resume condition
        if (!shouldResume(paused, event)) {
            log.debug("ResumeEngine: Resume condition not satisfied for {}", canonicalKey);
            return;
        }

        // 4️⃣ Mark resumeReady=true
        paused.setResumeReady(true);
        paused.setPaused(false);

        storagePort.save(paused);

        log.info("⚡ ResumeEngine: Marked resumeReady for {}", canonicalKey);
    }

    /**
     * Resume rules:
     *  Expand later to include eventType matching, stage validation, etc.
     */
    private boolean shouldResume(EventContext paused, EventContext event) {
        // Base rule: canonicalKey match
        if (!paused.getCanonicalKey().equals(event.getCanonicalKey())) {
            return false;
        }

        // Optional: eventType check
        if (paused.getEventType() != null &&
                !paused.getEventType().equals(event.getEventType())) {
            return false;
        }

        return true;
    }
}
