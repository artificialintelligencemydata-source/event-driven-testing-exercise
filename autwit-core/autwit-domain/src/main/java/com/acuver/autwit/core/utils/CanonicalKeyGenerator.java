package com.acuver.autwit.core.utils;

public class CanonicalKeyGenerator {

    public static String forOrder(String orderId, String expectedEvent) {
        return orderId + "::" + expectedEvent;
    }

    public static String generate(String featureName, int scenarioLine, String exampleId, String orderId, String expectedEvent) {
        return String.join("::",
                featureName,
                String.valueOf(scenarioLine),
                exampleId,
                orderId,
                expectedEvent
        );
    }
}
