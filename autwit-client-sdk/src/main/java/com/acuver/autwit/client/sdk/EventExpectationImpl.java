package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.testng.SkipException;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Event Expectation Implementation.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@RequiredArgsConstructor
class EventExpectationImpl implements Autwit.EventExpectation {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final EventMatcherPort matcher;
    private final String orderId;
    private final String eventType;

    private Function<Map<String, Object>, Boolean> payloadValidator;
    private String expectedFieldName;
    private String expectedFieldValue;
    private Predicate<String> fieldPredicate;

    @Override
    public void assertSatisfied() {
       // log.debug("Asserting event: {} for orderId={}", eventType, orderId);

        try {
            EventContextEntities ctx = matcher.match(orderId, eventType).getNow(null);

            if (ctx == null) {
               // log.info("Event not yet available: {} for orderId={} - pausing scenario", eventType, orderId);
                throw new SkipException("Event not yet available: " + eventType + " for orderId=" + orderId);
            }

            // Apply payload validation if provided
            if (payloadValidator != null) {
                Map<String, Object> payload = parsePayload(ctx.getKafkaPayload());
                if (payload == null || !payloadValidator.apply(payload)) {
                    throw new RuntimeException("Payload validation failed for event: " + eventType);
                }
            }

            // Apply field validation if provided
            if (expectedFieldName != null) {
                Map<String, Object> payload = parsePayload(ctx.getKafkaPayload());
                if (payload == null) {
                    throw new RuntimeException("Payload is empty for field validation");
                }

                Object actualValue = getNestedValue(payload, expectedFieldName);
                String actualStr = actualValue != null ? actualValue.toString() : null;

                if (fieldPredicate != null) {
                    if (!fieldPredicate.test(actualStr)) {
                        throw new RuntimeException("Field validation failed: " + expectedFieldName);
                    }
                } else if (expectedFieldValue != null) {
                    if (!expectedFieldValue.equals(actualStr)) {
                        throw new RuntimeException("Field mismatch: " + expectedFieldName +
                                " expected=" + expectedFieldValue + ", actual=" + actualStr);
                    }
                }
            }

            //log.info("Event assertion satisfied: {} for orderId={}", eventType, orderId);

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

    @Override
    public Autwit.EventExpectation withPayloadField(String fieldName, String expectedValue) {
        this.expectedFieldName = fieldName;
        this.expectedFieldValue = expectedValue;
        this.fieldPredicate = null;
        return this;
    }

    @Override
    public Autwit.EventExpectation withPayloadField(String fieldName, Predicate<String> validator) {
        this.expectedFieldName = fieldName;
        this.fieldPredicate = validator;
        this.expectedFieldValue = null;
        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse payload as JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null || path == null) return null;
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}