package com.acuver.autwit.internal.context;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.config.XmlConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.params.CoreConnectionPNames;

public final class ApiContext {

    private static final ThreadLocal<RequestSpecification> threadLocalRequest = new ThreadLocal<>();
    private static final ThreadLocal<Response> threadLocalResponse = new ThreadLocal<>();
    private static final ThreadLocal<RestAssuredConfig> threadLocalConfig = ThreadLocal.withInitial(() ->
            RestAssured.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000))
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                    .xmlConfig(XmlConfig.xmlConfig().disableLoadingOfExternalDtd()));

    private ApiContext() { /* utility */ }

    public static void initRequest() {
        threadLocalRequest.set(RestAssured.given().config(threadLocalConfig.get()));
    }

    public static RequestSpecification request() {
        RequestSpecification req = threadLocalRequest.get();
        if (req == null) {
            throw new IllegalStateException("Request not initialized. Call ApiContextEntities.initRequest() first.");
        }
        return req;
    }

    public static void setResponse(Response response) {
        threadLocalResponse.set(response);
    }

    public static Response response() {
        return threadLocalResponse.get();
    }

    public static void cleanUp() {
        threadLocalRequest.remove();
        threadLocalResponse.remove();
        threadLocalConfig.remove();
    }
}
