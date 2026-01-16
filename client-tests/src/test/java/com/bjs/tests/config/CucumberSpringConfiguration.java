package com.bjs.tests.config;
import com.acuver.autwit.AutwitProfileInitializer;
import com.acuver.autwit.internal.listeners.TestNGListenerNew;
import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(classes = ClientTestApplication.class)
@ContextConfiguration(initializers = AutwitProfileInitializer.class)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        TestNGListenerNew.setApplicationContext(applicationContext);
    }

    @PostConstruct
    public void verifyYaml() {
        System.out.println("✅ Test YAML loaded, profile = " +
                applicationContext.getEnvironment().getActiveProfiles()[0]);
    }
}
