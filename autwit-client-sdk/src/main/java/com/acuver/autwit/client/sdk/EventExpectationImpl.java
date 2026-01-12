package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.testng.SkipException;

import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
class EventExpectationImpl implements Autwit.EventExpectation {

    private final EventMatcherPort matcher;
    private final String orderId;
    private final String eventType;
    private Function<Map<String, Object>, Boolean> payloadValidator;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void assertSatisfied() {
        try {
            EventContext ctx = matcher.match(orderId, eventType).getNow(null);
            if (ctx == null) {
                throw new SkipException("Event not yet available: " + eventType + " for orderId=" + orderId);
            }
            
            // Apply payload validation if provided
            if (payloadValidator != null) {
                Map<String, Object> payload = parsePayload(ctx.getKafkaPayload());
                if (payload == null || !payloadValidator.apply(payload)) {
                    throw new RuntimeException("Payload validation failed for event: " + eventType + " (orderId=" + orderId + ")");
                }
            }
        } catch (SkipException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Event verification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Autwit.EventExpectation assertPayload(Function<Map<String, Object>, Boolean> validator) {
        this.payloadValidator = validator;
        return this;
    }

    /**
     * Parse JSON payload to Map (read-only access).
     * Returns null if payload is null or empty.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse event payload as JSON: " + e.getMessage(), e);
        }
    }
}