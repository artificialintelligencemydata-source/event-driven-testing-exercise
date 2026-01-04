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
 * MongoDB Poller for paused EventContext entries.
 * Enabled only when autwit.database=mongo
 */
@Component
@ConditionalOnProperty(name = "autwit.database", havingValue = "mongo")
public class MongoEventPoller {

    private static final Logger LOG = LogManager.getLogger(MongoEventPoller.class);

    private final EventContextPort storage;
    private final EventMatcherPort awaiter;

    public MongoEventPoller(EventContextPort storage, EventMatcherPort awaiter) {
        this.storage = storage;
        this.awaiter = awaiter;
    }

    @Scheduled(fixedDelayString = "${autwit.poller.delay-ms:1000}")
    public void poll() {

        List<EventContext> paused = storage.findPaused();
        if (paused.isEmpty()) return;

        LOG.debug("MongoPoller: {} paused contexts", paused.size());

        for (EventContext ctx : paused) {
            try { processPaused(ctx); }
            catch (Exception e) {
                LOG.error("MongoPoller: error: {}", e.getMessage(), e);
            }
        }
    }

    private void processPaused(EventContext pausedCtx) {

        storage.findLatest(pausedCtx.getOrderId(), pausedCtx.getEventType())
                .ifPresent(latest -> {
                    LOG.info("MongoPoller: Match found for {}", pausedCtx.getCanonicalKey());
                    storage.markResumeReady(pausedCtx.getCanonicalKey());
                    awaiter.eventArrived(latest);
                });
    }
}

