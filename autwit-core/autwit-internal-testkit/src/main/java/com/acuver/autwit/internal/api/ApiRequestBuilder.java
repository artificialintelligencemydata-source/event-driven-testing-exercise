package com.acuver.autwit.internal.api;
import com.acuver.autwit.internal.context.ApiContext;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
public final class ApiRequestBuilder {
    private ApiRequestBuilder(){}
    public static RequestSpecification buildRequest(String contentType, Map<String,Object> headers, Object body) {
        ApiContext.initRequest();
        RequestSpecification req = ApiContext.request();
        if (contentType != null && !contentType.isEmpty()) req.header("Content-Type", contentType);
        if (headers != null) headers.forEach((k,v) -> req.header(k, String.valueOf(v)));
        if (body != null) req.body(body);
        return req;
    }
}
