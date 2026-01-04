package com.acuver.autwit.internal.api;

import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.helper.BaseActions;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Thin facade for test steps to call APIs.
 * Keep step-level logic here to avoid exposing internals.
 */
public class ApiCalls {

    public Response createOrder(Map<String,Object> payload) {
        String url = FileReaderManager.getInstance()
                .getConfigReader()
                .getCreateOrderEndPoint();

        return BaseActions.makeAPICall(url, "POST", payload, "application/json", null);
    }

    public Response getShipment(String orderId) {
        String url = FileReaderManager.getInstance()
                .getConfigReader()
                .getShipmentBaseUri();
        return BaseActions.makeAPICall(url + "/" + orderId, "GET", null, "application/json", null);
    }
}
