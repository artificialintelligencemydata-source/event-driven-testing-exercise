package com.acuver.autwit.adapter.kafka;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Converts Kafka JSON payload → EventContext domain object.
 */
@Component
public class EventContextMapper {
    private static final Logger log = LogManager.getLogger(EventContextMapper.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public EventContext fromJson(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            // Extract fields
            String orderId = read(node, "orderId");
            String eventType = read(node, "eventType");
            long eventTs = readLong(node, "eventTimestamp", System.currentTimeMillis());

            // Unified canonicalKey format (STRICT)
            String canonicalKey = CanonicalKeyGenerator.forOrder(orderId, eventType);

            // Build EventContext object
            return EventContext.builder()
                    .orderId(orderId)
                    .eventType(eventType)
                    .eventTimestamp(eventTs)
                    .canonicalKey(canonicalKey)
                    .kafkaPayload(json)

                    // Resume engine defaults
                    .paused(false)
                    .resumeReady(false)
                    .retryCount(0)
                    .firstPausedAt(0L)
                    .lastRetryAt(0L)
                    .timestamp(System.currentTimeMillis())
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to map Kafka JSON → EventContext: {}", e.getMessage(), e);
            throw new RuntimeException("EventContext mapping failed", e);
        }
    }

    // -------------------------------------------------------------
    // Helper extractors (clean readability)
    // -------------------------------------------------------------
    private String read(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }

    private long readLong(JsonNode node, String field, long defaultValue) {
        return node.has(field) ? node.get(field).asLong() : defaultValue;
    }
}

