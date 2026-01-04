
package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.EventContext;

public interface EventReceiverPort {

    /**
     * Notify the engine that a new EventRecord has arrived.
     * Used by Kafka adapter and any other inbound event adapters.
     * Called by adapters when a new event arrives (Kafka, DB changes, etc.)
     */
    void receive(EventContext context);
}

