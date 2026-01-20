package com.acuver.autwit.core.utils;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * CanonicalKeyGenerator - Generates and parses canonical keys for AUTWIT.
 *
 * <h2>PURPOSE</h2>
 * <p>Canonical keys uniquely identify an event expectation in the system.
 * They are used for:</p>
 * <ul>
 *   <li>Database lookups (finding paused contexts, matching events)</li>
 *   <li>Waiter registry (EventStepNotifier futures)</li>
 *   <li>Resume engine matching</li>
 * </ul>
 *
 * <h2>KEY FORMATS</h2>
 *
 * <h3>V2 Format (PREFERRED)</h3>
 * <pre>
 * scenarioName::orderId::eventType
 * Example: "Verify_Order_Ships::ORD-123::ORDER_SHIPPED"
 * </pre>
 * <p>V2 format includes scenario name, which prevents collisions when
 * multiple scenarios test the same orderId with the same eventType.</p>
 *
 * <h3>V1 Format (DEPRECATED)</h3>
 * <pre>
 * orderId::eventType
 * Example: "ORD-123::ORDER_SHIPPED"
 * </pre>
 * <p>V1 format is maintained for backward compatibility but should not
 * be used for new code. It can cause cross-scenario collisions.</p>
 *
 * <h3>FULL Format (EXTENDED)</h3>
 * <pre>
 * featureName::lineNumber::exampleId::orderId::eventType
 * Example: "OrderFeature::42::example-1::ORD-123::ORDER_SHIPPED"
 * </pre>
 * <p>Full format provides maximum disambiguation for complex scenarios.</p>
 *
 * <h2>MIGRATION</h2>
 * <p>Code should migrate from V1 to V2 format. During migration:</p>
 * <ul>
 *   <li>Use {@link #generate(String, String, String)} for V2 keys</li>
 *   <li>Use {@link #forOrder(String, String)} only for backward compatibility</li>
 *   <li>Use {@link #matches(String, String)} for cross-format comparison</li>
 *   <li>Use {@link #parse(String)} to extract components from any format</li>
 * </ul>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public final class CanonicalKeyGenerator {

    /** Separator between key components */
    public static final String SEPARATOR = "::";

    /** Pattern for V2 format detection */
    private static final Pattern V2_PATTERN = Pattern.compile("^[^:]+::[^:]+::[^:]+$");

    /** Pattern for FULL format detection */
    private static final Pattern FULL_PATTERN = Pattern.compile("^[^:]+::[^:]+::[^:]+::[^:]+::[^:]+$");

    private CanonicalKeyGenerator() {
        // Utility class - no instantiation
    }

    // =========================================================================
    // V2 KEY GENERATION (PREFERRED)
    // =========================================================================

    /**
     * Generate V2 canonical key.
     *
     * <p>This is the PREFERRED method for generating canonical keys.
     * V2 format includes scenario name to prevent cross-scenario collisions.</p>
     *
     * @param scenarioName Name of the scenario (required)
     * @param orderId Order ID (required)
     * @param eventType Event type (required)
     * @return V2 canonical key in format: scenarioName::orderId::eventType
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public static String generate(String scenarioName, String orderId, String eventType) {
        validateNotBlank(scenarioName, "scenarioName");
        validateNotBlank(orderId, "orderId");
        validateNotBlank(eventType, "eventType");

        return sanitize(scenarioName) + SEPARATOR + sanitize(orderId) + SEPARATOR + sanitize(eventType);
    }

    /**
     * Generate FULL canonical key with maximum disambiguation.
     *
     * @param featureName Feature file name
     * @param lineNumber Scenario line number
     * @param exampleId Example ID for parameterized scenarios
     * @param orderId Order ID
     * @param eventType Event type
     * @return FULL canonical key
     */
    public static String generateFull(String featureName, int lineNumber, String exampleId,
                                      String orderId, String eventType) {
        validateNotBlank(orderId, "orderId");
        validateNotBlank(eventType, "eventType");

        String feature = featureName != null ? sanitize(featureName) : "unknown";
        String line = String.valueOf(lineNumber);
        String example = exampleId != null ? sanitize(exampleId) : "default";

        return feature + SEPARATOR + line + SEPARATOR + example + SEPARATOR +
                sanitize(orderId) + SEPARATOR + sanitize(eventType);
    }

    // =========================================================================
    // V1 KEY GENERATION (DEPRECATED)
    // =========================================================================

    /**
     * Generate V1 canonical key.
     *
     * @deprecated Use {@link #generate(String, String, String)} instead.
     *             V1 format can cause cross-scenario collisions.
     *
     * @param orderId Order ID
     * @param eventType Event type
     * @return V1 canonical key in format: orderId::eventType
     */
    @Deprecated
    public static String forOrder(String orderId, String eventType) {
        validateNotBlank(orderId, "orderId");
        validateNotBlank(eventType, "eventType");

        return sanitize(orderId) + SEPARATOR + sanitize(eventType);
    }

    // =========================================================================
    // KEY PARSING
    // =========================================================================

    /**
     * Parse a canonical key into its components.
     *
     * <p>Automatically detects the format (V1, V2, or FULL) and extracts
     * the appropriate components.</p>
     *
     * @param canonicalKey The key to parse
     * @return KeyComponents record with extracted values
     * @throws IllegalArgumentException if key format is invalid
     */
    public static KeyComponents parse(String canonicalKey) {
        if (canonicalKey == null || canonicalKey.isBlank()) {
            throw new IllegalArgumentException("Canonical key cannot be null or blank");
        }

        String[] parts = canonicalKey.split(SEPARATOR);

        return switch (parts.length) {
            case 2 -> // V1 format: orderId::eventType
                    new KeyComponents(null, null, null, parts[0], parts[1], KeyFormat.V1);
            case 3 -> // V2 format: scenarioName::orderId::eventType
                    new KeyComponents(parts[0], null, null, parts[1], parts[2], KeyFormat.V2);
            case 5 -> // FULL format: feature::line::example::orderId::eventType
                    new KeyComponents(null, parts[0], parts[2], parts[3], parts[4], KeyFormat.FULL);
            default ->
                    throw new IllegalArgumentException("Invalid canonical key format: " + canonicalKey);
        };
    }

    // =========================================================================
    // FORMAT DETECTION
    // =========================================================================

    /**
     * Check if key is in V2 format.
     *
     * @param key The key to check
     * @return true if V2 format (scenarioName::orderId::eventType)
     */
    public static boolean isV2Format(String key) {
        if (key == null) return false;
        return V2_PATTERN.matcher(key).matches();
    }

    /**
     * Check if key is in V1 format.
     *
     * @param key The key to check
     * @return true if V1 format (orderId::eventType)
     */
    public static boolean isV1Format(String key) {
        if (key == null) return false;
        String[] parts = key.split(SEPARATOR);
        return parts.length == 2;
    }

    /**
     * Check if key is in FULL format.
     *
     * @param key The key to check
     * @return true if FULL format
     */
    public static boolean isFullFormat(String key) {
        if (key == null) return false;
        return FULL_PATTERN.matcher(key).matches();
    }

    /**
     * Get the format of a canonical key.
     *
     * @param key The key to analyze
     * @return KeyFormat enum value
     */
    public static KeyFormat getFormat(String key) {
        if (isFullFormat(key)) return KeyFormat.FULL;
        if (isV2Format(key)) return KeyFormat.V2;
        if (isV1Format(key)) return KeyFormat.V1;
        return KeyFormat.UNKNOWN;
    }

    // =========================================================================
    // CROSS-FORMAT MATCHING
    // =========================================================================

    /**
     * Check if two keys match, allowing for V1/V2 cross-compatibility.
     *
     * <p>Keys match if their orderId and eventType components are equal,
     * regardless of whether one is V1 and the other is V2.</p>
     *
     * @param key1 First key
     * @param key2 Second key
     * @return true if keys match (same orderId and eventType)
     */
    public static boolean matches(String key1, String key2) {
        if (key1 == null || key2 == null) return false;
        if (key1.equals(key2)) return true;

        try {
            KeyComponents c1 = parse(key1);
            KeyComponents c2 = parse(key2);

            return Objects.equals(c1.orderId(), c2.orderId()) &&
                    Objects.equals(c1.eventType(), c2.eventType());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Convert a V1 key to V2 format by adding scenario name.
     *
     * @param v1Key V1 format key
     * @param scenarioName Scenario name to add
     * @return V2 format key
     */
    public static String toV2(String v1Key, String scenarioName) {
        KeyComponents components = parse(v1Key);
        return generate(scenarioName, components.orderId(), components.eventType());
    }

    /**
     * Extract orderId from any key format.
     *
     * @param key The canonical key
     * @return Order ID component
     */
    public static String extractOrderId(String key) {
        return parse(key).orderId();
    }

    /**
     * Extract eventType from any key format.
     *
     * @param key The canonical key
     * @return Event type component
     */
    public static String extractEventType(String key) {
        return parse(key).eventType();
    }

    /**
     * Extract scenarioName from V2 or FULL key format.
     *
     * @param key The canonical key
     * @return Scenario name or null if V1 format
     */
    public static String extractScenarioName(String key) {
        return parse(key).scenarioName();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Sanitize a key component by replacing separator characters.
     */
    private static String sanitize(String value) {
        if (value == null) return "";
        // Replace any colons and spaces with underscores
        return value.replaceAll("[:\\s]", "_").trim();
    }

    /**
     * Validate that a value is not null or blank.
     */
    private static void validateNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    /**
     * Key format enumeration.
     */
    public enum KeyFormat {
        V1,      // orderId::eventType
        V2,      // scenarioName::orderId::eventType
        FULL,    // feature::line::example::orderId::eventType
        UNKNOWN
    }

    /**
     * Record holding parsed key components.
     */
    public record KeyComponents(
            String scenarioName,
            String featureName,
            String exampleId,
            String orderId,
            String eventType,
            KeyFormat format
    ) {
        /**
         * Check if this key has scenario name (V2 or FULL format).
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
         * Check if this is V2 format.
         */
        public boolean isV2() {
            return format == KeyFormat.V2;
        }

        /**
         * Convert to V2 key string.
         */
        public String toV2Key(String scenarioName) {
            String scenario = this.scenarioName != null ? this.scenarioName : scenarioName;
            if (scenario == null || scenario.isBlank()) {
                throw new IllegalArgumentException("Scenario name required for V2 key");
            }
            return generate(scenario, orderId, eventType);
        }

        /**
         * Convert to V1 key string.
         */
        public String toV1Key() {
            return forOrder(orderId, eventType);
        }
    }
}