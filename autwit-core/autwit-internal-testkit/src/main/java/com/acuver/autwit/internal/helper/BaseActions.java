
package com.acuver.autwit.internal.helper;
import com.acuver.autwit.internal.api.ApiRequestBuilder;
import com.acuver.autwit.internal.context.ApiContext;
import com.acuver.autwit.internal.logging.JsonXmlLogBuilder;
import com.acuver.autwit.internal.reporting.AllureAttachmentUtils;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class BaseActions {
    private BaseActions(){}
    private static final Logger logger = LogManager.getLogger(BaseActions.class);
    public static void logResponse(String apiName, String responseBody){
        String pretty = JsonXmlLogBuilder.build(apiName + " RESPONSE", responseBody);
        logger.info(pretty);
    }
    public static void logRequest(String apiName, String requestBody){
        String pretty = JsonXmlLogBuilder.build(apiName + " REQUEST", requestBody);
        logger.info(pretty);
    }
    public static Response makeAPICall(String fullUrl, String httpMethod, Map<String,Object> apiInput, String contentType, Map<String,Object> headers) {
        RequestSpecification request = ApiRequestBuilder.buildRequest(contentType, headers, apiInput);

        // Logging request
        String payload = apiInput == null ? "" : apiInput.toString();
        System.out.println(JsonXmlLogBuilder.build("API REQUEST -> " + fullUrl, payload));

        Response response;
        switch (httpMethod.toUpperCase()) {
            case "GET" -> response = request.get(fullUrl);
            case "POST" -> response = request.post(fullUrl);
            case "PUT" -> response = request.put(fullUrl);
            case "DELETE" -> response = request.delete(fullUrl);
            default -> throw new IllegalArgumentException("Unsupported method: " + httpMethod);
        }

        String resp = response == null ? "<null>" : response.asString();
        System.out.println(JsonXmlLogBuilder.build("API RESPONSE <- " + fullUrl, resp));
        AllureAttachmentUtils.saveLargePayload("API " + fullUrl + " Response", resp, "json");
        ApiContext.cleanUp();
        return response;
    }
}
