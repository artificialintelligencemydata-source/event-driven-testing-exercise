package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import lombok.RequiredArgsConstructor;

/**
 * Context Accessor Implementation.
 *
 * Provides access to:
 * - Scenario context (get/set)
 * - BaseActions (generic API calls)
 * - Sterling API client
 * - XML utilities
 * - Configuration
 * - Soft assertions
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@RequiredArgsConstructor
class ContextAccessorImpl implements Autwit.ContextAccessor {

    private final RuntimeContextPort contextAccess;

    // Lazy-initialized components
    private BaseActionsImpl baseActions;
    private SterlingApiImpl sterlingApi;
    private XmlUtilsImpl xmlUtils;
    private ConfigReaderImpl configReader;

    @Override
    public void setCurrentStep(String stepName) {
        contextAccess.set("currentStep", stepName);
    }

    @Override
    public <T> void set(String key, T value) {
        contextAccess.set(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return contextAccess.get(key);
    }

    @Override
    public void setOrderId(String orderId) {
        contextAccess.set("orderId", orderId);
        // Set in MDC for logging correlation
        try {
            Class<?> mdcClass = Class.forName("com.acuver.autwit.internal.context.ScenarioMDC");
            java.lang.reflect.Method method = mdcClass.getMethod("setOrderId", String.class);
            method.invoke(null, orderId);
        } catch (Exception e) {
            // MDC not available - ignore silently
        }
    }

    @Override
    public Autwit.ContextAccessor.BaseActions baseActions() {
        if (baseActions == null) {
            baseActions = new BaseActionsImpl();
        }
        return baseActions;
    }

    @Override
    public Autwit.ContextAccessor.SterlingApi sterling() {
        if (sterlingApi == null) {
            sterlingApi = new SterlingApiImpl();
        }
        return sterlingApi;
    }

    @Override
    public Autwit.ContextAccessor.XmlUtils xml() {
        if (xmlUtils == null) {
            xmlUtils = new XmlUtilsImpl();
        }
        return xmlUtils;
    }

    @Override
    public Autwit.ContextAccessor.ConfigReader config() {
        if (configReader == null) {
            configReader = new ConfigReaderImpl();
        }
        return configReader;
    }

    @Override
    public Autwit.ContextAccessor.SoftAssertions assertions() {
        return new SoftAssertionsImpl();
    }
}