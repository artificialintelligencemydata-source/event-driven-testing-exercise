package com.acuver.autwit.runner;
import com.acuver.autwit.internal.listeners.TestNGListenerNew;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import java.util.Arrays;
@Listeners(TestNGListenerNew.class)
public abstract class BaseAutwitCucumberRunner
        extends AbstractTestNGCucumberTests {
    @Override
    @DataProvider(name = "scenarios", parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }

    @DataProvider(name = "filteredScenarios", parallel = false)
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

