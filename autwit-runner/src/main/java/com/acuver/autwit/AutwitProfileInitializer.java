package com.acuver.autwit;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class AutwitProfileInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    // 🔥 TEMP DEBUG — ADD HERE
    static {
        System.out.println(" == AutwitProfileInitializer LOADED");
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String database = env.getProperty("autwit.database");

        System.out.println("== AUTWIT Profile Initializer invoked");
        System.out.println("== Existing active profiles: " +
                String.join(",", env.getActiveProfiles()));

        if (database == null || database.isBlank()) {
            System.out.println("⚠== AUTWIT: 'autwit.database' not set. No DB profile activated.");
            return;
        }

        boolean alreadyActive = java.util.Arrays.asList(env.getActiveProfiles())
                .contains(database);

        if (alreadyActive) {
            System.out.println("== AUTWIT: DB profile already active: " + database);
            return;
        }

        env.addActiveProfile(database);
        String driverClass = env.getProperty("spring.datasource.driver-class-name",
                "org.postgresql.Driver");
        System.out.println("== AUTWIT driverClasse: " + driverClass);
        System.out.println("== AUTWIT activated DB profile: " + database);
        System.out.println("== Final active profiles: " +
                String.join(",", env.getActiveProfiles()));
    }
}
