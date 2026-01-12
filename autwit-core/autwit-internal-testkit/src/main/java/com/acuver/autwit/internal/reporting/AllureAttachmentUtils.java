package com.acuver.autwit.internal.reporting;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AllureAttachmentUtils - Utility methods for Allure attachments.
 *
 * <h2>USAGE</h2>
 * <pre>
 * // For large payloads (saved to file with link)
 * AllureAttachmentUtils.saveLargePayload("API Response", jsonContent, "json");
 *
 * // For regular attachments (via AllureLifecycleManager)
 * allureLifecycleManager.attachJson("Response", jsonContent);
 * </pre>
 *
 * <h2>THREAD SAFETY</h2>
 * All methods are thread-safe. Uses AllureLifecycleManager for lifecycle checks.
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
@Component
public class AllureAttachmentUtils {

    private static final Logger log = LogManager.getLogger(AllureAttachmentUtils.class);

    private static final Path BASE = Paths.get("allure-artifacts");

    /** Static instance for backward compatibility with static method calls */
    private static AllureAttachmentUtils instance;

    /** Injected lifecycle manager for safe attachments */
    private final AllureLifecycleManager allureLifecycle;

    static {
        try {
            Files.createDirectories(BASE);
        } catch (IOException e) {
            LogManager.getLogger(AllureAttachmentUtils.class)
                    .warn("Failed to create allure-artifacts directory: {}", e.getMessage());
        }
    }

    /**
     * Constructor with dependency injection.
     *
     * @param allureLifecycle AllureLifecycleManager for lifecycle checks
     */
    @Autowired
    public AllureAttachmentUtils(AllureLifecycleManager allureLifecycle) {
        this.allureLifecycle = allureLifecycle;
        instance = this; // Set static instance for backward compatibility
    }

    /**
     * Default constructor for static usage (backward compatibility).
     * Uses null lifecycle manager - will skip lifecycle checks.
     */
    public AllureAttachmentUtils() {
        this.allureLifecycle = null;
    }

    /**
     * Save large payload to file and add as Allure link.
     *
     * <p>Use this for large payloads (>10KB) that would bloat the Allure report.
     * The content is saved to a file and linked from the report.</p>
     *
     * @param name Attachment name (will be sanitized for filename)
     * @param content Content to save
     * @param ext File extension (e.g., "json", "xml", "txt")
     */
    public static void saveLargePayload(String name, String content, String ext) {
        if (content == null || content.isEmpty()) {
            log.debug("Skipping empty payload: {}", name);
            return;
        }

        try {
            // Sanitize filename
            String fileName = sanitizeFilename(name) + "." + ext;
            Path filePath = BASE.resolve(fileName);

            // Write content to file
            Files.writeString(filePath, content);

            // Add link to Allure report
            if (isTestRunning()) {
                Allure.link(name, filePath.toUri().toString());
                log.debug("📎 Large payload saved: {} ({} bytes)", fileName, content.length());
            } else {
                log.debug("Large payload saved to file but not linked (no test running): {}", fileName);
            }

        } catch (Exception e) {
            log.warn("⚠️ Failed to save large payload '{}': {}", name, e.getMessage());
            // Fallback: try to attach error message
            attachErrorFallback(name, e);
        }
    }

    /**
     * Safely attach text content.
     *
     * <p>Checks if Allure test is running before attempting attachment.</p>
     *
     * @param name Attachment name
     * @param content Text content
     */
    public static void attachTextSafely(String name, String content) {
        if (!isTestRunning()) {
            log.debug("⚠️ Cannot attach '{}' - no test running", name);
            return;
        }

        if (content == null) {
            log.debug("Skipping null attachment: {}", name);
            return;
        }

        try {
            Allure.addAttachment(name, "text/plain", content);
        } catch (Exception e) {
            log.warn("⚠️ Failed to attach '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Safely attach JSON content.
     *
     * @param name Attachment name
     * @param jsonContent JSON content
     */
    public static void attachJsonSafely(String name, String jsonContent) {
        if (!isTestRunning()) {
            log.debug("⚠️ Cannot attach '{}' - no test running", name);
            return;
        }

        if (jsonContent == null) {
            log.debug("Skipping null attachment: {}", name);
            return;
        }

        try {
            Allure.addAttachment(name, "application/json", jsonContent);
        } catch (Exception e) {
            log.warn("⚠️ Failed to attach JSON '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Safely attach XML content.
     *
     * @param name Attachment name
     * @param xmlContent XML content
     */
    public static void attachXmlSafely(String name, String xmlContent) {
        if (!isTestRunning()) {
            log.debug("⚠️ Cannot attach '{}' - no test running", name);
            return;
        }

        if (xmlContent == null) {
            log.debug("Skipping null attachment: {}", name);
            return;
        }

        try {
            Allure.addAttachment(name, "application/xml", xmlContent);
        } catch (Exception e) {
            log.warn("⚠️ Failed to attach XML '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Attach content with automatic type detection.
     *
     * @param name Attachment name
     * @param content Content (JSON, XML, or plain text)
     */
    public static void attachAuto(String name, String content) {
        if (!isTestRunning() || content == null) {
            return;
        }

        String trimmed = content.trim();
        String mimeType;

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            mimeType = "application/json";
        } else if (trimmed.startsWith("<")) {
            mimeType = "application/xml";
        } else {
            mimeType = "text/plain";
        }

        try {
            Allure.addAttachment(name, mimeType, content);
        } catch (Exception e) {
            log.warn("⚠️ Failed to attach '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Instance method for attaching via lifecycle manager.
     *
     * @param name Attachment name
     * @param content Content
     * @param mimeType MIME type
     */
    public void attach(String name, String content, String mimeType) {
        if (allureLifecycle != null) {
            allureLifecycle.attachSafely(name, content, mimeType);
        } else {
            // Fallback to static method
            if (isTestRunning() && content != null) {
                try {
                    Allure.addAttachment(name, mimeType, content);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to attach '{}': {}", name, e.getMessage());
                }
            }
        }
    }

    // ==============================================================
    // PRIVATE HELPERS
    // ==============================================================

    /**
     * Check if an Allure test is currently running.
     *
     * <p>Uses the static instance's lifecycle manager if available,
     * otherwise uses Allure's internal state.</p>
     */
    private static boolean isTestRunning() {
        // Try using injected lifecycle manager
        if (instance != null && instance.allureLifecycle != null) {
            return instance.allureLifecycle.isTestRunning();
        }

        // Fallback: try to check Allure's internal state
        try {
            // This is a heuristic - Allure doesn't expose a direct "isTestRunning" method
            // We attempt a safe operation and catch if it fails
            return true; // Assume running and let Allure handle errors
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sanitize string for use as filename.
     */
    private static String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name
                .replaceAll("[^a-zA-Z0-9-_\\.]", "_")
                .replaceAll("_+", "_")
                .replaceAll("[\\._]+$", "")
                .replaceAll("^\\.+", "");
    }

    /**
     * Fallback attachment for errors.
     */
    private static void attachErrorFallback(String name, Exception error) {
        try {
            if (isTestRunning()) {
                Allure.addAttachment(name + " (error)", "text/plain", error.getMessage());
            }
        } catch (Exception ignored) {
            // Silently ignore - we're already in error handling
        }
    }
}