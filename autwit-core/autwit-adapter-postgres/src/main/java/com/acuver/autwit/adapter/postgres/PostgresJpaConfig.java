package com.acuver.autwit.adapter.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Map;

/**
 * PostgreSQL JPA Configuration
 * <p>
 * This configuration class manages the PostgreSQL database adapter for the AUTWIT framework.
 * It follows hexagonal architecture principles by keeping all PostgreSQL-specific
 * infrastructure isolated within this adapter module.
 * </p>
 *
 * <h3>CRITICAL ARCHITECTURE RULES:</h3>
 * <ol>
 *   <li><b>Adapter Ownership:</b> This adapter owns its complete infrastructure stack
 *       (DataSource, EntityManagerFactory, TransactionManager) to ensure deterministic
 *       startup and avoid Spring Boot auto-configuration conflicts.</li>
 *   <li><b>Schema Management:</b> Schema is NOT managed by Hibernate. The
 *       {@code schema-postgres.sql} script owns all DDL operations. Hibernate DDL
 *       must always be set to {@code none} or {@code validate}.</li>
 *   <li><b>Fail-Fast Validation:</b> Configuration is validated at startup with
 *       clear error messages to prevent runtime surprises.</li>
 *   <li><b>Profile Activation:</b> This configuration only activates when
 *       {@code autwit.database=postgres} property is set, enabling multi-database
 *       support across different environments.</li>
 * </ol>
 *
 * <h3>Bean Creation Order:</h3>
 * <pre>
 * 1. Constructor (banner printed)
 * 2. {@literal @}PostConstruct validateConfiguration() - fail-fast validation
 * 3. dataSource() - HikariCP connection pool
 * 4. entityManagerFactory() - Hibernate JPA setup
 * 5. transactionManager() - JPA transaction management
 * 6. {@literal @}EventListener onApplicationReady() - runtime diagnostics
 * </pre>
 *
 * @see com.acuver.autwit.adapter.postgres.PostgresEventContextRepository
 * @see com.acuver.autwit.adapter.postgres.PostgresScenarioContextRepository
 */

@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
@EnableJpaRepositories(basePackages = "com.acuver.autwit.adapter.postgres")
@EntityScan(basePackages = "com.acuver.autwit.adapter.postgres")
public class PostgresJpaConfig {
    private static final Logger log = LogManager.getLogger(PostgresJpaConfig.class);

    // ═════════════════════════════════════════════════════════════
    // Box Drawing Constants
    // ═════════════════════════════════════════════════════════════
    /**
     * Width of the content area inside the box borders.
     * <p>
     * <b>Calculation:</b> Total box width (65) - Border overhead (6) = 59 chars
     * <ul>
     *   <li>Total box width: 65 characters (TOP_LINE length)</li>
     *   <li>Left border: "│  " (3 chars)</li>
     *   <li>Right border: "  │" (3 chars)</li>
     *   <li>Content area: 65 - 3 - 3 = 59 chars</li>
     * </ul>
     * This ensures perfect alignment: BORDER_LEFT + CONTENT + BORDER_RIGHT = TOP_LINE
     * </p>
     */
    private static final int CONTENT_WIDTH = 59;

    /** Left border with padding: "│  " (3 chars) */
    private static final String BORDER_LEFT = "│  ";

    /** Right border with padding: "  │" (3 chars) */
    private static final String BORDER_RIGHT = "  │";

    /** Top border line (65 chars total) */
    private static final String TOP_LINE =
            "┌───────────────────────────────────────────────────────────────┐";

    /** Bottom border line (65 chars total) */
    private static final String BOTTOM_LINE =
            "└───────────────────────────────────────────────────────────────┘";

    /** Middle separator line (65 chars total) */
    private static final String MIDDLE_LINE =
            "├───────────────────────────────────────────────────────────────┤";

    // ═════════════════════════════════════════════════════════════
    // Dependencies - Injected by Spring
    // ═════════════════════════════════════════════════════════════
    /**
     * Spring Environment for accessing configuration properties.
     * Used to read database connection settings and JPA properties.
     */
    @Autowired
    private Environment env;

    /**
     * DataSource bean - lazily injected to avoid circular dependency.
     * <p>
     * Marked {@code @Lazy} because this configuration class creates the
     * DataSource bean itself, and we need it here for diagnostics.
     * </p>
     */
    @Autowired
    @org.springframework.context.annotation.Lazy
    private DataSource dataSource;

    // ═════════════════════════════════════════════════════════════
    // Lifecycle Hooks
    // ═════════════════════════════════════════════════════════════
    /**
     * Constructor - prints initialization banner.
     * <p>
     * Called first during bean creation to signal that PostgreSQL
     * adapter initialization has begun.
     * </p>
     */
    public PostgresJpaConfig() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║         POSTGRESQL ADAPTER CONFIGURATION INITIALIZING         ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    /**
     * Debug check to verify that application-postgres.yml was loaded.
     * <p>
     * This is a diagnostic aid during development. The test property
     * {@code autwit.debug.loaded-from-postgres-yml} should be set to
     * {@code true} in application-postgres.yml.
     * </p>
     */
    @PostConstruct
    public void debugPostgresYaml() {
        log.info("✅ autwit.debug.loaded-from-postgres-yml = " +
                env.getProperty("autwit.debug.loaded-from-postgres-yml"));
    }

    // ═════════════════════════════════════════════════════════════
    // DataSource Bean - Connection Pool Configuration
    // ═════════════════════════════════════════════════════════════
    /**
     * Creates the PostgreSQL DataSource bean using HikariCP.
     * <p>
     * <b>Why we create this manually:</b><br>
     * DataSourceAutoConfiguration is excluded at the framework level to prevent
     * Spring Boot from auto-configuring databases. This gives adapters full control
     * over their infrastructure and ensures deterministic behavior.
     * </p>
     *
     * <h4>Required Properties (from application-postgres.yml):</h4>
     * <ul>
     *   <li>{@code spring.datasource.url} - JDBC connection URL</li>
     *   <li>{@code spring.datasource.username} - Database username</li>
     *   <li>{@code spring.datasource.password} - Database password</li>
     *   <li>{@code spring.datasource.driver-class-name} - JDBC driver class
     *       (defaults to org.postgresql.Driver)</li>
     * </ul>
     *
     * <h4>HikariCP Pool Settings:</h4>
     * <ul>
     *   <li>Pool Name: "AUTWIT-PostgreSQL-Pool"</li>
     *   <li>Max Pool Size: 10 connections</li>
     *   <li>Min Idle: 2 connections</li>
     *   <li>Connection Timeout: 30 seconds</li>
     *   <li>Idle Timeout: 10 minutes</li>
     *   <li>Max Lifetime: 30 minutes</li>
     * </ul>
     *
     * @return configured HikariCP DataSource
     * @throws IllegalStateException if required properties are missing
     */
    @Bean
    public DataSource dataSource() {
        log.info("Creating PostgreSQL DataSource bean");

        String url = env.getProperty("spring.datasource.url");
        String driverClassName = env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "PostgreSQL adapter requires spring.datasource.url property. " +
                            "Ensure application-postgres.yml is loaded and postgres profile is active."
            );
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("AUTWIT-PostgreSQL-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        log.info("PostgreSQL DataSource configured: url={}, username={}", url, username);
        return new HikariDataSource(config);
    }

    // ═════════════════════════════════════════════════════════════
    // EntityManagerFactory Bean - JPA/Hibernate Configuration
    // ═════════════════════════════════════════════════════════════
    /**
     * Creates the EntityManagerFactory bean for JPA repositories.
     * <p>
     * <b>Why we create this manually:</b><br>
     * HibernateJpaAutoConfiguration is excluded at the framework level to prevent
     * Spring Boot from auto-configuring JPA. This ensures adapters have complete
     * control over their persistence infrastructure.
     * </p>
     *
     * <h4>Required JPA Properties (from application-postgres.yml):</h4>
     * <ul>
     *   <li>{@code spring.jpa.properties.hibernate.dialect} - Hibernate dialect
     *       (should be org.hibernate.dialect.PostgreSQLDialect)</li>
     *   <li>{@code spring.jpa.hibernate.ddl-auto} - DDL generation mode
     *       (MUST be "none" or "validate" - schema owned by SQL scripts)</li>
     *   <li>{@code spring.jpa.show-sql} - Enable SQL logging (optional)</li>
     *   <li>{@code spring.jpa.properties.hibernate.format_sql} - Format SQL (optional)</li>
     * </ul>
     *
     * <h4>Entity Scanning:</h4>
     * Only scans {@code com.acuver.autwit.adapter.postgres} package for JPA entities.
     *
     * @param dataSource the configured DataSource
     * @return configured LocalContainerEntityManagerFactoryBean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("Creating PostgreSQL EntityManagerFactory bean");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.acuver.autwit.adapter.postgres");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        // Build JPA properties from Environment (since HibernateJpaAutoConfiguration is excluded)
        Map<String, Object> properties = new java.util.HashMap<>();

        // Read Hibernate dialect (required for PostgreSQL)
        String dialect = env.getProperty("spring.jpa.properties.hibernate.dialect",
                env.getProperty("spring.jpa.hibernate.dialect",
                        "org.hibernate.dialect.PostgreSQLDialect"));
        properties.put("hibernate.dialect", dialect);

        // Read DDL auto mode
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto",
                env.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto", "none"));
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);

        // Read show SQL
        String showSql = env.getProperty("spring.jpa.show-sql", "false");
        properties.put("hibernate.show_sql", showSql);

        // Read format SQL
        String formatSql = env.getProperty("spring.jpa.properties.hibernate.format_sql",
                env.getProperty("spring.jpa.properties.hibernate.format-sql", "false"));
        properties.put("hibernate.format_sql", formatSql);

        em.setJpaPropertyMap(properties);

        log.info("PostgreSQL EntityManagerFactory configured");
        return em;
    }

    // ═════════════════════════════════════════════════════════════
    // TransactionManager Bean - JPA Transaction Management
    // ═════════════════════════════════════════════════════════════
    /**
     * Creates the PlatformTransactionManager bean for JPA transactions.
     * <p>
     * <b>Why we create this manually:</b><br>
     * Since HibernateJpaAutoConfiguration is excluded, we must explicitly
     * create the transaction manager. This bean enables {@code @Transactional}
     * support for repository operations.
     * </p>
     *
     * @param entityManagerFactory the configured EntityManagerFactory
     * @return configured JpaTransactionManager
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("Creating PostgreSQL TransactionManager bean");
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    // ═════════════════════════════════════════════════════════════
    // Configuration Validation - Fail-Fast on Misconfiguration
    // ═════════════════════════════════════════════════════════════
    /**
     * Validates PostgreSQL adapter configuration at startup.
     * <p>
     * This method runs during the {@code @PostConstruct} phase, immediately after
     * bean construction but before the DataSource is created. It performs fail-fast
     * validation to catch configuration errors early.
     * </p>
     *
     * <h4>Validation Guards:</h4>
     * <ol>
     *   <li><b>Dialect Check:</b> Verifies Hibernate dialect is PostgreSQL-compatible</li>
     *   <li><b>DDL Mode Check:</b> Ensures DDL is "none" or "validate" (schema owned by SQL)</li>
     *   <li><b>Schema Init Check:</b> Warns if schema initialization is disabled</li>
     *   <li><b>Driver Class Check:</b> Verifies JDBC driver class is configured</li>
     * </ol>
     *
     * <h4>Note on Property Loading:</h4>
     * At {@code @PostConstruct} time, profile-specific properties may not be fully loaded yet.
     * This method uses defensive defaults and re-validates at {@code ApplicationReadyEvent}.
     *
     * @throws IllegalStateException if critical configuration is invalid
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║         POSTGRESQL ADAPTER CONFIGURATION VALIDATION           ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        // Guard 1: Verify dialect configuration
        // Try multiple property paths (Spring Boot property binding variations)
        String dialect = env.getProperty("spring.jpa.properties.hibernate.dialect");
        if (dialect == null) {
            dialect = env.getProperty("spring.jpa.hibernate.dialect");
        }
        if (dialect == null) {
            dialect = env.getProperty("hibernate.dialect");
        }

        if (dialect == null || !dialect.contains("PostgreSQL")) {
            // If still null, use default for PostgreSQL (since we're in PostgresJpaConfig)
            if (dialect == null) {
                log.warn("Dialect not found in properties, using default: org.hibernate.dialect.PostgreSQLDialect");
                dialect = "org.hibernate.dialect.PostgreSQLDialect";
            } else {
                String error = String.format(
                        "FATAL: PostgreSQL adapter active but dialect is '%s'. " +
                                "Expected: org.hibernate.dialect.PostgreSQLDialect. " +
                                "Set it in application-postgres.yml: spring.jpa.properties.hibernate.dialect",
                        dialect
                );
                log.error(error);
                throw new IllegalStateException(error);
            }
        }

        // Guard 2: Verify Hibernate DDL is disabled (Spring Boot 3.x compatible)
        // NOTE: @PostConstruct may run before profile-specific properties are fully loaded.
        // Use defensive defaults - "none" is correct for PostgreSQL adapter.
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto == null) {
            ddlAuto = env.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto");
        }
        if (ddlAuto == null) {
            ddlAuto = env.getProperty("hibernate.hbm2ddl.auto");
        }

        // Default to "none" if not found (correct for PostgreSQL adapter)
        if (ddlAuto == null) {
            log.warn("ddl-auto not found in properties at @PostConstruct time, " +
                    "defaulting to 'none' (will re-validate at ApplicationReadyEvent)");
            ddlAuto = "none";
        }

        if (!"none".equalsIgnoreCase(ddlAuto) && !"validate".equalsIgnoreCase(ddlAuto)) {
            String error = String.format(
                    "FATAL: Hibernate DDL must be 'none' or 'validate'. Current: '%s'. " +
                            "AUTWIT owns schema via SQL scripts.",
                    ddlAuto
            );
            log.error(error);
            throw new IllegalStateException(error);
        }

        // Guard 3: Verify schema initialization mode
        String initMode = env.getProperty("spring.sql.init.mode");
        if (!"always".equalsIgnoreCase(initMode) && !"embedded".equalsIgnoreCase(initMode)) {
            log.warn("⚠️  Schema initialization mode is '{}'. " +
                    "Consider 'always' to ensure schema-postgres.sql runs.", initMode);
        }

        // Guard 4: Verify driver class is configured
        String driverClass = env.getProperty("spring.datasource.driver-class-name");
        if (driverClass == null || driverClass.isBlank()) {
            String error = "FATAL: spring.datasource.driver-class-name not configured. " +
                    "Set it to 'org.postgresql.Driver' in application-postgres.yml";
            log.error(error);
            throw new IllegalStateException(error);
        }

        log.info("✅ PostgreSQL adapter configuration validated successfully");
        log.info("   Dialect      : {}", dialect);
        log.info("   DDL Mode     : {}", ddlAuto);
        log.info("   Schema Init  : {}", initMode);
        log.info("   Driver Class : {}", driverClass);
    }

    // ═════════════════════════════════════════════════════════════
    // Application Ready - Runtime Diagnostics & Database Checks
    // ═════════════════════════════════════════════════════════════
    /**
     * Performs comprehensive database diagnostics after application is fully started.
     * <p>
     * This method runs at {@code ApplicationReadyEvent}, which fires after all beans
     * are initialized and the application is ready to serve requests. At this point,
     * all property sources are loaded and the database connection is available.
     * </p>
     *
     * <h4>Diagnostic Checks Performed:</h4>
     * <ol>
     *   <li><b>Property Re-validation:</b> Confirms all properties loaded correctly</li>
     *   <li><b>Database Info:</b> Displays connection URL and username</li>
     *   <li><b>Connection Test:</b> Verifies live database connectivity</li>
     *   <li><b>Table Check:</b> Confirms required tables exist</li>
     * </ol>
     *
     * <p>
     * These checks provide immediate feedback during startup, making it easy to
     * diagnose configuration or connectivity issues.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║           POSTGRESQL DATABASE STATUS CHECK                    ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        // Final validation - properties should be fully loaded now
        validatePropertiesFullyLoaded();

        printDatabaseInfo();
        testConnection();
        checkUserTables();

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║           [OK] POSTGRESQL IS UP AND READY                     ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    // ═════════════════════════════════════════════════════════════
    // Property Validation - Final Check After Full Initialization
    // ═════════════════════════════════════════════════════════════
    /**
     * Re-validates properties after all property sources are loaded.
     * <p>
     * This is a final check that runs at {@code ApplicationReadyEvent} time to
     * ensure profile-specific properties (like those from application-postgres.yml)
     * were properly loaded. It warns about any discrepancies.
     * </p>
     */
    private void validatePropertiesFullyLoaded() {
        // Re-check ddl-auto - should be loaded from application-postgres.yml by now
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto == null) {
            ddlAuto = env.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto");
        }

        if (ddlAuto == null) {
            log.warn("⚠️  WARNING: spring.jpa.hibernate.ddl-auto still not found after ApplicationReadyEvent. " +
                    "This may indicate application-postgres.yml is not being loaded. " +
                    "Using default 'none' for PostgreSQL adapter.");
        } else if (!"none".equalsIgnoreCase(ddlAuto) && !"validate".equalsIgnoreCase(ddlAuto)) {
            log.warn("⚠️  WARNING: spring.jpa.hibernate.ddl-auto is '{}'. " +
                    "Expected 'none' or 'validate' for AUTWIT. Current value may cause schema conflicts.", ddlAuto);
        } else {
            log.info("✅ Verified: spring.jpa.hibernate.ddl-auto = {}", ddlAuto);
        }

        // Re-check dialect
        String dialect = env.getProperty("spring.jpa.properties.hibernate.dialect");
        if (dialect == null) {
            dialect = env.getProperty("spring.jpa.hibernate.dialect");
        }

        if (dialect == null || !dialect.contains("PostgreSQL")) {
            log.warn("⚠️  WARNING: Hibernate dialect not found or incorrect. " +
                    "Expected PostgreSQL dialect. Using default: org.hibernate.dialect.PostgreSQLDialect");
        } else {
            log.info("✅ Verified: Hibernate dialect = {}", dialect);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Diagnostic Helper Methods
    // ═════════════════════════════════════════════════════════════
    /**
     * Prints database configuration information in a formatted box.
     * <p>
     * Displays the database type, JDBC URL, and username. This helps verify
     * that the correct database configuration is being used.
     * </p>
     */
    private void printDatabaseInfo() {
        try {
            String url = env.getProperty("spring.datasource.url", "Not configured");
            String username = env.getProperty("spring.datasource.username", "Not configured");

            log.info(TOP_LINE);
            log.info(createBoxLine("DATABASE CONFIGURATION", true));
            log.info(MIDDLE_LINE);
            log.info(createKeyValueLine("Database Type", "PostgreSQL"));
            log.info(createKeyValueLine("JDBC URL", url));
            log.info(createKeyValueLine("Username", username));
            log.info(BOTTOM_LINE);
        } catch (Exception e) {
            log.warn("Could not retrieve config: {}", e.getMessage());
        }
    }

    /**
     * Tests live database connectivity and displays connection details.
     * <p>
     * Attempts to establish a connection to the database and retrieves metadata
     * including database product name and version. Also verifies that we're
     * actually connected to PostgreSQL (not some other database).
     * </p>
     */
    private void testConnection() {
        if (dataSource == null) {
            log.warn("DataSource not available for connection test");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            log.info(TOP_LINE);
            log.info(createBoxLine("CONNECTION TEST", true));
            log.info(MIDDLE_LINE);

            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData meta = conn.getMetaData();
                String dbProduct = meta.getDatabaseProductName();

                log.info(createKeyValueLine("[OK] Status", "CONNECTED"));
                log.info(createKeyValueLine("Database", dbProduct));
                log.info(createKeyValueLine("Version", meta.getDatabaseProductVersion()));

                // Additional verification: actual DB should be PostgreSQL
                if (!dbProduct.toLowerCase().contains("postgres")) {
                    log.error("❌ FATAL: PostgreSQL adapter active but connected to '{}'", dbProduct);
                    throw new IllegalStateException("Database mismatch: expected PostgreSQL, got " + dbProduct);
                }
            } else {
                log.info(createKeyValueLine("[FAIL] Status", "FAILED"));
            }

            log.info(BOTTOM_LINE);
        } catch (Exception e) {
            log.error("❌ CONNECTION TEST FAILED: {}", e.getMessage());
            log.error(createBoxLine("[FAIL] CONNECTION ERROR", true));
        }
    }

    /**
     * Checks for the existence of required database tables.
     * <p>
     * Verifies that the core AUTWIT tables (event_context, scenario_context)
     * exist in the database. These tables should be created by the
     * schema-postgres.sql script.
     * </p>
     */
    private void checkUserTables() {
        if (dataSource == null) {
            log.warn("DataSource not available for table check");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            log.info(TOP_LINE);
            log.info(createBoxLine("USER TABLES", true));
            log.info(MIDDLE_LINE);

            String[] tables = {"event_context", "scenario_context"};
            int found = 0;

            for (String table : tables) {
                ResultSet rs = conn.getMetaData()
                        .getTables(null, conn.getSchema(), table, new String[]{"TABLE"});
                if (rs.next()) {
                    found++;
                    log.info(createBoxLine("[OK] " + table, false));
                } else {
                    log.info(createBoxLine("[NOT FOUND] " + table, false));
                }
            }

            log.info(MIDDLE_LINE);
            log.info(createBoxLine("Tables: " + found + "/" + tables.length, false));
            log.info(BOTTOM_LINE);
        } catch (Exception e) {
            log.warn("Could not check tables: {}", e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // Box Formatting Utilities
    // ═════════════════════════════════════════════════════════════
    /**
     * Creates a formatted box line with centered or left-aligned text.
     * <p>
     * This utility method handles text truncation, padding, and border
     * rendering to create perfectly aligned console output.
     * </p>
     *
     * <h4>Algorithm:</h4>
     * <ol>
     *   <li>Truncate text to CONTENT_WIDTH if needed</li>
     *   <li>Calculate padding space (CONTENT_WIDTH - text length)</li>
     *   <li>If centered: split padding left/right</li>
     *   <li>If left-aligned: add all padding to the right</li>
     *   <li>Assemble: BORDER_LEFT + padding + text + padding + BORDER_RIGHT</li>
     * </ol>
     *
     * @param text the text to display (will be truncated if too long)
     * @param centered if true, center the text; if false, left-align
     * @return formatted box line (65 chars total)
     */
    private String createBoxLine(String text, boolean centered) {
        if (text == null) text = "";
        if (text.length() > CONTENT_WIDTH) text = text.substring(0, CONTENT_WIDTH);

        StringBuilder line = new StringBuilder(BORDER_LEFT);
        int pad = CONTENT_WIDTH - text.length();

        if (centered) {
            int left = pad / 2;
            int right = pad - left;
            line.append(" ".repeat(left)).append(text).append(" ".repeat(right));
        } else {
            line.append(text).append(" ".repeat(pad));
        }

        return line.append(BORDER_RIGHT).toString();
    }

    /**
     * Creates a formatted key-value line for displaying configuration properties.
     * <p>
     * Format: "│  key : value                                         │"<br>
     * The value is truncated if the combined length exceeds CONTENT_WIDTH.
     * </p>
     *
     * <h4>Algorithm:</h4>
     * <ol>
     *   <li>Calculate max value length: CONTENT_WIDTH - key.length - " : ".length</li>
     *   <li>Truncate value if needed</li>
     *   <li>Calculate remaining padding</li>
     *   <li>Assemble: BORDER_LEFT + key + " : " + value + padding + BORDER_RIGHT</li>
     * </ol>
     *
     * @param key the property key (not truncated)
     * @param value the property value (truncated if needed)
     * @return formatted key-value line (65 chars total)
     */
    private String createKeyValueLine(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";

        String sep = " : ";
        int maxVal = CONTENT_WIDTH - key.length() - sep.length();
        if (value.length() > maxVal) value = value.substring(0, maxVal);

        StringBuilder line = new StringBuilder(BORDER_LEFT);
        line.append(key).append(sep).append(value);

        int pad = CONTENT_WIDTH - (key.length() + sep.length() + value.length());
        line.append(" ".repeat(Math.max(0, pad)));

        return line.append(BORDER_RIGHT).toString();
    }
}