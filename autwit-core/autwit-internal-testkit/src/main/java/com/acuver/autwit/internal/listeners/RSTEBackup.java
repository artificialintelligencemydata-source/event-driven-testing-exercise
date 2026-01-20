package com.acuver.autwit.internal.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.TestNG;

import java.util.List;

public class RSTEBackup {

    private static final Logger LOG = LogManager.getLogger(RetrySkippedTestsExecutor.class);
    private static final int MAX_RETRY = 3;

    public void executeRetries() {
        LOG.info("Starting retry executor...");
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            List<ITestResult> skipped = TestNGListenerNew.skippedTests.stream().filter(r -> r.getStatus() == ITestResult.SKIP).toList();
            if (skipped.isEmpty()) {
                LOG.info("No skipped tests remaining.");
                break;
            }
            LOG.info("Retry attempt {} for {} skipped tests", attempt, skipped.size());

            // Build and run TestNG suite programmatically (simplified)
            try {
                TestNG tng = new TestNG();
                // In real code we build XmlSuite to run only selected methods
                tng.run();
            } catch (Exception e) {
                LOG.error("Retry run failed", e);
            }
        }
    }
}
