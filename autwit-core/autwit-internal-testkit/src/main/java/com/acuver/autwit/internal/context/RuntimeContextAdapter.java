package com.acuver.autwit.internal.context;

import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import org.springframework.stereotype.Component;

@Component
public class RuntimeContextAdapter implements RuntimeContextPort {

    @Override
    public <T> void set(String key, T value) {
        ScenarioContext.set(key, value);
    }

    @Override
    public <T> T get(String key) {
        return ScenarioContext.get(key);
    }

    @Override
    public void remove(String key) {
        ScenarioContext.remove(key);
    }
    public void clear() {
        ScenarioContext.clear();
        // optional but best:
        ScenarioContext.removeThread();
    }
}
