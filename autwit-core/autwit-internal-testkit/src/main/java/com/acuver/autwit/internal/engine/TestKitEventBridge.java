package com.acuver.autwit.internal.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Minimal in-memory notifier used by EventStepAwaiter to register callbacks.
 * In production, ResumeEngine will call complete() when DB/adapter flips resumeReady.
 */
@Component
public class TestKitEventBridge {

    private final Map<String, Consumer<Map<String,Object>>> listeners = new ConcurrentHashMap<>();

    public void register(String key, Consumer<Map<String,Object>> callback){
        listeners.put(key, callback);
    }

    public void complete(String key, Map<String,Object> ctx){
        var cb = listeners.remove(key);
        if (cb != null) cb.accept(ctx);
    }
}
