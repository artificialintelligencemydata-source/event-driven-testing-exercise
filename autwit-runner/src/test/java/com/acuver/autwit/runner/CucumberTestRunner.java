package com.acuver.autwit.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.acuver.autwit.stepdef",
                "com.acuver.autwit.internal",
                "com.acuver.autwit.config"
        }
,
        plugin = {
                "pretty",
                "html:target/cucumber-reports.html",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true
)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {

    private List<String> scenariosToRun = null;

    @Override
    @DataProvider(name = "scenarios", parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @DataProvider(name = "filteredScenarios", parallel = true)
    public Object[][] filteredScenarios(ITestContext context) {

        String retryParam = context.getCurrentXmlTest().getParameter("scenariosToRun");

        Object[][] original = scenarios();

        if (retryParam == null || retryParam.isBlank()) {
            return original;
        }

        System.out.println("🔁 RETRY MODE: Running only: " + retryParam);

        List<String> scenariosToRun = Arrays.stream(retryParam.split(","))
                .map(String::trim)
                .toList();

        return Arrays.stream(original)
                .filter(s -> scenariosToRun.stream().anyMatch(s[0].toString()::contains))
                .toArray(Object[][]::new);
    }


    private boolean shouldRunScenario(Object scenarioObj) {
        String scenarioName = scenarioObj.toString();
        return scenariosToRun.stream()
                .anyMatch(scenarioName::contains);
    }
}
