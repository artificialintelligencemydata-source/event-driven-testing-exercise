package com.acuver.autwit.engine.notifier;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;


/**
 * Internal engine implementation of EventMatcherPort.
 * Testkit & SDK should depend only on the port, not this class.
 */
@Component
public class EventStepNotifier implements EventMatcherPort {

    private static final Logger log = LogManager.getLogger(EventStepNotifier.class);

    private final ConcurrentMap<String, List<CompletableFuture<EventContext>>> waiters =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "EventStepNotifier");
                t.setDaemon(true);
                return t;
            });

    private final EventContextPort storage;

    public EventStepNotifier(EventContextPort storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<EventContext> match(String orderId, String eventType) {
        String key = CanonicalKeyGenerator.forOrder(orderId, eventType);
        log.debug("Await request for key={}", key);

        // Fast probe in DB
        try {
            Optional<EventContext> found = storage.findByCanonicalKey(key);
            if (found.isPresent()) {
                log.debug("Immediate DB match for key={}", key);
                return CompletableFuture.completedFuture(found.get());
            }
        } catch (Exception e) {
            log.warn("DB probe failed: {}", e.getMessage());
        }

        CompletableFuture<EventContext> future = new CompletableFuture<>();

        waiters.compute(key, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(future);
            return list;
        });

        // Cleanup TTL (1 hr safety)
        ScheduledFuture<?> cleaner = scheduler.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException("Await TTL expired for key=" + key));
                removeWaiter(key, future);
            }
        }, Duration.ofHours(1).toMillis(), TimeUnit.MILLISECONDS);

        future.whenComplete((r, t) -> cleaner.cancel(true));

        return future;
    }

    @Override
    public void eventArrived(EventContext ctx) {
        if (ctx == null) return;

        String key = ctx.getCanonicalKey();
        log.debug("notifyEvent for key={} resumeReady={}", key, ctx.isResumeReady());

        complete(key, ctx);
    }

    private void complete(String key, EventContext ctx) {
        List<CompletableFuture<EventContext>> list = waiters.remove(key);
        if (list == null || list.isEmpty()) {
            log.debug("No waiters for key={}", key);
            return;
        }

        log.info("Completing {} waiter(s) for key={}", list.size(), key);
        for (CompletableFuture<EventContext> f : list) {
            f.complete(ctx);
        }
    }

    private void removeWaiter(String key, CompletableFuture<EventContext> future) {
        waiters.computeIfPresent(key, (k, list) -> {
            list.remove(future);
            return list.isEmpty() ? null : list;
        });
    }
}

