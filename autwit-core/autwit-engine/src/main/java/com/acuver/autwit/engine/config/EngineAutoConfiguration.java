package com.acuver.autwit.engine.config;

import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.ports.EventReceiverPort;
import com.acuver.autwit.engine.bus.InMemoryEventBus;
import com.acuver.autwit.engine.resume.ResumeEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Auto-configuration to wire the engine:
 * - Exposes InMemoryEventBus as the EventReceiverPort (if none provided)
 * - Creates ResumeEngine and subscribes it to the bus after context refresh.
 */

@Configuration
public class EngineAutoConfiguration {
    private static final Logger log = LogManager.getLogger(EngineAutoConfiguration.class);
    /**
     * Default notifier if client does NOT provide their own implementation.
     */
    @Bean
    public EventReceiverPort eventNotifierPort(ObjectProvider<InMemoryEventBus> busProvider) {
        InMemoryEventBus bus = busProvider.getIfAvailable();
        if (bus == null) {
            bus = new InMemoryEventBus();
            log.info("Created default InMemoryEventBus as EventReceiverPort");
        }
        return bus;
    }

    /**
     * Create ResumeEngine if missing.
     */
    @Bean
    public ResumeEngine resumeEngine(EventContextPort storagePort) {
        log.info("Creating ResumeEngine");
        return new ResumeEngine(storagePort);
    }

    /**
     * After context is ready, auto-subscribe ResumeEngine to InMemoryEventBus.
     */
    @Bean
    public ApplicationListener<ContextRefreshedEvent> engineBusSubscriber(
            ObjectProvider<EventReceiverPort> notifierProvider,
            ObjectProvider<ResumeEngine> engineProvider) {

        return (ContextRefreshedEvent ev) -> {

            EventReceiverPort notifier = notifierProvider.getIfAvailable();
            ResumeEngine engine = engineProvider.getIfAvailable();

            if (notifier == null || engine == null) {
                log.warn("Engine or Notifier missing — skipping subscription.");
                return;
            }

            // Only subscribe if implementation is InMemoryEventBus
            if (notifier instanceof InMemoryEventBus bus) {
                bus.subscribe(engine);  // Consumer<EventContextEntities>
                log.info("ResumeEngine subscribed to InMemoryEventBus");
            } else {
                log.info("EventReceiverPort is external implementation (Kafka/Custom). " +
                        "Skipping subscription because it does not support subscribe().");
            }
        };
    }
}


