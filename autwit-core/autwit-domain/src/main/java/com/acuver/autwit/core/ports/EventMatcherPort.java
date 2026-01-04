package com.acuver.autwit.core.ports;

import com.acuver.autwit.core.domain.EventContext;

import java.util.concurrent.CompletableFuture;

public interface EventMatcherPort {
    /**
     * Test step registers interest in a particular event.
     * No waiting here — it returns a future (non-blocking).
     */
    CompletableFuture<EventContext> match(String orderId, String eventType);
    /**
     * Called internally when a matching event arrives.
     */
    void eventArrived(EventContext ctx);
}
