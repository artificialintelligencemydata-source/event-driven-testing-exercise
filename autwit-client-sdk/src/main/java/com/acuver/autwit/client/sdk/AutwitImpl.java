package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
public class AutwitImpl implements Autwit {
    private final EventMatcherPort matcher;
    private final ScenarioStatePort scenarioState;
    private final RuntimeContextPort contextAccess;
    @Override
    public Autwit.EventExpectation expectEvent(String orderId, String eventType) {
        return new EventExpectationImpl(matcher, orderId, eventType);
    }
    @Override
    public Autwit.ScenarioStepStatus step() {
        return new ScenarioStepStatusImpl(scenarioState, contextAccess);
    }
    @Override
    public Autwit.ContextAccessor context() {
        return new ContextAccessorImpl(contextAccess);
    }
}
