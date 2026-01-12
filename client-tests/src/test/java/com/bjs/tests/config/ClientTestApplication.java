package com.bjs.tests.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "com.acuver.autwit",   // AUTWIT framework beans
                "com.bjs.tests"        // Client beans (step defs, configs)
        }
)
public class ClientTestApplication {
}
