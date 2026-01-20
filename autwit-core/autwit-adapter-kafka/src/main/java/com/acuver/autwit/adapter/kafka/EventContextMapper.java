package com.acuver.autwit.adapter.kafka;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * EventContextMapper - Converts Kafka JSON payload to EventContext domain object.
 *
 * <h2>RESPONSIBILITY</h2>
 * <p>Transform raw Kafka messages into AUTWIT's domain model. This is a pure
 * mapping component with no business logic.</p>
 *
 * <h2>V2 CANONICAL KEY SUPPORT</h2>
 * <p>This mapper supports both V1 and V2 canonical key formats:</p>
 * <ul>
 *   <li><b>V2 (preferred):</b> scenarioName::orderId::eventType - used when scenarioName is present in payload</li>
 *   <li><b>V1 (fallback):</b> orderId::eventType - used when scenarioName is absent</li>
 * </ul>
 *
 * <h2>EXPECTED KAFKA PAYLOAD FORMAT</h2>
 * <pre>
 * {
 *   "orderId": "ORD-123",
 *   "eventType": "ORDER_SHIPPED",
 *   "eventTimestamp": 1705012345678,
 *   "scenarioName": "Verify_Order_Ships"  // Optional - enables V2 key
 * }
 * </pre>
 *
 * <h2>ARCHITECTURAL NOTE</h2>
 * <p>This mapper is part of the Kafka adapter layer. It translates external
 * data formats into internal domain objects, following hexagonal architecture.</p>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
public class EventContextMapper {

    private static final Logger log = LogManager.getLogger(EventContextMapper.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert Kafka JSON payload to EventContext domain object.
     *
     * <p>Extracts required fields from JSON and generates appropriate canonical key.
     * If scenarioName is present in the payload, V2 key format is used. Otherwise,
     * falls back to V1 format.</p>
     *
     * @param json Raw JSON string from Kafka
     * @return EventContext domain object
     * @throws RuntimeException if mapping fails
     */
    public EventContext fromJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            // Extract required fields
            String orderId = readString(node, "orderId");
            String eventType = readString(node, "eventType");
            long eventTimestamp = readLong(node, "eventTimestamp", System.currentTimeMillis());

            // Extract optional scenario name for V2 key format
            String scenarioName = readString(node, "scenarioName");

            // Validate required fields
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("orderId is required in Kafka payload");
            }
            if (eventType == null || eventType.isBlank()) {
                throw new IllegalArgumentException("eventType is required in Kafka payload");
            }

            // ═══════════════════════════════════════════════════════════════
            // V2 CANONICAL KEY GENERATION
            // ═══════════════════════════════════════════════════════════════
            // If scenarioName is present in the Kafka payload, use V2 format.
            // This enables scenario-specific event matching and prevents
            // cross-scenario collisions.
            //
            // If scenarioName is absent, fall back to V1 format for backward
            // compatibility with older event producers.
            // ═══════════════════════════════════════════════════════════════

            String canonicalKey;
            if (scenarioName != null && !scenarioName.isBlank()) {
                // V2 format: scenarioName::orderId::eventType
                canonicalKey = CanonicalKeyGenerator.generate(scenarioName, orderId, eventType);
                log.debug("EventContextMapper: Using V2 canonical key - scenarioName={}, key={}",
                        scenarioName, canonicalKey);
            } else {
                // V1 format (fallback): orderId::eventType
                canonicalKey = CanonicalKeyGenerator.forOrder(orderId, eventType);
                log.debug("EventContextMapper: Using V1 canonical key (scenarioName absent) - key={}",
                        canonicalKey);
            }

            // Build EventContext domain object
            EventContext eventContext = EventContext.builder()
                    .orderId(orderId)
                    .eventType(eventType)
                    .eventTimestamp(eventTimestamp)
                    .canonicalKey(canonicalKey)
                    .kafkaPayload(json)

                    // Resume engine defaults - these are system events, not paused tests
                    .paused(false)
                    .resumeReady(false)
                    .retryCount(0)
                    .firstPausedAt(0L)
                    .lastRetryAt(0L)

                    // Metadata
                    .status("RECEIVED")
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.debug("EventContextMapper: Successfully mapped Kafka payload to EventContext - key={}",
                    canonicalKey);

            return eventContext;

        } catch (IllegalArgumentException e) {
            log.error("EventContextMapper: Validation error - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("EventContextMapper: Failed to map Kafka JSON to EventContext: {}",
                    e.getMessage(), e);
            throw new RuntimeException("EventContext mapping failed", e);
        }
    }

    /**
     * Convert EventContext to JSON string.
     *
     * <p>Used when publishing events or for logging/debugging.</p>
     *
     * @param ctx EventContext to serialize
     * @return JSON string representation
     */
    public String toJson(EventContext ctx) {
        try {
            return objectMapper.writeValueAsString(ctx);
        } catch (Exception e) {
            log.error("EventContextMapper: Failed to serialize EventContext to JSON: {}",
                    e.getMessage(), e);
            throw new RuntimeException("EventContext serialization failed", e);
        }
    }

    /**
     * Extract scenario name from canonical key (if V2 format).
     *
     * <p>Useful for logging and debugging when only the key is available.</p>
     *
     * @param canonicalKey The canonical key to parse
     * @return Scenario name or null if V1 format
     */
    public String extractScenarioName(String canonicalKey) {
        if (canonicalKey == null) {
            return null;
        }

        if (CanonicalKeyGenerator.isV2Format(canonicalKey)) {
            try {
                CanonicalKeyGenerator.KeyComponents components = CanonicalKeyGenerator.parse(canonicalKey);
                return components.scenarioName();
            } catch (Exception e) {
                log.trace("Could not extract scenarioName from key {}: {}", canonicalKey, e.getMessage());
            }
        }

        return null;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Read string value from JSON node.
     */
    private String readString(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    /**
     * Read long value from JSON node with default.
     */
    private long readLong(JsonNode node, String field, long defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asLong();
        }
        return defaultValue;
    }

    /**
     * Read integer value from JSON node with default.
     */
    private int readInt(JsonNode node, String field, int defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asInt();
        }
        return defaultValue;
    }

    /**
     * Read boolean value from JSON node with default.
     */
    private boolean readBoolean(JsonNode node, String field, boolean defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asBoolean();
        }
        return defaultValue;
    }
}