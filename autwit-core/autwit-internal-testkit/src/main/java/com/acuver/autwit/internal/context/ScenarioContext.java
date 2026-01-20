package com.acuver.autwit.internal.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime (ThreadLocal) scenario context.
 * 
 * This is execution-time, thread-scoped scenarioStateTracker.
 * It is NOT persisted scenarioStateTracker (see ScenarioStateContext in core.domain).
 */
public final class ScenarioContext {

    private static final ThreadLocal<Map<String, Object>> store =
            ThreadLocal.withInitial(HashMap::new);

    private ScenarioContext() {}

    public static <T> void set(String key, T value) {
        store.get().put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) store.get().get(key);
    }

    public static void remove(String key) {
        store.get().remove(key);
    }

    public static void clear() {
        store.get().clear();
    }

    // Convenience
    public static void initApi(Object api) {
        set("api", api);
    }

    public static <T> T api() {
        return get("api");
    }
    public static void removeThread() {store.remove(); }
}

