package com.acuver.autwit.client.sdk.contextAccessor;

import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.client.sdk.contextAccessor.ContextAccessorImpl;
import com.acuver.autwit.client.sdk.contextAccessor.EventExpectationImpl;
import com.acuver.autwit.client.sdk.contextAccessor.ScenarioStepStatusImpl;
import com.acuver.autwit.core.domain.EventContextEntities;
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import com.acuver.autwit.core.utils.CanonicalKeyGenerator;
import lombok.RequiredArgsConstructor;

/**
 * AUTWIT Implementation - Main facade implementation.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@RequiredArgsConstructor
public class AutwitImpl implements Autwit {

    private final EventMatcherPort matcher;
    private final ScenarioStatePort scenarioState;
    private final RuntimeContextPort contextAccess;

    @Override
    public EventExpectation expectEvent(String orderId, String eventType) {
        return new EventExpectationImpl(matcher, orderId, eventType);
    }

    @Override
    public void pauseUntilEvent(String orderId, String eventType) {

    }

    @Override
    public ScenarioStepStatus step() {
        return new ScenarioStepStatusImpl(scenarioState, contextAccess);
    }

    @Override
    public ContextAccessor context() {
        return new ContextAccessorImpl(contextAccess);
    }

//    @Override
//    public void pauseUntilEvent(String orderId, String eventType) {
//
//        String scenarioName = contextAccess.get("scenarioName");
//
//        String canonicalKey =
//                CanonicalKeyGenerator.generate(
//                        scenarioName,
//                        orderId,
//                        eventType
//                );
//
//        EventContextEntities ctx =
//                EventContextEntities.builder()
//                        .canonicalKey(canonicalKey)
//                        .orderId(orderId)
//                        .eventType(eventType)
//                        .status("WAITING")
//                        .paused(true)
//                        .resumeReady(false)
//                        .retryCount(1)
//                        .firstPausedAt(System.currentTimeMillis())
//                        .createdAt(System.currentTimeMillis())
//                        .build();
//
//        // 🔒 Delegate to the ONLY authority
//        eventContextPort.markPaused(ctx);
//    }
}