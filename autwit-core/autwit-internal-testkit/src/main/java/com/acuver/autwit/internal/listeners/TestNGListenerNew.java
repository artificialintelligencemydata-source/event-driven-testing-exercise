package com.acuver.autwit.internal.listeners;


import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.TestThreadContext;
import com.acuver.autwit.internal.helper.BaseActions;
import com.acuver.autwit.internal.integration.CreateIssue;

import io.cucumber.java.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.testng.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

/**
 * Enterprise TestNG Listener with Spring Integration.
 *
 * <h2>FEATURES</h2>
 * <ul>
 *   <li>Thread-safe test data directory management</li>
 *   <li>MDC-aware logging with thread correlation</li>
 *   <li>Jira ticket creation on failure</li>
 *   <li>Retry framework support</li>
 *   <li>Soft assertion error collection</li>
 *   <li>Config file per-thread isolation</li>
 * </ul>
 *
 * <h2>COMPARISON: OLD vs NEW</h2>
 * <pre>
 * ┌────────────────────────────────────┬─────────────┬─────────────┐
 * │ Feature                            │ OLD         │ NEW         │
 * ├────────────────────────────────────┼─────────────┼─────────────┤
 * │ ITestListener                      │ ✓           │ ✓           │
 * │ ISuiteListener                     │ ✗           │ ✓           │
 * │ onStart (ITestContext)             │ ✓           │ ✓           │
 * │ onTestStart                        │ ✓           │ ✓           │
 * │ onTestSuccess                      │ ✓           │ ✓           │
 * │ onTestFailure                      │ ✓           │ ✓           │
 * │ onTestSkipped                      │ ✓           │ ✓           │
 * │ onTestFailedButWithinSuccess%      │ ✓           │ ✓           │
 * │ onFinish (ITestContext)            │ ✓           │ ✓           │
 * │ Thread-safe test data copy         │ ✓           │ ✓           │
 * │ Config file per-thread             │ ✓           │ ✓           │
 * │ MDC thread correlation             │ ✓           │ ✓           │
 * │ Jira ticket on failure             │ ✓           │ ✓           │
 * │ Soft assertion collection          │ ✓           │ ✓           │
 * │ Error response XML parsing         │ ✓           │ ✓           │
 * │ Directory cleanup                  │ ✓           │ ✓           │
 * │ Retry framework (skipped tracking) │ ✗           │ ✓           │
 * │ Spring ApplicationContext          │ ✗           │ ✓           │
 * │ ThreadLocal SterlingApiCalls       │ ✓           │ ✓           │
 * │ Unique test key computation        │ ✗           │ ✓           │
 * └────────────────────────────────────┴─────────────┴─────────────┘
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
public class TestNGListenerNew implements ITestListener, ISuiteListener {

    private static final Logger log = LogManager.getLogger(TestNGListenerNew.class);

    // ==========================================================================
    // CONSTANTS (from OLD)
    // ==========================================================================

    private static final String BASE_DIRECTORY = "target/testData";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String ERROR_RESPONSE_PATH = "target/testData/responseXmls/ErrorResponse.xml";

    // ==========================================================================
    // THREAD-SAFE COLLECTIONS (NEW - for retry framework)
    // ==========================================================================

    /** Thread-safe lists for retry framework */
    public static final List<ITestResult> skippedTests = new CopyOnWriteArrayList<>();
    public static final Set<String> passedTestKeys = ConcurrentHashMap.newKeySet();
    public static final Set<String> failedTestKeys = ConcurrentHashMap.newKeySet();

    /** Attribute key for caching computed test key */
    public static final String TEST_KEY_ATTR = "AUTWIT_SCENARIO_KEY";

    // ==========================================================================
    // THREAD-LOCAL STORAGE (from OLD)
    // ==========================================================================

    /** Thread-local SterlingApiCalls instance (from OLD) */
    public static final ThreadLocal<Object> apiCalls = new ThreadLocal<>();

    /** Thread-local current scenario */
    private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>();

    // ==========================================================================
    // SYNCHRONIZATION (from OLD)
    // ==========================================================================

    private static final Lock fileLock = new ReentrantLock();

    // ==========================================================================
    // SPRING INTEGRATION (NEW)
    // ==========================================================================

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    public static void setCurrentScenario(Scenario scenario) {
        currentScenario.set(scenario);
    }

    // ==========================================================================
    // SUITE LISTENER METHODS (NEW)
    // ==========================================================================

    @Override
    public void onStart(ISuite suite) {
        log.info("════════════════════════════════════════════════════════════════");
        log.info("  SUITE START: {}", suite.getName());
        log.info("════════════════════════════════════════════════════════════════");
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("════════════════════════════════════════════════════════════════");
        log.info("  SUITE FINISH: {} | Passed: {} | Failed: {} | Skipped: {}",
                suite.getName(), passedTestKeys.size(), failedTestKeys.size(), skippedTests.size());
        log.info("════════════════════════════════════════════════════════════════");
    }

    // ==========================================================================
    // TEST LISTENER METHODS (from OLD + enhanced)
    // ==========================================================================

    /**
     * Called when test context starts (from OLD: onStart)
     */
    public void onStart(ITestContext context) {
        log.info("TestNG Context Start: {}", context.getName());
    }

    /**
     * Called when each test starts.
     * Sets up thread-specific test data directory.
     * (from OLD: onTestStart)
     */
    @Override
    public void onTestStart(ITestResult result) {
        long threadId = Thread.currentThread().threadId();

        // Set MDC for logging correlation (from OLD)
        ThreadContext.put("threadId", String.valueOf(threadId));
        ScenarioContext.set("testNGThreadId", threadId);

        log.info("➡ Test START [{}] on thread {}", getTestKey(result), threadId);

        // Setup thread-specific directories (from OLD)
        String destination = BASE_DIRECTORY + "/Thread_" + threadId;
        String source = "src/test/resources/testData";
        File sourceDirectory = new File(source);
        File destinationDirectory = new File(destination);

        // Initialize thread-local SterlingApiCalls (from OLD)
        try {
            Class<?> sterlingClass = Class.forName("com.acuver.autwit.internal.api.SterlingApiCalls");
            apiCalls.set(sterlingClass.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            log.warn("Could not initialize SterlingApiCalls: {}", e.getMessage());
        }

        // Copy test data to thread-specific directory (from OLD)
        try {
            createDirectoryIfNotExists(destinationDirectory, threadId);
            copyDirectory(sourceDirectory, destinationDirectory, threadId);
            copyConfigFile(destinationDirectory, threadId);
            updateConfigFilePath(destinationDirectory.getPath(), threadId);
        } catch (IOException e) {
            log.error("Failed to initialize directories and copy files for thread {}", threadId, e);
        }
    }

    /**
     * Called when test succeeds.
     * (from OLD: onTestSuccess + NEW: retry tracking)
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        String key = getTestKey(result);

        // Track for retry framework (NEW)
        passedTestKeys.add(key);
        skippedTests.removeIf(r -> getTestKey(r).equals(key));

        log.info("✓ PASS [{}]", key);
    }

    /**
     * Called when test fails.
     * Creates Jira ticket if configured.
     * (from OLD: onTestFailure + enhanced)
     */
    @Override
    public void onTestFailure(ITestResult result) {
        String key = getTestKey(result);

        // Track for retry framework (NEW)
        failedTestKeys.add(key);
        skippedTests.removeIf(r -> getTestKey(r).equals(key));

        log.error("✗ FAIL [{}] — {}", key,
                (result.getThrowable() != null ? result.getThrowable().getMessage() : "No error"));

        // Create Jira ticket (from OLD)
        createJiraTicket(result);
    }

    /**
     * Called when test is skipped.
     * (from OLD: onTestSkipped + NEW: retry tracking)
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        String key = getTestKey(result);

        // Preserve scenario name (NEW)
        String scenarioName = TestThreadContext.get("scenario");
        if (scenarioName != null) {
            result.setTestName(scenarioName);
            result.setAttribute("SCENARIO_NAME", scenarioName);
        }

        // Track for retry framework if not already passed (NEW)
        if (!passedTestKeys.contains(key)) {
            skippedTests.add(result);
            log.warn("⏩ SKIPPED [{}]", key);
        }
    }

    /**
     * Called when test fails but within success percentage.
     * (from OLD: onTestFailedButWithinSuccessPercentage)
     */
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        log.info("⚠ PARTIAL FAIL [{}]", getTestKey(result));
    }

    /**
     * Called when test context finishes.
     * Cleans up thread-specific resources.
     * (from OLD: onFinish)
     */
    @Override
    public void onFinish(ITestContext context) {
        long threadId = Thread.currentThread().threadId();
        log.info("TestNG Context Finish: {} on thread {}", context.getName(), threadId);

        // Cleanup test data directory (from OLD)
        File directory = new File(BASE_DIRECTORY + "/testdata");
        if (directory.exists() && directory.isDirectory()) {
            boolean deleted = deleteDirectoryRecursively(directory);
            if (deleted) {
                log.debug("Directory deleted: {}", directory.getAbsolutePath());
            } else {
                log.warn("Failed to delete directory: {}", directory.getAbsolutePath());
            }
        }

        // Clear MDC and thread context (from OLD)
        ThreadContext.remove("threadId");
        TestThreadContext.clear();
        apiCalls.remove();
        currentScenario.remove();
    }

    // ==========================================================================
    // UNIQUE TEST KEY (NEW - for retry framework)
    // ==========================================================================

    /**
     * Computes a stable unique key for each scenario.
     * Needed for retry logic and DB-resume logic.
     */
    public static String getTestKey(ITestResult result) {
        Object cached = result.getAttribute(TEST_KEY_ATTR);
        if (cached != null) return cached.toString();

        String methodName = result.getMethod().getQualifiedName();
        StringBuilder key = new StringBuilder(methodName);

        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            key.append("::");
            for (Object p : params) {
                key.append(p == null ? "null" : p.toString()).append("|");
            }
        }

        String scenarioName = TestThreadContext.get("scenario");
        if (scenarioName != null) {
            key.append("[").append(scenarioName).append("]");
        }

        String finalKey = key.toString();
        result.setAttribute(TEST_KEY_ATTR, finalKey);
        return finalKey;
    }

    // ==========================================================================
    // JIRA TICKET CREATION (from OLD + enhanced)
    // ==========================================================================

    /**
     * Creates Jira ticket on test failure.
     */
    private void createJiraTicket(ITestResult result) {
        try {
            Scenario scenario = currentScenario.get();

            // Fallback: try to get from HooksOld (from OLD)
            if (scenario == null) {
                try {
                    Class<?> hooksClass = Class.forName("com.acuver.autwit.internal.hooks.HooksOld");
                    java.lang.reflect.Method method = hooksClass.getMethod("AfterTest");
                    scenario = (Scenario) method.invoke(null);
                } catch (Exception e) {
                    log.debug("Could not get scenario from HooksOld: {}", e.getMessage());
                }
            }

            if (scenario == null) {
                log.warn("Cannot create Jira ticket - scenario not available");
                return;
            }

            log.info("Processing failed scenario: {}", scenario.getName());

            StringBuilder description = new StringBuilder();
            description.append("Scenario name: ").append(scenario.getName())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());

            // Collect soft assertion errors (from OLD)
            try {
                SoftAssertUtils.assertAll();
            } catch (AssertionError e) {
                log.error("Assertion errors found: {}", e.getMessage());
                handleAssertionFailure(e, description);
            }

            String summary = "Test Failed: " + scenario.getName();
            logIssueIfNeeded(summary, description.toString());

        } catch (Exception e) {
            log.error("Error creating Jira ticket", e);
        }
    }

    /**
     * Handles assertion failure and extracts error details.
     * (from OLD: handleAssertionFailure)
     */
    private void handleAssertionFailure(AssertionError e, StringBuilder description) {
        log.error("Assertion failed during scenario execution: {}", e.getMessage());

        // Try to extract error from XML response (from OLD)
        try {
            String errorResponsePath = getErrorResponsePath();
            String errorMessage = BaseActions.XMLXpathReader(errorResponsePath, "/Errors/Error/@ErrorDescription");

            if (errorMessage != null && !errorMessage.isEmpty()) {
                description.append("API Error: ").append(errorMessage)
                        .append(System.lineSeparator());
            }
        } catch (Exception ex) {
            log.debug("Could not extract error from XML: {}", ex.getMessage());
        }

        description.append("Assertion Error: ").append(e.getMessage())
                .append(System.lineSeparator());
    }

    /**
     * Get error response path for current thread.
     */
    private String getErrorResponsePath() {
        long threadId = Thread.currentThread().threadId();
        return BASE_DIRECTORY + "/Thread_" + threadId + "/responseXmls/ErrorResponse.xml";
    }

    /**
     * Logs issue to Jira if configured.
     * (from OLD: logIssueIfNeeded + Spring integration)
     */
    private void logIssueIfNeeded(String summary, String description) {
        try {
            boolean isTicketLoggingRequired;

            // Try Spring context first (NEW)
            if (applicationContext != null) {
                try {
                    FileReaderManager fileReaderManager = applicationContext.getBean(FileReaderManager.class);
                    isTicketLoggingRequired = fileReaderManager.getInstance()
                            .getConfigReader()
                            .isTicketNeedsToBeLogged();
                } catch (Exception e) {
                    // Fallback to static access (from OLD)
                    isTicketLoggingRequired = FileReaderManager.getInstance()
                            .getConfigReader()
                            .isTicketNeedsToBeLogged();
                }
            } else {
                // Static access (from OLD)
                isTicketLoggingRequired = FileReaderManager.getInstance()
                        .getConfigReader()
                        .isTicketNeedsToBeLogged();
            }

            if (!isTicketLoggingRequired) {
                log.debug("Jira ticket logging is disabled");
                return;
            }

            // Create issue
            if (applicationContext != null) {
                try {
                    CreateIssue createIssue = applicationContext.getBean(CreateIssue.class);
                    createIssue.createIssue("Bug", summary, description);
                } catch (Exception e) {
                    // Fallback to static method (from OLD)
                    Class<?> createIssueClass = Class.forName("com.acuver.autwit.internal.integration.CreateIssue");
                    java.lang.reflect.Method method = createIssueClass.getMethod(
                            "createIssue", String.class, String.class, String.class);
                    method.invoke(null, "Bug", summary, description);
                }
            } else {
                // Static method (from OLD)
                Class<?> createIssueClass = Class.forName("com.acuver.autwit.internal.integration.CreateIssue");
                java.lang.reflect.Method method = createIssueClass.getMethod(
                        "createIssue", String.class, String.class, String.class);
                method.invoke(null, "Bug", summary, description);
            }

            log.info("✅ Jira issue created successfully");

        } catch (Exception e) {
            log.error("Error creating Jira issue: {}", e.getMessage(), e);
        }
    }

    // ==========================================================================
    // DIRECTORY MANAGEMENT (from OLD)
    // ==========================================================================

    /**
     * Create directory if not exists.
     */
    private void createDirectoryIfNotExists(File directory, long threadId) throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            log.error("Failed to create directory for Thread {}: {}", threadId, directory.getAbsolutePath());
            throw new IOException("Failed to create directory: " + directory.getAbsolutePath());
        }
    }

    /**
     * Copy directory with thread-safety.
     */
    private void copyDirectory(File sourceDirectory, File destinationDirectory, long threadId) throws IOException {
        fileLock.lock();
        try {
            Path sourceDirPath = Paths.get(sourceDirectory.getPath());
            Path destinationDirPath = Paths.get(destinationDirectory.getPath());
            createDirectoryIfNotExists(destinationDirPath.toFile(), threadId);
            cleanupDestinationDirectory(destinationDirPath, threadId);
            copyFilesWithRetry(sourceDirPath, destinationDirPath, threadId);
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * Copy files with retry logic.
     */
    private void copyFilesWithRetry(Path sourceDirPath, Path destinationDirPath, long threadId) throws IOException {
        AtomicInteger copyFileCounter = new AtomicInteger();

        try (Stream<Path> paths = Files.walk(sourceDirPath)) {
            paths.forEach(sourcePath -> {
                int retries = 0;
                boolean fileCopied = false;

                while (!fileCopied && retries < MAX_RETRIES) {
                    try {
                        Path destinationPath = destinationDirPath.resolve(sourceDirPath.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            Files.createDirectories(destinationPath);
                        } else {
                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        copyFileCounter.incrementAndGet();
                        fileCopied = true;
                    } catch (IOException e) {
                        retries++;
                        log.warn("Retrying copy for file: {}, attempt {}", sourcePath, retries);
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!fileCopied) {
                    log.error("Failed to copy file after {} retries: {}", MAX_RETRIES, sourcePath);
                }
            });
        } catch (IOException e) {
            log.error("Error walking directory: {}", sourceDirPath, e);
            throw e;
        }

        log.debug("Thread {} copied {} files", threadId, copyFileCounter.get());
    }

    /**
     * Copy config file to thread-specific directory.
     */
    private void copyConfigFile(File destinationDirectory, long threadId) {
        try {
            String configPath = "src/test/resources/configs/config.properties";
            Path sourceConfigPath = Paths.get(configPath);
            Path destinationConfigPath = destinationDirectory.toPath().resolve(CONFIG_FILE_NAME);

            if (Files.exists(sourceConfigPath)) {
                if (!Files.exists(destinationConfigPath)) {
                    Files.copy(sourceConfigPath, destinationConfigPath);
                    log.info("Config file copied to: {}", destinationConfigPath);
                }
            } else {
                log.warn("Config file does not exist: {}", sourceConfigPath);
            }
        } catch (IOException e) {
            log.error("Failed to copy config file", e);
        }
    }

    /**
     * Cleanup destination directory before copy.
     */
    private void cleanupDestinationDirectory(Path destinationDirPath, long threadId) throws IOException {
        AtomicInteger deleteCounter = new AtomicInteger(0);
        AtomicInteger deleteDirCounter = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(destinationDirPath)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> handleFileDeletion(file, deleteCounter, deleteDirCounter, threadId));
        } catch (IOException e) {
            log.error("Error walking directory: {}", destinationDirPath, e);
            throw e;
        }

        log.debug("Thread {} deleted {} files and {} directories", threadId, deleteCounter.get(), deleteDirCounter.get());
    }

    /**
     * Handle individual file deletion with retry.
     */
    private void handleFileDeletion(File file, AtomicInteger deleteCounter,
                                    AtomicInteger deleteDirCounter, long threadId) {
        // Skip properties files
        if (file.getName().endsWith(".properties")) {
            return;
        }

        if (file.isDirectory()) {
            String[] fileList = file.list();
            if (fileList != null && fileList.length == 0 && file.delete()) {
                deleteDirCounter.incrementAndGet();
            }
        } else {
            boolean fileDeleted = false;
            int retries = 0;

            while (!fileDeleted && retries < MAX_RETRIES) {
                try {
                    if (file.delete()) {
                        deleteCounter.incrementAndGet();
                        fileDeleted = true;
                    } else {
                        throw new IOException("File is in use or locked");
                    }
                } catch (IOException e) {
                    retries++;
                    log.warn("Thread {} failed to delete file: {}, retry {}/{}",
                            threadId, file.getName(), retries, MAX_RETRIES);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Update config file paths to thread-specific directory.
     */
    private void updateConfigFilePath(String destinationDirectory, long threadId) {
        String threadSpecificConfigFileName = destinationDirectory + "/config.properties";
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(threadSpecificConfigFileName)) {
            properties.load(fis);

            properties.setProperty("inputXml", separatorsToSystem(destinationDirectory + "/inputXML/"));
            properties.setProperty("transferOrderXmls", separatorsToSystem(destinationDirectory + "/inputXML/transferOrder/"));
            properties.setProperty("responseXmls", separatorsToSystem(destinationDirectory + "/responseXmls/"));
            properties.setProperty("apiTemplatesXmlPath", separatorsToSystem(destinationDirectory + "/apiTemplates/"));
            properties.setProperty("validationExcels", separatorsToSystem(destinationDirectory + "/excels/"));

            // Normalize path separators
            properties.forEach((key, value) -> {
                String updatedValue = value.toString().replace("\\", "/");
                properties.put(key, updatedValue);
            });

            try (FileOutputStream fos = new FileOutputStream(threadSpecificConfigFileName)) {
                properties.store(fos, "Updated by TestNGListenerNew for thread " + threadId);
            }

            log.debug("Config file updated for thread {}", threadId);

        } catch (IOException e) {
            log.error("Error updating config file path for thread {}", threadId, e);
        }
    }

    /**
     * Delete directory recursively.
     */
    private boolean deleteDirectoryRecursively(File directory) {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        try {
                            if (file.isDirectory()) {
                                String[] fileList = file.list();
                                if (fileList == null || fileList.length == 0) {
                                    file.delete();
                                }
                            } else {
                                file.delete();
                            }
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", file.getAbsolutePath());
                        }
                    });
            return true;
        } catch (IOException e) {
            log.error("Error walking directory: {}", directory.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Check if directories are identical (from OLD).
     */
    private boolean directoriesAreIdentical(File source, File destination) {
        File[] sourceFiles = source.listFiles();
        File[] destinationFiles = destination.listFiles();

        if (sourceFiles == null || destinationFiles == null) {
            return false;
        }

        if (sourceFiles.length != destinationFiles.length - 1) {
            return false;
        }

        for (File sourceFile : sourceFiles) {
            boolean foundMatch = false;
            for (File destinationFile : destinationFiles) {
                if (sourceFile.getName().equals(destinationFile.getName())) {
                    if (sourceFile.isDirectory()) {
                        if (!directoriesAreIdentical(sourceFile, destinationFile)) {
                            return false;
                        }
                    } else {
                        if (sourceFile.length() != destinationFile.length()) {
                            return false;
                        }
                    }
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }
        return true;
    }
}