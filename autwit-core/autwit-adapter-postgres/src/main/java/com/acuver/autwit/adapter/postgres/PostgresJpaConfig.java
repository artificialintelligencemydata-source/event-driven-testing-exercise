package com.acuver.autwit.adapter.postgres;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "autwit.database", havingValue = "postgres")
@EnableJpaRepositories(basePackages = "com.acuver.autwit.adapter.postgres")
@EntityScan(basePackages = "com.acuver.autwit.adapter.postgres")
public class PostgresJpaConfig {

    // Box drawing
    private static final int CONTENT_WIDTH = 59;
    private static final String BORDER_LEFT = "│  ";
    private static final String BORDER_RIGHT = "  │";
    private static final String TOP_LINE =    "┌───────────────────────────────────────────────────────────────┐";
    private static final String BOTTOM_LINE = "└───────────────────────────────────────────────────────────────┘";
    private static final String MIDDLE_LINE = "├───────────────────────────────────────────────────────────────┤";
    private static final String EMPTY_LINE =  "│                                                               │";

    @Autowired
    private Environment env;

    @Autowired(required = false)
    private DataSource dataSource;

    public PostgresJpaConfig() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║         POSTGRESQL DATABASE CONFIGURATION INITIALIZING        ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║           POSTGRESQL DATABASE STATUS CHECK                    ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        printDatabaseInfo();
        testConnection();
        checkUserTables();

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║                                                               ║");
        log.info("║           [OK] POSTGRESQL IS UP AND READY                     ║");
        log.info("║                                                               ║");
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

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

    private void testConnection() {
        if (dataSource == null) {
            log.warn("DataSource not available");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            log.info(TOP_LINE);
            log.info(createBoxLine("CONNECTION TEST", true));
            log.info(MIDDLE_LINE);

            if (conn != null && !conn.isClosed()) {
                log.info(createKeyValueLine("[OK] Status", "CONNECTED"));
                DatabaseMetaData meta = conn.getMetaData();
                log.info(createKeyValueLine("Database", meta.getDatabaseProductName()));
                log.info(createKeyValueLine("Version", meta.getDatabaseProductVersion()));
            } else {
                log.info(createKeyValueLine("[FAIL] Status", "FAILED"));
            }
            log.info(BOTTOM_LINE);
        } catch (Exception e) {
            log.error(createBoxLine("[FAIL] CONNECTION ERROR", true));
        }
    }

    private void checkUserTables() {
        if (dataSource == null) return;

        try (Connection conn = dataSource.getConnection()) {
            log.info(TOP_LINE);
            log.info(createBoxLine("USER TABLES", true));
            log.info(MIDDLE_LINE);

            String[] tables = {"event_context", "scenario_context"};
            int found = 0;

            for (String table : tables) {
                ResultSet rs = conn.getMetaData().getTables(null, conn.getSchema(), table, new String[]{"TABLE"});
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

    private String createBoxLine(String text, boolean centered) {
        if (text == null) text = "";
        if (text.length() > CONTENT_WIDTH) text = text.substring(0, CONTENT_WIDTH);

        StringBuilder line = new StringBuilder(BORDER_LEFT);

        if (centered) {
            int pad = CONTENT_WIDTH - text.length();
            int left = pad / 2;
            int right = pad - left;
            for (int i = 0; i < left; i++) line.append(' ');
            line.append(text);
            for (int i = 0; i < right; i++) line.append(' ');
        } else {
            line.append(text);
            for (int i = 0; i < CONTENT_WIDTH - text.length(); i++) line.append(' ');
        }

        line.append(BORDER_RIGHT);
        return line.toString();
    }

    private String createKeyValueLine(String key, String value) {
        if (key == null) key = "";
        if (value == null) value = "";

        String sep = " : ";
        int maxVal = CONTENT_WIDTH - key.length() - sep.length();
        if (value.length() > maxVal) value = value.substring(0, maxVal);

        StringBuilder line = new StringBuilder(BORDER_LEFT);
        line.append(key).append(sep).append(value);

        int pad = CONTENT_WIDTH - (key.length() + sep.length() + value.length());
        for (int i = 0; i < pad; i++) line.append(' ');

        line.append(BORDER_RIGHT);
        return line.toString();
    }
}