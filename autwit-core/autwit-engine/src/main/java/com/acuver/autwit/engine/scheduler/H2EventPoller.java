package com.acuver.autwit.engine.scheduler;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.EventContextPort;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * H2 Poller for paused EventContext entries.
 * Enabled only when autwit.database=h2
 */
@Component
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
public class H2EventPoller {

    private static final Logger LOG = LogManager.getLogger(H2EventPoller.class);

    private final EventContextPort storage;
    private final EventMatcherPort awaiter;

    public H2EventPoller(EventContextPort storage, EventMatcherPort awaiter) {
        this.storage = storage;
        this.awaiter = awaiter;
    }

    @Scheduled(fixedDelayString = "${autwit.poller.delay-ms:1000}")
    public void poll() {

        List<EventContext> paused = storage.findPaused();
        if (paused.isEmpty()) return;

        LOG.debug("H2Poller: {} paused contexts", paused.size());

        for (EventContext ctx : paused) {
            try { processPaused(ctx); }
            catch (Exception e) {
                LOG.error("H2Poller: error processing context {}: {}", ctx.getCanonicalKey(), e.getMessage());
            }
        }
    }

    private void processPaused(EventContext pausedCtx) {

        storage.findLatest(pausedCtx.getOrderId(), pausedCtx.getEventType())
                .ifPresent(latest -> {
                    LOG.info("H2Poller: Match found for {}", pausedCtx.getCanonicalKey());
                    storage.markResumeReady(pausedCtx.getCanonicalKey());
                    awaiter.eventArrived(latest);
                });
    }
}
