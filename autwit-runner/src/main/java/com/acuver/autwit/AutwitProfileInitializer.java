package com.acuver.autwit;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class AutwitProfileInitializer  implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String database = env.getProperty("autwit.database");
        if (database != null && !database.isBlank()) {
            env.addActiveProfile(database);
            System.out.println("✅ AUTWIT activated DB profile: " + database);
        }
    }
    }

