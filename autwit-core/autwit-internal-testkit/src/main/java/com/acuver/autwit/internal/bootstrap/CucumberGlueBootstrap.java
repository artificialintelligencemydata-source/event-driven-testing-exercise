package com.acuver.autwit.internal.bootstrap;

public class CucumberGlueBootstrap {
    static {
        String baseGlue = System.getProperty("cucumber.glue");
        String extraGlue = System.getProperty("cucumber.glue.additional");

        if (extraGlue != null && !extraGlue.isBlank()) {
            System.setProperty(
                    "cucumber.glue",
                    baseGlue + "," + extraGlue
            );
        }
    }
}
