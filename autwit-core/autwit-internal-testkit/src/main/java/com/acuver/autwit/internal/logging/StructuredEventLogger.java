package com.acuver.autwit.internal.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Map;

public final class StructuredEventLogger {
    private static final Logger LOGGER = LogManager.getLogger("StructuredEventLogger");
    private StructuredEventLogger(){}

    public static void event(String eventType, String correlationId, Map<String,Object> payload) {
        String message = String.format("{\"ts\":\"%s\",\"event\":\"%s\",\"correlation\":\"%s\",\"payload\":%s}",
                Instant.now().toString(), eventType, correlationId, toJson(payload));
        LOGGER.info(message);
    }

    private static String toJson(Map<String,Object> payload){
        try {
            return JsonXmlLogBuilder.formatJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload));
        } catch (Exception e){ return "{}"; }
    }
}
