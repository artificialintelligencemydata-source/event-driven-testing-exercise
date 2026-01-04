package com.acuver.autwit.internal.context;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Helpers to propagate MDC into asynchronous tasks (ExecutorService, CompletableFuture).
 * Usage: executor.submit(AsyncContext.wrap(() -> { ... }));
 */
public final class AsyncContext {

    private AsyncContext() { }

    public static Runnable wrap(Runnable runnable) {
        final Map<String, String> mdcSnapshot = ScenarioMDC.snapshot();
        return () -> {
            ScenarioMDC.restore(mdcSnapshot);
            try {
                runnable.run();
            } finally {
                ScenarioMDC.clear(); // avoid leaks within async thread
            }
        };
    }

    public static <V> Callable<V> wrap(Callable<V> callable) {
        final Map<String, String> mdcSnapshot = ScenarioMDC.snapshot();
        return () -> {
            ScenarioMDC.restore(mdcSnapshot);
            try {
                return callable.call();
            } finally {
                ScenarioMDC.clear();
            }
        };
    }
}
