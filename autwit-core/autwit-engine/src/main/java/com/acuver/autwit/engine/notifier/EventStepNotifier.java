package com.acuver.autwit.engine.notifier;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * EventStepNotifier - Internal engine implementation of EventMatcherPort.
 *
 * <h2>RESPONSIBILITY</h2>
 * <ul>
 *   <li>Register test step interest in specific events</li>
 *   <li>Complete futures when matching events arrive</li>
 *   <li>Manage waiter registry for pending expectations</li>
 * </ul>
 *
 * <h2>V2 CANONICAL KEY FORMAT</h2>
 * <p>This implementation uses the V2 canonical key format:</p>
 * <pre>
 * scenarioName::orderId::eventType
 * </pre>
 * <p>This prevents cross-scenario collisions when multiple scenarios
 * test the same orderId with the same eventType.</p>
 *
 * <h2>NON-BLOCKING DESIGN</h2>
 * <p>This notifier is completely non-blocking. The match() method returns
 * immediately with a CompletableFuture. Callers should use getNow() for
 * immediate checks, not get() which would block.</p>
 *
 * <h2>ARCHITECTURAL NOTE</h2>
 * <p>Testkit and SDK should depend only on EventMatcherPort interface,
 * not on this implementation class directly.</p>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
public class EventStepNotifier implements EventMatcherPort {

    private static final Logger log = LogManager.getLogger(EventStepNotifier.class);

    /**
     * Registry of waiting futures, keyed by canonical key.
     * Multiple futures can wait for the same key.
     */
    private final ConcurrentMap<String, List<CompletableFuture<EventContextEntities>>> waiters =
            new ConcurrentHashMap<>();

    /**
     * Scheduler for TTL cleanup of orphaned waiters.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "EventStepNotifier-Cleanup");
                t.setDaemon(true);
                return t;
            });

    /**
     * Storage port for database lookups.
     */
    private final EventContextPort storage;

    /**
     * Runtime context port for accessing scenario information.
     */
    private final RuntimeContextPort runtimeContext;

    /**
     * TTL for orphaned waiters (default: 1 hour).
     */
    private final Duration waiterTtl;

    /**
     * Constructor with required dependencies.
     *
     * @param storage EventContextPort for database access
     * @param runtimeContext RuntimeContextPort for scenario context
     */
    public EventStepNotifier(EventContextPort storage, RuntimeContextPort runtimeContext) {
        this.storage = storage;
        this.runtimeContext = runtimeContext;
        this.waiterTtl = Duration.ofHours(1);
        log.info("EventStepNotifier initialized with V2 canonical key format");
    }

    /**
     * Register interest in an event and return a future for the result.
     *
     * <p>This method is NON-BLOCKING. It returns immediately with a
     * CompletableFuture that will be completed when:</p>
     * <ul>
     *   <li>The event is found in the database (immediate completion)</li>
     *   <li>The event arrives later (via eventArrived())</li>
     *   <li>The TTL expires (exceptional completion)</li>
     * </ul>
     *
     * <h3>V2 Key Generation</h3>
     * <p>This method generates a V2 canonical key using the current scenario
     * name from RuntimeContext. If scenario name is not available, it falls
     * back to V1 format with a warning.</p>
     *
     * @param orderId The order ID to match
     * @param eventType The event type to match
     * @return CompletableFuture that completes with EventContextEntities when found
     */
    @Override
    public CompletableFuture<EventContextEntities> match(String orderId, String eventType) {
        // ═══════════════════════════════════════════════════════════════
        // V2 CANONICAL KEY GENERATION
        // ═══════════════════════════════════════════════════════════════
        // Use V2 format: scenarioName::orderId::eventType
        // This prevents cross-scenario collision when multiple scenarios
        // test the same orderId with the same eventType.
        // ═══════════════════════════════════════════════════════════════

        String scenarioName = getScenarioName();
        String key;

        if (scenarioName != null && !scenarioName.isBlank()) {
            // V2 format (preferred)
            key = CanonicalKeyGenerator.generate(scenarioName, orderId, eventType);
            log.debug("EventStepNotifier: Using V2 key format - key={}", key);
        } else {
            // V1 fallback (deprecated) - log warning
            key = CanonicalKeyGenerator.forOrder(orderId, eventType);
            log.warn("EventStepNotifier: Scenario name not available, using deprecated V1 key format - key={}. " +
                    "This may cause cross-scenario collisions.", key);
        }

        log.debug("EventStepNotifier: match() called - orderId={}, eventType={}, key={}",
                orderId, eventType, key);

        // Fast probe: Check if event already exists in DB
        try {
            Optional<EventContextEntities> found = storage.findByCanonicalKey(key);
            if (found.isPresent()) {
                log.debug("EventStepNotifier: Immediate DB match for key={}", key);
                return CompletableFuture.completedFuture(found.get());
            }

            // Also try V1 key for backward compatibility during migration
            if (scenarioName != null) {
                String v1Key = CanonicalKeyGenerator.forOrder(orderId, eventType);
                Optional<EventContextEntities> v1Found = storage.findByCanonicalKey(v1Key);
                if (v1Found.isPresent()) {
                    log.info("EventStepNotifier: Found event using V1 key (migration compatibility) - v1Key={}", v1Key);
                    return CompletableFuture.completedFuture(v1Found.get());
                }
            }
        } catch (Exception e) {
            log.warn("EventStepNotifier: DB probe failed for key={}: {}", key, e.getMessage());
        }

        // No immediate match - register waiter for future completion
        CompletableFuture<EventContextEntities> future = new CompletableFuture<>();

        waiters.compute(key, (k, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }
            list.add(future);
            return list;
        });

        log.debug("EventStepNotifier: Registered waiter for key={}, total waiters for key: {}",
                key, waiters.get(key).size());

        // Schedule TTL cleanup to prevent memory leaks
        ScheduledFuture<?> cleanupTask = scheduler.schedule(() -> {
            if (!future.isDone()) {
                log.warn("EventStepNotifier: Waiter TTL expired for key={}", key);
                future.completeExceptionally(
                        new TimeoutException("Event wait TTL expired for key=" + key));
                removeWaiter(key, future);
            }
        }, waiterTtl.toMillis(), TimeUnit.MILLISECONDS);

        // Cancel cleanup task when future completes
        future.whenComplete((result, error) -> cleanupTask.cancel(true));

        return future;
    }

    /**
     * Notify that an event has arrived.
     *
     * <p>Called by ResumeEngine or InMemoryEventBus when a matching event
     * is detected. Completes all waiting futures for the event's canonical key.</p>
     *
     * @param ctx The event context that arrived
     */
    @Override
    public void eventArrived(EventContextEntities ctx) {
        if (ctx == null) {
            log.warn("EventStepNotifier: eventArrived() called with null context");
            return;
        }

        String key = ctx.getCanonicalKey();
        log.debug("EventStepNotifier: eventArrived() - key={}, resumeReady={}",
                key, ctx.isResumeReady());

        completeWaiters(key, ctx);

        // Also try to complete waiters using V1 key (migration compatibility)
        if (CanonicalKeyGenerator.isV2Format(key)) {
            try {
                CanonicalKeyGenerator.KeyComponents components = CanonicalKeyGenerator.parse(key);
                String v1Key = CanonicalKeyGenerator.forOrder(
                        components.orderId(),
                        components.eventType()
                );
                if (!v1Key.equals(key)) {
                    completeWaiters(v1Key, ctx);
                }
            } catch (Exception e) {
                log.trace("EventStepNotifier: Could not derive V1 key from V2 key: {}", e.getMessage());
            }
        }
    }

    /**
     * Complete all waiters for a given key.
     */
    private void completeWaiters(String key, EventContextEntities ctx) {
        List<CompletableFuture<EventContextEntities>> waitingFutures = waiters.remove(key);

        if (waitingFutures == null || waitingFutures.isEmpty()) {
            log.trace("EventStepNotifier: No waiters found for key={}", key);
            return;
        }

        log.info("EventStepNotifier: Completing {} waiter(s) for key={}", waitingFutures.size(), key);

        for (CompletableFuture<EventContextEntities> future : waitingFutures) {
            try {
                future.complete(ctx);
            } catch (Exception e) {
                log.warn("EventStepNotifier: Error completing future for key={}: {}", key, e.getMessage());
            }
        }
    }

    /**
     * Remove a specific waiter from the registry.
     */
    private void removeWaiter(String key, CompletableFuture<EventContextEntities> future) {
        waiters.computeIfPresent(key, (k, list) -> {
            list.remove(future);
            return list.isEmpty() ? null : list;
        });
    }

    /**
     * Get the current scenario name from runtime context.
     *
     * @return Scenario name or null if not available
     */
    private String getScenarioName() {
        try {
            return runtimeContext.get("scenarioName");
        } catch (Exception e) {
            log.trace("EventStepNotifier: Could not get scenarioName from context: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get count of active waiters (for monitoring).
     *
     * @return Total number of waiting futures
     */
    public int getActiveWaiterCount() {
        return waiters.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Get count of unique keys being waited on (for monitoring).
     *
     * @return Number of unique canonical keys with waiters
     */
    public int getUniqueKeyCount() {
        return waiters.size();
    }

    /**
     * Clear all waiters (for testing or cleanup).
     */
    public void clearAll() {
        int count = getActiveWaiterCount();
        waiters.clear();
        log.info("EventStepNotifier: Cleared {} waiter(s)", count);
    }

    /**
     * Shutdown the cleanup scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("EventStepNotifier: Scheduler shutdown complete");
    }
}