package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.ScenarioStatePort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ContextAccessorImpl implements Autwit.ContextAccessor {

    private final RuntimeContextPort contextAccess;

    @Override
    public void setCurrentStep(String stepName) {contextAccess.set("currentStep", stepName);}

    @Override
    public <T> void set(String key, T value) {
        contextAccess.set(key, value);
    }

    @Override
    public <T> T get(String key) {
        return contextAccess.get(key);
    }

    @Override
    public Autwit.ContextAccessor.ApiClient api() {
        return new ApiClientImpl(contextAccess);
    }

    @Override
    public Autwit.ContextAccessor.SoftAssertions assertions() {
        return new SoftAssertionsImpl();
    }

    @Override
    public void setOrderId(String orderId) {
        contextAccess.set("orderId", orderId);
        try {
            Class<?> mdcClass = Class.forName("com.acuver.autwit.internal.context.ScenarioMDC");
            java.lang.reflect.Method method = mdcClass.getMethod("setOrderId", String.class);
            method.invoke(null, orderId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set MDC orderId", e);
        }
    }
}