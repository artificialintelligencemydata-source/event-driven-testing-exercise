package com.acuver.autwit.core.ports.runtime;

/**
 * Port for accessing runtime (ThreadLocal) scenario context.
 * 
 * This port provides access to execution-time, thread-scoped scenarioStateTracker.
 * It is NOT for persisted scenarioStateTracker (use ScenarioContextPort for that).
 * 
 * Implemented by internal-testkit's RuntimeContextAdapter.
 */
public interface RuntimeContextPort {
    <T> void set(String key, T value);
    <T> T get(String key);
    void remove(String key);
    void clear();
}

