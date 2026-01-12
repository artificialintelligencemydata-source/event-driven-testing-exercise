package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
class ApiClientImpl implements Autwit.ContextAccessor.ApiClient {
    private final RuntimeContextPort contextAccess;

    @Override
    public Response createOrder(Map<String, Object> payload) {
        Object api = contextAccess.get("api");
        try {
            java.lang.reflect.Method method = api.getClass().getMethod("createOrder", Map.class);
            return (Response) method.invoke(api, payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke createOrder", e);
        }
    }
}
