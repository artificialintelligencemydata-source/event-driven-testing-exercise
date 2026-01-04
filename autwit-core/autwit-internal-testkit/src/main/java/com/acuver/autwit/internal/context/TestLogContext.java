package com.acuver.autwit.internal.context;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@Component
public class TestLogContext  implements AsyncConfigurer {
    public static void initScenario(String scenarioName) {
        System.out.println("scenarioName is in initScenario TestLogContext "+scenarioName);
        ThreadContext.put("scenario", scenarioName);
        ThreadContext.put("scenarioId", UUID.randomUUID().toString());
        ThreadContext.put("threadId", Thread.currentThread().getName());
    }

    public static void setOrderId(String orderId) {
        ThreadContext.put("orderId", orderId);
    }

    public static void clear() {
        ThreadContext.clearAll();
    }
    public static Map<String, String> snapshot() {
        return ThreadContext.getImmutableContext();
    }
    public static void restore(Map<String, String> context) {
        ThreadContext.clearAll();
        if (context != null) ThreadContext.putAll(context);
    }
    @Override
    public java.util.concurrent.Executor getAsyncExecutor() {
        return runnable -> {
            Map<String, String> parentContext = snapshot();
            Runnable contextAware = () -> {
                restore(parentContext);
                runnable.run();
            };
            new Thread(contextAware).start();
        };
    }

    public static <V> Callable<V> wrap(Callable<V> task) {
        Map<String, String> context = snapshot();
        return () -> {
            restore(context);
            return task.call();
        };
    }
    public static String summary() {
        return String.format("[Scenario: %s | Order: %s | Thread: %s]",
                ThreadContext.get("scenario"),
                ThreadContext.get("orderId"),
                ThreadContext.get("threadId"));
    }
}
