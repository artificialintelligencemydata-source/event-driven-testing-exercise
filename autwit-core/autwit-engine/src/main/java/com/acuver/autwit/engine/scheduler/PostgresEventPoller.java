package com.acuver.autwit.engine.scheduler;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.engine.resume.ResumeEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * PostgresEventPoller - Polls PostgreSQL database for paused scenarios with matching events.
 *
 * <h2>ARCHITECTURAL RESPONSIBILITY: DETECTION ONLY</h2>
 * <p>This poller is responsible for ONE thing: detecting when a paused scenario
 * has a matching system event available in the database.</p>
 *
 * <h2>WHAT THIS POLLER DOES</h2>
 * <ul>
 *   <li>Periodically scans for paused test contexts (paused=true)</li>
 *   <li>For each paused context, checks if matching system event exists</li>
 *   <li>If match found, DELEGATES to ResumeEngine for decision</li>
 * </ul>
 *
 * <h2>WHAT THIS POLLER DOES NOT DO</h2>
 * <ul>
 *   <li>❌ Does NOT call markResumeReady() - that's ResumeEngine's job</li>
 *   <li>❌ Does NOT evaluate retry limits - that's ResumeEngine's job</li>
 *   <li>❌ Does NOT apply business rules - that's ResumeEngine's job</li>
 *   <li>❌ Does NOT trigger execution - that's ResumeScheduler's job</li>
 * </ul>
 *
 * <h2>ARCHITECTURAL COMPLIANCE</h2>
 * <p>This implementation follows the Single Resume Authority principle:
 * Only ResumeEngine may decide when a scenario is ready to resume.</p>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 * @see ResumeEngine
 */
@Component
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
public class PostgresEventPoller {

    private static final Logger log = LogManager.getLogger(PostgresEventPoller.class);

    private final EventContextPort storage;
    private final ResumeEngine resumeEngine;

    /**
     * Constructor with required dependencies.
     *
     * @param storage EventContextPort for database access
     * @param resumeEngine ResumeEngine for resume decisions (SOLE AUTHORITY)
     */
    public PostgresEventPoller(EventContextPort storage, ResumeEngine resumeEngine) {
        this.storage = storage;
        this.resumeEngine = resumeEngine;
        log.info("PostgresEventPoller initialized - delegating resume decisions to ResumeEngine");
    }

    /**
     * Scheduled polling task.
     *
     * <p>Runs at configured interval to detect matches between paused test
     * contexts and available system events.</p>
     *
     * <p>IMPORTANT: This method only DETECTS matches. It does NOT make
     * resume decisions - that responsibility belongs to ResumeEngine.</p>
     */
    @Scheduled(fixedDelayString = "${autwit.poller.delay-ms:1000}")
    public void poll() {
        log.trace("PostgresEventPoller: Starting poll cycle");

        // 1. Find all paused test contexts
        List<EventContextEntities> pausedContexts = storage.findPaused();

        if (pausedContexts.isEmpty()) {
            log.trace("PostgresEventPoller: No paused contexts found");
            return;
        }

        log.debug("PostgresEventPoller: Found {} paused context(s) to check", pausedContexts.size());

        // 2. For each paused context, check if matching event exists
        for (EventContextEntities pausedCtx : pausedContexts) {
            try {
                processPausedContext(pausedCtx);
            } catch (Exception e) {
                log.error("PostgresEventPoller: Error processing paused context {}: {}",
                        pausedCtx.getCanonicalKey(), e.getMessage(), e);
                // Continue with other contexts - don't let one failure stop all processing
            }
        }
    }

    /**
     * Process a single paused context.
     *
     * <p>Checks if a matching system event exists for this paused test context.
     * If found, delegates to ResumeEngine for resume decision.</p>
     *
     * <p>NOTE: This method does NOT call markResumeReady(). That decision
     * is made by ResumeEngine based on its evaluation rules.</p>
     *
     * @param pausedCtx The paused test context to check
     */
    private void processPausedContext(EventContextEntities pausedCtx) {
        String canonicalKey = pausedCtx.getCanonicalKey();
        String orderId = pausedCtx.getOrderId();
        String eventType = pausedCtx.getEventType();

        log.trace("PostgresEventPoller: Checking for match - key={}, orderId={}, eventType={}",
                canonicalKey, orderId, eventType);

        // Look for matching system event in database
        Optional<EventContextEntities> matchingEvent = storage.findLatest(orderId, eventType);

        if (matchingEvent.isPresent()) {
            EventContextEntities event = matchingEvent.get();

            log.info("PostgresEventPoller: Match found for paused context {} - delegating to ResumeEngine",
                    canonicalKey);

            // ═══════════════════════════════════════════════════════════════
            // CRITICAL: DELEGATE TO RESUME ENGINE
            // ═══════════════════════════════════════════════════════════════
            // We do NOT call markResumeReady() here.
            // We do NOT evaluate retry limits here.
            // We do NOT apply business rules here.
            //
            // We simply notify ResumeEngine that a matching event exists.
            // ResumeEngine will:
            //   1. Evaluate whether this scenario should resume
            //   2. Apply any retry/guard rules
            //   3. Call markResumeReady() if appropriate
            //
            // This maintains SINGLE RESUME AUTHORITY.
            // ═══════════════════════════════════════════════════════════════

            resumeEngine.accept(event);

        } else {
            log.trace("PostgresEventPoller: No matching event found for key={}", canonicalKey);
        }
    }

    /**
     * Manual trigger for immediate poll.
     *
     * <p>Can be called programmatically to force an immediate poll cycle
     * without waiting for the scheduled interval.</p>
     */
    public void triggerImmediatePoll() {
        log.info("PostgresEventPoller: Manual poll triggered");
        poll();
    }
}