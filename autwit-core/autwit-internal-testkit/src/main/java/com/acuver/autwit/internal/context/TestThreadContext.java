package com.acuver.autwit.internal.context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public final class TestThreadContext {
    private static final Logger logger = LogManager.getLogger(TestThreadContext.class);

    private static final ThreadLocal<Map<String, Object>> threadContextTest =
            ThreadLocal.withInitial(HashMap::new);

    private TestThreadContext() { /* utility */ }

    public static <T> void set(String key, T value) {
        if (key == null) return;
        threadContextTest.get().put(key, value);

        if (value != null) {
            ThreadContext.put(key, value.toString());
        } else {
            ThreadContext.remove(key);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (key == null) return null;
        return (T) threadContextTest.get().get(key);
    }

    public static void clear() {
        threadContextTest.remove();
        ThreadContext.clearAll();
    }

    public static void clearAll() {
        threadContextTest.get().clear();
        ThreadContext.clearAll();
    }

    public static Map<String, Object> getContextMap() {
        return new HashMap<>(threadContextTest.get());
    }

    public static void setTestContext(String threadPool, long threadId, String scenario) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        set("testNGThreadPool", threadPool);
        set("threadId", String.valueOf(threadId));
        set("scenario", scenario);
        set("assignedAt", now);

        ThreadContext.put("assignedAt", now.toString());
        logger.info("[Thread: {}] Assigned context at {}", threadId, formatter.format(now));
    }
}
