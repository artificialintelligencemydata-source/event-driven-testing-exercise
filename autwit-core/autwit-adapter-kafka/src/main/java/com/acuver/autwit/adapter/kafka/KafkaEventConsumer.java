package com.acuver.autwit.adapter.kafka;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventContextPort;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "autwit.adapter.kafka.enabled", havingValue = "true")
public class KafkaEventConsumer {
    private static final Logger LOG = LogManager.getLogger(KafkaEventConsumer.class);

    private final EventContextMapper mapper;
    private final EventContextPort storage;

    public KafkaEventConsumer(EventContextMapper mapper, EventContextPort storage) {
        this.mapper = mapper;
        this.storage = storage;
    }

    /**
     * Listen to configured topic (autwit.kafka.topicEvents) using kafkaListenerContainerFactory.
     * Manual ack: we only ack after storage.saveEvent succeeds.
     */
    /**
     * Listens on configured Kafka topic & persists EventContext.
     * Uses manual ack — only acknowledge after successful storage.
     */
    @KafkaListener(
            topics = "${autwit.kafka.topicEvents}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handle(ConsumerRecord<String, String> rec, Acknowledgment ack) {

        String payload = rec.value();
        String key = rec.key();

        LOG.debug("Kafka received key={} partition={} offset={}", key, rec.partition(), rec.offset());

        EventContext ctx;
        try {
            ctx = mapper.fromJson(payload);
        } catch (Exception e) {
            LOG.error("Failed to map Kafka message → EventContext. key={}  Error={}",
                    key, e.getMessage(), e);
            // Do NOT ack — either retry or your DLQ policy handles it.
            return;
        }

        try {
            storage.save(ctx);
            ack.acknowledge();

            LOG.info("✔ Kafka event persisted: canonicalKey={} orderId={} eventType={}",
                    ctx.getCanonicalKey(), ctx.getOrderId(), ctx.getEventType());

        } catch (Exception e) {
            LOG.error("❌ Failed to store EventContext canonicalKey={} — Not ACKing. Error={}",
                    ctx.getCanonicalKey(), e.getMessage(), e);
            // Message will be retried depending on Kafka consumer configuration.
        }
    }
}
