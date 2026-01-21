package com.acuver.autwit.engine.bus;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventReceiverPort;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Minimal EventBus implementation used for event fanout:
 * KafkaAdapter → EventBus → ResumeEngine (and others)
 */

/**
 * Default in-memory event bus.
 *
 * - Implements EventReceiverPort so adapters (Kafka/Postgres/Mongo/etc)
 *   can publish EventContextEntities into the engine.
 *
 * - Allows multiple subscribers (ResumeEngine, EventStepNotifier, logging hooks).
 *
 * - Thread-safe using CopyOnWriteArrayList.
 *
 * - Non-blocking: publishing events never blocks step execution.
 */
public class InMemoryEventBus implements EventReceiverPort {
    /** List of all subscribers. Each subscriber consumes EventContextEntities. */
    private final List<Consumer<EventContextEntities>> subscribers =
            new CopyOnWriteArrayList<>();
    /**
     * Subscribe a new listener to this bus.
     * Example subscribers:
     *   - ResumeEngine (engine logic)
     *   - EventStepNotifier (completes futures for test steps)
     */
    public void subscribe(Consumer<EventContextEntities> consumer) {
        if (consumer != null) {
            subscribers.add(consumer);
        }
    }

    public void unsubscribe(Consumer<EventContextEntities> consumer) {
        if (consumer != null) {
            subscribers.remove(consumer);
        }
    }

    /**
     * Publish an event to all subscribers.
     * This is called by adapters:
     *   - KafkaEventConsumer
     *   - MongoEventPoller
     *   - H2/Postgres adapters
     *
     * It is the main callback entry.
     */
    @Override
    public void receive(EventContextEntities eventRecord) {
        for (Consumer<EventContextEntities> sub : subscribers) {
            try {
                sub.accept(eventRecord);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
