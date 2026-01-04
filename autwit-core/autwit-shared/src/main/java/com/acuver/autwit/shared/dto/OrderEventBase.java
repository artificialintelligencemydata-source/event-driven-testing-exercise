
package com.acuver.autwit.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
public class OrderEventBase {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("eventType")
    private String eventType;
    @JsonProperty("payload")
    private String payload;

    public String getOrderId(){return orderId;}
    public void setOrderId(String v){this.orderId=v;}
    public String getEventType(){return eventType;}
    public void setEventType(String v){this.eventType=v;}
    public String getPayload(){return payload;}
    public void setPayload(String v){this.payload=v;}
}
