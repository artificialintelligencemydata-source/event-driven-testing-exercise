package com.acuver.autwit.runner;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import java.util.Arrays;

public abstract class BaseAutwitCucumberRunner
        extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(name = "scenarios", parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @DataProvider(name = "filteredScenarios", parallel = true)
    public Object[][] filteredScenarios(ITestContext context) {

        String retryParam = context.getCurrentXmlTest()
                .getParameter("scenariosToRun");

        Object[][] original = scenarios();

        if (retryParam == null || retryParam.isBlank()) {
            return original;
        }

        String[] wanted = retryParam.split(",");

        return Arrays.stream(original)
                .filter(s -> Arrays.stream(wanted)
                        .anyMatch(w -> s[0].toString().contains(w.trim())))
                .toArray(Object[][]::new);
    }
}

