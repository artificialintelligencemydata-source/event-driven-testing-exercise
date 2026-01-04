package com.acuver.autwit.runner;

import com.acuver.autwit.internal.listeners.RetrySkippedTestsExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testng.TestNG;

import java.util.List;

@SpringBootApplication(scanBasePackages = "com.acuver.autwit")
public class RunnerApp {

    @Value("${autwit.suiteFile:src/test/resources/testng.xml}")// ← default value after ':'
    private String suiteFile;

    @Value("${autwit.database:mongo}")// ← default value after ':'
    private String database;

    @Value("${autwit.environment:qa}")// ← default value after ':'
    private String environment;

    public static void main(String[] args) {
        runSpring(args);
    }

    private static void runSpring(String[] args) {

        ConfigurableApplicationContext context =
                SpringApplication.run(RunnerApp.class, args);

        RunnerApp app = context.getBean(RunnerApp.class);

        System.out.println("➡ DB        : " + app.database);
        System.out.println("➡ Env       : " + app.environment);
        System.out.println("➡ SuiteFile : " + app.suiteFile);

        TestNG testng = new TestNG();
        testng.setTestSuites(List.of(app.suiteFile));
        testng.setVerbose(2);
        testng.run();

        // Trigger retries via DB-driven ResumeEngine
        RetrySkippedTestsExecutor retry = context.getBean(RetrySkippedTestsExecutor.class);
        retry.executeRetries();

        context.close();
    }
}
