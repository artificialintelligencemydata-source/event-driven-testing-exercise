package com.acuver.autwit.core.utils;

import java.util.Objects;

/**
 * CanonicalKeyGenerator - Generates unique keys for event matching and scenario identification.
 *
 * <h2>KEY FORMATS</h2>
 * <ul>
 *   <li><b>V2 (Recommended)</b>: {@code scenarioName::orderId::eventType}</li>
 *   <li><b>V1 (Deprecated)</b>: {@code orderId::eventType}</li>
 * </ul>
 *
 * <h2>WHY SCENARIO NAME IS NEEDED</h2>
 * <p>Without scenario name, two different scenarios testing the same orderId could collide:</p>
 * <pre>
 * Scenario A: expects ORDER_CREATED for order123
 * Scenario B: expects ORDER_CREATED for order123 (different test case)
 *
 * V1 key for both: order123::ORDER_CREATED (COLLISION!)
 * V2 key: ScenarioA::order123::ORDER_CREATED vs ScenarioB::order123::ORDER_CREATED (UNIQUE!)
 * </pre>
 *
 * <h2>BACKWARD COMPATIBILITY</h2>
 * <p>V1 format is still supported for reading existing data. New data should use V2.</p>
 *
 * @author AUTWIT Framework
 * @since 1.0.0
 */
public final class CanonicalKeyGenerator {

    /** Delimiter between key components */
    public static final String DELIMITER = "::";

    /** Maximum key length to prevent excessive storage */
    private static final int MAX_KEY_LENGTH = 500;

    private CanonicalKeyGenerator() {
        // Utility class - no instantiation
    }

    // =========================================================================
    // V2 FORMAT (RECOMMENDED)
    // =========================================================================

    /**
     * Generate canonical key WITH scenario name (V2 format - recommended).
     *
     * <p>Format: {@code scenarioName::orderId::eventType}</p>
     *
     * <p>This format ensures uniqueness across different scenarios testing the same order.</p>
     *
     * @param scenarioName Scenario name (from Cucumber)
     * @param orderId Business correlation ID
     * @param eventType Expected event type
     * @return Canonical key in V2 format
     */
    public static String generate(String scenarioName, String orderId, String eventType) {
        // Validate inputs
        if (isBlank(orderId)) {
            throw new IllegalArgumentException("orderId cannot be null or blank");
        }
        if (isBlank(eventType)) {
            throw new IllegalArgumentException("eventType cannot be null or blank");
        }

        // If no scenario name, fall back to V1
        if (isBlank(scenarioName)) {
            return forOrder(orderId, eventType);
        }

        String key = String.join(DELIMITER,
                sanitize(scenarioName),
                sanitize(orderId),
                sanitize(eventType)
        );

        return truncateIfNeeded(key);
    }

    /**
     * Generate canonical key with full context.
     *
     * <p>Format: {@code featureName::scenarioLine::exampleId::orderId::eventType}</p>
     *
     * <p>Use this for maximum uniqueness in complex test suites.</p>
     *
     * @param featureName Feature file name
     * @param scenarioLine Scenario line number
     * @param exampleId Example ID (for Scenario Outline)
     * @param orderId Business correlation ID
     * @param eventType Expected event type
     * @return Canonical key with full context
     */
    public static String generateFull(String featureName, int scenarioLine, String exampleId,
                                      String orderId, String eventType) {
        String key = String.join(DELIMITER,
                sanitize(featureName),
                String.valueOf(scenarioLine),
                sanitize(exampleId),
                sanitize(orderId),
                sanitize(eventType)
        );

        return truncateIfNeeded(key);
    }

    // =========================================================================
    // V1 FORMAT (DEPRECATED - FOR BACKWARD COMPATIBILITY)
    // =========================================================================

    /**
     * Generate canonical key WITHOUT scenario name (V1 format).
     *
     * <p>Format: {@code orderId::eventType}</p>
     *
     * <p><b>WARNING:</b> This format can cause collisions if multiple scenarios
     * test the same order. Use {@link #generate(String, String, String)} instead.</p>
     *
     * @param orderId Business correlation ID
     * @param eventType Expected event type
     * @return Canonical key in V1 format
     * @deprecated Use {@link #generate(String, String, String)} instead
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    public static String forOrder(String orderId, String eventType) {
        if (isBlank(orderId)) {
            throw new IllegalArgumentException("orderId cannot be null or blank");
        }
        if (isBlank(eventType)) {
            throw new IllegalArgumentException("eventType cannot be null or blank");
        }

        return sanitize(orderId) + DELIMITER + sanitize(eventType);
    }

    // =========================================================================
    // PARSING
    // =========================================================================

    /**
     * Parse a canonical key into its components.
     *
     * <p>Automatically detects V1 vs V2 format based on number of components.</p>
     *
     * @param canonicalKey Key to parse
     * @return Parsed components, or null if invalid
     */
    public static KeyComponents parse(String canonicalKey) {
        if (isBlank(canonicalKey)) {
            return null;
        }

        String[] parts = canonicalKey.split(DELIMITER);

        return switch (parts.length) {
            case 2 -> // V1 format: orderId::eventType
                    new KeyComponents(null, parts[0], parts[1], KeyFormat.V1);
            case 3 -> // V2 format: scenarioName::orderId::eventType
                    new KeyComponents(parts[0], parts[1], parts[2], KeyFormat.V2);
            case 5 -> // Full format: featureName::line::exampleId::orderId::eventType
                    new KeyComponents(parts[0] + "_" + parts[2], parts[3], parts[4], KeyFormat.FULL);
            default -> null;
        };
    }

    /**
     * Check if a key is in V2 format (has scenario name).
     *
     * @param canonicalKey Key to check
     * @return true if V2 format
     */
    public static boolean isV2Format(String canonicalKey) {
        KeyComponents parsed = parse(canonicalKey);
        return parsed != null && parsed.format() != KeyFormat.V1;
    }

    /**
     * Extract orderId from a canonical key.
     *
     * @param canonicalKey Key to parse
     * @return orderId, or null if parsing fails
     */
    public static String extractOrderId(String canonicalKey) {
        KeyComponents parsed = parse(canonicalKey);
        return parsed != null ? parsed.orderId() : null;
    }

    /**
     * Extract eventType from a canonical key.
     *
     * @param canonicalKey Key to parse
     * @return eventType, or null if parsing fails
     */
    public static String extractEventType(String canonicalKey) {
        KeyComponents parsed = parse(canonicalKey);
        return parsed != null ? parsed.eventType() : null;
    }

    /**
     * Extract scenarioName from a canonical key.
     *
     * @param canonicalKey Key to parse
     * @return scenarioName, or null if V1 format or parsing fails
     */
    public static String extractScenarioName(String canonicalKey) {
        KeyComponents parsed = parse(canonicalKey);
        return parsed != null ? parsed.scenarioName() : null;
    }

    // =========================================================================
    // MATCHING
    // =========================================================================

    /**
     * Check if two keys match for resume purposes.
     *
     * <p>Handles V1 to V2 migration by comparing orderId + eventType components.</p>
     *
     * @param key1 First key
     * @param key2 Second key
     * @return true if keys match
     */
    public static boolean matches(String key1, String key2) {
        if (Objects.equals(key1, key2)) {
            return true;
        }

        KeyComponents c1 = parse(key1);
        KeyComponents c2 = parse(key2);

        if (c1 == null || c2 == null) {
            return false;
        }

        // Both must have same orderId and eventType
        if (!Objects.equals(c1.orderId(), c2.orderId()) ||
                !Objects.equals(c1.eventType(), c2.eventType())) {
            return false;
        }

        // If both have scenario names, they must match
        if (c1.hasScenarioName() && c2.hasScenarioName()) {
            return Objects.equals(c1.scenarioName(), c2.scenarioName());
        }

        // One or both are V1 format - match on orderId + eventType only
        return true;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Sanitize a string for use in canonical key.
     *
     * <p>Replaces delimiter characters and trims whitespace.</p>
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "null";
        }

        return value.trim()
                .replace(DELIMITER, "_")  // Escape delimiter
                .replaceAll("\\s+", "_")  // Replace whitespace
                .replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");  // Keep safe chars only
    }

    /**
     * Check if a string is null or blank.
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Truncate key if it exceeds maximum length.
     */
    private static String truncateIfNeeded(String key) {
        if (key.length() <= MAX_KEY_LENGTH) {
            return key;
        }

        // Truncate but keep a hash suffix for uniqueness
        int hash = key.hashCode();
        String suffix = "_" + Integer.toHexString(hash);
        return key.substring(0, MAX_KEY_LENGTH - suffix.length()) + suffix;
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Key format version.
     */
    public enum KeyFormat {
        /** orderId::eventType */
        V1,
        /** scenarioName::orderId::eventType */
        V2,
        /** featureName::line::exampleId::orderId::eventType */
        FULL
    }

    /**
     * Parsed components of a canonical key.
     *
     * @param scenarioName Scenario name (null for V1 format)
     * @param orderId Business correlation ID
     * @param eventType Event type
     * @param format Key format version
     */
    public record KeyComponents(
            String scenarioName,
            String orderId,
            String eventType,
            KeyFormat format
    ) {
        /**
         * Check if this key has a scenario name (V2 or FULL format).
         */
        public boolean hasScenarioName() {
            return scenarioName != null && !scenarioName.isBlank();
        }

        /**
         * Check if this is V1 format.
         */
        public boolean isV1() {
            return format == KeyFormat.V1;
        }

        /**
         * Convert to V2 format key string.
         */
        public String toV2Key(String newScenarioName) {
            return CanonicalKeyGenerator.generate(newScenarioName, orderId, eventType);
        }
    }
}