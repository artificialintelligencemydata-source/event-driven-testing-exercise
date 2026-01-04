package com.acuver.autwit.internal.stepDef;

import com.acuver.autwit.core.domain.EventContext;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.testng.SkipException;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReusableStepDefsImpl implements ReusableStepDefs {

    private static final Logger log = LogManager.getLogger(ReusableStepDefsImpl.class);

    private final EventContextPort eventStore;
    private final EventMatcherPort matcher;
    private final ScenarioStatePort scenarioState;

    // ============================================================
    // INTERNAL UTILITIES (DO NOT EXPOSE TO STEPDEFS)
    // ============================================================

    private boolean skipIfAlreadyPassed(String stepName) {

        String scenarioKey = ScenarioContext.get("uniqueScenarioKey");

        if (!scenarioState.isStepAlreadySuccessful(scenarioKey, stepName)) {
            return false;
        }

        log.info("⏭ Step already passed → {}", stepName);

        Map<String, String> prev = scenarioState.getStepData(scenarioKey, stepName);
        if (prev != null && prev.containsKey("orderId")) {
            String orderId = prev.get("orderId");
            ScenarioContext.set("orderId", orderId);
            ScenarioMDC.setOrderId(orderId);
        }

        return true;
    }

    private void createPlaceholderAndSkip(String orderId, String eventType) {

        String canonical = CanonicalKeyGenerator.forOrder(orderId, eventType);

        EventContext placeholder = EventContext.builder()
                .canonicalKey(canonical)
                .orderId(orderId)
                .eventType(eventType)
                .paused(true)
                .resumeReady(false)
                .retryCount(0)
                .firstPausedAt(System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .build();

        eventStore.markPaused(placeholder);

        throw new SkipException("⏸ Event pending → " + eventType + " for order " + orderId);
    }

    // ============================================================
    // PUBLIC STEP METHODS
    // ============================================================

    @Override
    public EventContext verifyEvent(String eventType) {

        String orderId = ScenarioContext.get("orderId");
        String scenarioKey = ScenarioContext.get("uniqueScenarioKey");
        String stepName = "verifyEvent:" + eventType;

        // 1️⃣ Skip if step already passed
        if (skipIfAlreadyPassed(stepName)) {
            return eventStore.findLatest(orderId, eventType).orElse(null);
        }

        // 2️⃣ DB lookup
        Optional<EventContext> fromDb = eventStore.findLatest(orderId, eventType);

        if (fromDb.isPresent()) {

            scenarioState.markStep(
                    scenarioKey,
                    stepName,
                    "success",
                    Map.of("orderId", orderId)
            );

            return fromDb.get();
        }

        // 3️⃣ Not found → create placeholder → skip
        createPlaceholderAndSkip(orderId, eventType);

        return null; // unreachable
    }

    @Override
    public void validateEvent(String eventType) {

        String orderId = ScenarioContext.get("orderId");
        String scenarioKey = ScenarioContext.get("uniqueScenarioKey");
        String stepName = "validateEvent:" + eventType;

        // 1️⃣ Skip if step already passed
        if (skipIfAlreadyPassed(stepName)) return;

        // 2️⃣ Must exist in DB, else skip
        EventContext evt = eventStore.findLatest(orderId, eventType)
                .orElseThrow(() -> new SkipException(
                        "Event " + eventType + " missing for order " + orderId
                ));

        // 3️⃣ Additional validation logic (payload, fields, etc.)
        if (!eventType.equals(evt.getEventType())) {
            throw new AssertionError("EventType mismatch! expected=" + eventType +
                    " actual=" + evt.getEventType());
        }

        if (evt.getKafkaPayload() == null || evt.getKafkaPayload().isBlank()) {
            throw new AssertionError("Kafka payload is empty for order " + orderId);
        }

        // 4️⃣ Mark validated
        scenarioState.markStep(
                scenarioKey,
                stepName,
                "success",
                Map.of("orderId", orderId)
        );
    }

    @Override
    public void markStepSuccess(String stepName, Map<String, String> data) {

        scenarioState.markStep(
                ScenarioContext.get("uniqueScenarioKey"),
                stepName,
                "success",
                data
        );
    }
}
