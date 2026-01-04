package com.acuver.autwit.config;

import com.acuver.autwit.internal.listeners.TestNGListener;
import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.acuver.autwit.runner.RunnerApp;

@CucumberContextConfiguration
@SpringBootTest(classes = RunnerApp.class)
@ActiveProfiles("test") //← Tells Spring to load application-test.yaml
//@TestPropertySource(locations = "classpath:application.yml")
public class CucumberSpringConfiguration {
    // Enables Spring DI for Cucumber
    // Will use your application.yaml configuration:
    // - autwit.database=mongo (or h2, postgres)
    // - autwit.environment=qa
    // Override for testsproperties = {"autwit.database=h2","autwit.environment=test"}
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        // Make Spring context available to TestNGListener
        TestNGListener.setApplicationContext(applicationContext);
    }
    @PostConstruct
    public void verifyYaml() {
        System.out.println("✅ Test YAML loaded, profile = " +
                applicationContext.getEnvironment().getActiveProfiles()[0]);
    }
}