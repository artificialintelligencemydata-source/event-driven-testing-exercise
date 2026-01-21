package com.acuver.autwit.internal.stepDef;

import com.acuver.autwit.core.domain.EventContextEntities;
import java.util.Map;

public interface ReusableStepDefs {

    /**
     * Check if the expected event has arrived.
     * - If present in DB → return EventContextEntities
     * - If not present → create placeholder + skip scenario
     */
    EventContextEntities verifyEvent(String eventType);

    /**
     * Validate event payload or DB service AFTER verifyEvent().
     * - Ensures event exists
     * - Performs validation logic
     * - If missing → create placeholder + skip
     */
    void validateEvent(String eventType);

    /**
     * Mark a step as successfully executed in ScenarioContext.
     */
    void markStepSuccess(String stepName, Map<String, String> data);
}
