package com.acuver.autwit.internal.context;

import org.apache.logging.log4j.ThreadContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized MDC manager for scenario-based logging.
 * All ThreadContext operations are handled here only.
 */
public final class ScenarioMDC {

    private ScenarioMDC() {}

    /** Generic setter */
    public static void set(String key, String value) {
        if (key == null) return;
        if (value == null) {
            ThreadContext.remove(key);
        } else {
            ThreadContext.put(key, value);
        }
    }

    /** Bulk put */
    public static void putAll(Map<String, String> map) {
        if (map != null && !map.isEmpty()) {
            ThreadContext.putAll(map);
        }
    }

    /** Snapshot current MDC */
    public static Map<String, String> snapshot() {
        Map<String, String> current = ThreadContext.getImmutableContext();
        if (current == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(new HashMap<>(current));
    }

    /** Restore MDC cleanly */
    public static void restore(Map<String, String> snapshot) {
        ThreadContext.clearAll();
        if (snapshot != null && !snapshot.isEmpty()) {
            ThreadContext.putAll(snapshot);
        }
    }

    /** Clear MDC completely */
    public static void clear() {
        ThreadContext.clearAll();
    }

    // ----------- Convenience keys -------------

    /** Routing key: used by RoutingAppender */
    public static void setScenario(String scenarioKey) {
        set("scenario", scenarioKey);
    }

    /** Human-friendly name (file-safe) */
    public static void setScenarioName(String scenarioName) {
        set("scenarioName", scenarioName);
    }

    /** Cucumber scenario ID */
    public static void setScenarioId(String scenarioId) {
        set("scenarioId", scenarioId);
    }

    /** Optional */
    public static void setOrderId(String orderId) {
        set("orderId", orderId);
    }

    /** Optional thread identifier */
    public static void setThreadId(String threadId) {
        set("threadId", threadId);
    }
}
