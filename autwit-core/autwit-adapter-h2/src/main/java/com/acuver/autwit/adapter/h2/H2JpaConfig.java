package com.acuver.autwit.adapter.h2;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.sql.Statement;
import java.util.Map;


@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "h2")
@EnableJpaRepositories(basePackages = "com.acuver.autwit.adapter.h2")
@EntityScan(basePackages = "com.acuver.autwit.adapter.h2")
public class H2JpaConfig {
    private static final Logger log = LogManager.getLogger(H2JpaConfig.class);
    // Box drawing characters and dimensions
    private static final int CONTENT_WIDTH = 59;
    private static final String BORDER_LEFT = "│  ";
    private static final String BORDER_RIGHT = "  │";
    private static final String TOP_LINE =    "┌───────────────────────────────────────────────────────────────┐";
    private static final String BOTTOM_LINE = "└───────────────────────────────────────────────────────────────┘";
    private static final String MIDDLE_LINE = "├───────────────────────────────────────────────────────────────┤";
    private static final String EMPTY_LINE =  "│                                                               │";

    @Autowired
    private Environment env;

    // Lazy injection to avoid circular dependency (DataSource is created in this class)
    @Autowired
    @org.springframework.context.annotation.Lazy
    private DataSource dataSource;

    // ─────────────────────────────────────────────────────────────
    // DataSource Bean - Adapter owns its infrastructure
    // ─────────────────────────────────────────────────────────────
    /**
     * Creates H2 DataSource bean.
     * 
     * This adapter OWNS the DataSource creation. Spring Boot's
     * DataSourceAutoConfiguration is excluded, so adapters must
     * create their own DataSource beans.
     * 
     * Configuration is read from spring.datasource.* properties
     * in application-h2.yml profile.
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        log.info("Creating H2 DataSource bean");
        
        String url = env.getProperty("spring.datasource.url", 
            "jdbc:h2:mem:autwit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        String driverClassName = env.getProperty("spring.datasource.driver-class-name", 
            "org.h2.Driver");
        String username = env.getProperty("spring.datasource.username", "sa");
        String password = env.getProperty("spring.datasource.password", "");
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driverClassName);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("AUTWIT-H2-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        log.info("H2 DataSource configured: url={}, username={}", url, username);
        return new HikariDataSource(config);
    }

    // ─────────────────────────────────────────────────────────────
    // EntityManagerFactory Bean - Adapter owns JPA infrastructure
    // ─────────────────────────────────────────────────────────────
    /**
     * Creates EntityManagerFactory bean for JPA repositories.
     * 
     * CRITICAL: Since HibernateJpaAutoConfiguration is excluded at framework level,
     * adapters MUST create their own EntityManagerFactory beans. This ensures:
     * - Adapters own their complete JPA infrastructure
     * - No Spring Boot auto-configuration interference
     * - Deterministic startup
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("Creating H2 EntityManagerFactory bean");
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.acuver.autwit.adapter.h2");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        // Build JPA properties from Environment (since HibernateJpaAutoConfiguration is excluded)
        Map<String, Object> properties = new java.util.HashMap<>();
        
        // Read Hibernate dialect (required for H2)
        String dialect = env.getProperty("spring.jpa.properties.hibernate.dialect",
                env.getProperty("spring.jpa.hibernate.dialect",
                        "org.hibernate.dialect.H2Dialect"));
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
        
        log.info("H2 EntityManagerFactory configured");
        return em;
    }

    // ─────────────────────────────────────────────────────────────
    // TransactionManager Bean - Required for JPA transactions
    // ─────────────────────────────────────────────────────────────
    /**
     * Creates PlatformTransactionManager bean for JPA transactions.
     * 
     * CRITICAL: Since HibernateJpaAutoConfiguration is excluded, adapters
     * must create their own transaction manager.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("Creating H2 TransactionManager bean");
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    public H2JpaConfig() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║           H2 DATABASE CONFIGURATION INITIALIZING              ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        String ddlAuto = System.getProperty("spring.jpa.hibernate.ddl-auto",
                System.getenv("SPRING_JPA_HIBERNATE_DDL_AUTO"));
        if (ddlAuto == null) {
            ddlAuto = "update";
        }

        log.info("Schema Management Mode: {}", ddlAuto);

        if ("create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
            log.info("Hibernate will DROP and CREATE tables");
        } else if ("update".equals(ddlAuto)) {
            log.info("Hibernate will UPDATE existing schema");
        } else if ("validate".equals(ddlAuto)) {
            log.info("Hibernate will VALIDATE schema only");
        } else if ("none".equals(ddlAuto)) {
            log.info("Hibernate DDL is DISABLED");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║              H2 DATABASE STATUS CHECK                         ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        printDatabaseInfo();
        testConnection();
        printSchemaInitializationInfo();
        listTables();
        checkUserTables();
        printH2ConsoleInfo();

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║              [OK] H2 DATABASE IS UP AND READY                 ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    private void printDatabaseInfo() {
        try {
            String url = env.getProperty("spring.datasource.url", "Not configured");
            String username = env.getProperty("spring.datasource.username", "Not configured");
            String driverClass = env.getProperty("spring.datasource.driver-class-name", "Not configured");
            String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "Not configured");
            String showSql = env.getProperty("spring.jpa.show-sql", "false");

            log.info(TOP_LINE);
            log.info(createBoxLine("DATABASE CONFIGURATION", true));
            log.info(MIDDLE_LINE);
            log.info(createKeyValueLine("Database Type", "H2 In-Memory Database"));
            log.info(createKeyValueLine("JDBC URL", url));
            log.info(createKeyValueLine("Username", username));
            log.info(createKeyValueLine("Driver Class", driverClass));
            log.info(createKeyValueLine("DDL Auto", ddlAuto));
            log.info(createKeyValueLine("Show SQL", showSql));
            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.warn("Could not retrieve database configuration: {}", e.getMessage());
        }
    }

    private void printSchemaInitializationInfo() {
        try {
            String initMode = env.getProperty("spring.sql.init.mode", "embedded");
            String schemaLocations = env.getProperty("spring.sql.init.schema-locations", "Not configured");
            String dataLocations = env.getProperty("spring.sql.init.data-locations", "Not configured");
            String continueOnError = env.getProperty("spring.sql.init.continue-on-error", "false");

            log.info(TOP_LINE);
            log.info(createBoxLine("SCHEMA INITIALIZATION", true));
            log.info(MIDDLE_LINE);
            log.info(createKeyValueLine("Init Mode", initMode));
            log.info(createKeyValueLine("Schema Locations", schemaLocations));
            log.info(createKeyValueLine("Data Locations", dataLocations));
            log.info(createKeyValueLine("Continue on Error", continueOnError));
            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.warn("Could not retrieve schema initialization info: {}", e.getMessage());
        }
    }

    private void testConnection() {
        if (dataSource == null) {
            log.warn("DataSource is not available");
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            log.info(TOP_LINE);
            log.info(createBoxLine("CONNECTION TEST", true));
            log.info(MIDDLE_LINE);

            if (connection != null && !connection.isClosed()) {
                log.info(createKeyValueLine("[OK] Connection Status", "CONNECTED"));

                DatabaseMetaData metaData = connection.getMetaData();
                log.info(createKeyValueLine("Database Name", metaData.getDatabaseProductName()));
                log.info(createKeyValueLine("Database Version", metaData.getDatabaseProductVersion()));
                log.info(createKeyValueLine("Driver Name", metaData.getDriverName()));
                log.info(createKeyValueLine("Driver Version", metaData.getDriverVersion()));
                log.info(createKeyValueLine("JDBC Version", metaData.getJDBCMajorVersion() + "." + metaData.getJDBCMinorVersion()));
                log.info(createKeyValueLine("Catalog", connection.getCatalog()));
                log.info(createKeyValueLine("Auto Commit", String.valueOf(connection.getAutoCommit())));
                log.info(createKeyValueLine("Read Only", String.valueOf(connection.isReadOnly())));
            } else {
                log.info(createKeyValueLine("[FAIL] Connection Status", "FAILED"));
            }

            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.info(TOP_LINE);
            log.info(createBoxLine("[FAIL] CONNECTION FAILED", true));
            log.info(MIDDLE_LINE);
            log.info(createKeyValueLine("Error", e.getMessage()));
            log.info(BOTTOM_LINE);
        }
    }

    private void listTables() {
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE", "BASE TABLE"});

            log.info(TOP_LINE);
            log.info(createBoxLine("DATABASE TABLES (All)", true));
            log.info(MIDDLE_LINE);

            int tableCount = 0;
            int userTableCount = 0;

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (isUserTable(tableName)) {
                    userTableCount++;
                }
                tableCount++;
            }

            if (userTableCount == 0) {
                log.info(createBoxLine("[INFO] No user tables found yet", false));
            } else {
                log.info(createKeyValueLine("User Tables", String.valueOf(userTableCount)));
            }
            log.info(createKeyValueLine("System Tables", String.valueOf(tableCount - userTableCount)));
            log.info(createKeyValueLine("Total Tables", String.valueOf(tableCount)));

            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.warn("Could not list tables: {}", e.getMessage());
        }
    }

    private void checkUserTables() {
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            log.info(TOP_LINE);
            log.info(createBoxLine("USER TABLES & STRUCTURE", true));
            log.info(MIDDLE_LINE);

            String[] userTables = {"EVENT_CONTEXT", "SCENARIO_CONTEXT", "EVENT_STORE", "SCENARIO_AUDIT_LOG"};
            int foundCount = 0;

            for (String tableName : userTables) {
                ResultSet tables = metaData.getTables(null, null, tableName, new String[]{"TABLE", "BASE TABLE"});

                if (tables.next()) {
                    foundCount++;
                    log.info(createBoxLine("[OK] " + tableName, false));

                    // Get row count
                    try (Statement stmt = connection.createStatement()) {
                        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
                        if (rs.next()) {
                            int rowCount = rs.getInt(1);
                            log.info(createBoxLine("     Rows: " + rowCount, false));
                        }
                    } catch (Exception ex) {
                        log.debug("Could not get row count for {}: {}", tableName, ex.getMessage());
                    }

                    // Get column info
                    ResultSet columns = metaData.getColumns(null, null, tableName, "%");
                    int colCount = 0;
                    StringBuilder colNames = new StringBuilder();
                    while (columns.next()) {
                        if (colCount > 0) colNames.append(", ");
                        colNames.append(columns.getString("COLUMN_NAME"));
                        colCount++;
                    }
                    String colInfo = colCount + " (" + truncate(colNames.toString(), 32) + ")";
                    log.info(createBoxLine("     Columns: " + colInfo, false));

                } else {
                    log.info(createBoxLine("[NOT FOUND] " + tableName, false));
                }
                tables.close();
            }

            if (foundCount == 0) {
                log.info(EMPTY_LINE);
                log.info(createBoxLine("[WARN] No application tables found!", false));
                log.info(createBoxLine("[INFO] Tables created on first use", false));
            } else if (foundCount < userTables.length) {
                log.info(EMPTY_LINE);
                log.info(createBoxLine("[WARN] Some tables missing (" + foundCount + "/" + userTables.length + ")", false));
            } else {
                log.info(MIDDLE_LINE);
                log.info(createBoxLine("[OK] All tables created (" + foundCount + "/4)", false));
            }

            log.info(BOTTOM_LINE);

        } catch (Exception e) {
            log.warn("Could not check user tables: {}", e.getMessage());
        }
    }

    private void printH2ConsoleInfo() {
        String consoleEnabled = env.getProperty("spring.h2.console.enabled", "false");
        String consolePath = env.getProperty("spring.h2.console.path", "/h2-console");
        String port = env.getProperty("server.port", "8080");

        log.info(TOP_LINE);
        log.info(createBoxLine("H2 WEB CONSOLE", true));
        log.info(MIDDLE_LINE);

        if ("true".equalsIgnoreCase(consoleEnabled)) {
            String url = "http://localhost:" + port + consolePath;
            String jdbcUrl = env.getProperty("spring.datasource.url", "jdbc:h2:mem:testdb");
            String username = env.getProperty("spring.datasource.username", "sa");
            String password = env.getProperty("spring.datasource.password", "(empty)");

            log.info(createKeyValueLine("[OK] Status", "ENABLED"));
            log.info(createKeyValueLine("URL", url));
            log.info(createKeyValueLine("JDBC URL", jdbcUrl));
            log.info(createKeyValueLine("Username", username));
            log.info(createKeyValueLine("Password", password));
        } else {
            log.info(createKeyValueLine("[INFO] Status", "DISABLED"));
            log.info(createBoxLine("[TIP] Enable: spring.h2.console.enabled=true", false));
        }

        log.info(BOTTOM_LINE);
    }

    private boolean isUserTable(String tableName) {
        if (tableName == null) return false;

        String[] systemTables = {
                "CONSTANTS", "ENUM_VALUES", "INDEXES", "INDEX_COLUMNS",
                "INFORMATION_SCHEMA_CATALOG_NAME", "IN_DOUBT", "LOCKS",
                "QUERY_STATISTICS", "RIGHTS", "ROLES", "SESSIONS",
                "SESSION_STATE", "SETTINGS", "SYNONYMS", "USERS"
        };

        for (String sysTable : systemTables) {
            if (sysTable.equalsIgnoreCase(tableName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a properly formatted box line - NO EMOJIS to avoid width issues
     */
    private String createBoxLine(String text, boolean centered) {
        if (text == null) {
            text = "";
        }

        // Truncate if necessary
        if (text.length() > CONTENT_WIDTH) {
            text = text.substring(0, CONTENT_WIDTH);
        }

        StringBuilder line = new StringBuilder(BORDER_LEFT);

        if (centered) {
            int totalPadding = CONTENT_WIDTH - text.length();
            int leftPadding = totalPadding / 2;
            int rightPadding = totalPadding - leftPadding;

            for (int i = 0; i < leftPadding; i++) {
                line.append(' ');
            }
            line.append(text);
            for (int i = 0; i < rightPadding; i++) {
                line.append(' ');
            }
        } else {
            line.append(text);
            int padding = CONTENT_WIDTH - text.length();
            for (int i = 0; i < padding; i++) {
                line.append(' ');
            }
        }

        line.append(BORDER_RIGHT);
        return line.toString();
    }

    /**
     * Creates a key-value line - NO EMOJIS to avoid width issues
     */
    private String createKeyValueLine(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";

        String separator = " : ";
        int maxValueLength = CONTENT_WIDTH - key.length() - separator.length();

        if (value.length() > maxValueLength) {
            value = value.substring(0, maxValueLength);
        }

        StringBuilder line = new StringBuilder(BORDER_LEFT);
        line.append(key);
        line.append(separator);
        line.append(value);

        int currentLength = key.length() + separator.length() + value.length();
        int padding = CONTENT_WIDTH - currentLength;
        for (int i = 0; i < padding; i++) {
            line.append(' ');
        }

        line.append(BORDER_RIGHT);
        return line.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}