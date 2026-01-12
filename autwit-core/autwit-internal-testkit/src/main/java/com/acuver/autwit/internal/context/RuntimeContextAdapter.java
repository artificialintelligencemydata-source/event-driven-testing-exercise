package com.acuver.autwit.internal.context;

import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements RuntimeContextPort by delegating to ScenarioContext.
 * 
 * This bridges the port interface (in core) with the ThreadLocal implementation (in internal-testkit).
 */
@Component
public class RuntimeContextAdapter implements RuntimeContextPort {

    @Override
    public <T> void set(String key, T value) {
        ScenarioContext.set(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return ScenarioContext.get(key);
    }

    @Override
    public void remove(String key) {
        ScenarioContext.remove(key);
    }
}

