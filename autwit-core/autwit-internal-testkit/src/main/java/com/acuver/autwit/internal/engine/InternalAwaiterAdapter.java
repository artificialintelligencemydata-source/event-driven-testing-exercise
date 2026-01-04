package com.acuver.autwit.internal.engine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class InternalAwaiterAdapter {

    @Autowired
    private TestKitEventBridge bridge;

    public CompletableFuture<Map<String,Object>> await(String canonicalKey) {
        CompletableFuture<Map<String,Object>> fut = new CompletableFuture<>();
        bridge.register(canonicalKey, ctx -> fut.complete(ctx));
        return fut;
    }
}
