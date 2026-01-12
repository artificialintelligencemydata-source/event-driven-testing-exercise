package com.bjs.tests.config;
import com.acuver.autwit.internal.listeners.TestNGListener;
import io.cucumber.spring.CucumberContextConfiguration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(classes = ClientTestApplication.class)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        TestNGListener.setApplicationContext(applicationContext);
    }

    @PostConstruct
    public void verifyYaml() {
        System.out.println("✅ Test YAML loaded, profile = " +
                applicationContext.getEnvironment().getActiveProfiles()[0]);
    }
}
