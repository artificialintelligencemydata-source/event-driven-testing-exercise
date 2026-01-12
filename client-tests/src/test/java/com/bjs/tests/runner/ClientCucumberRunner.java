package com.bjs.tests.runner;
import com.acuver.autwit.runner.BaseAutwitCucumberRunner;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.bjs.tests.stepDefinitions",
                "com.bjs.tests.config",
                "com.acuver.autwit.internal"
        },
        plugin = {
                "pretty",
                "html:target/cucumber-reports.html"
        },
        monochrome = true
)
public class ClientCucumberRunner extends BaseAutwitCucumberRunner {
}

