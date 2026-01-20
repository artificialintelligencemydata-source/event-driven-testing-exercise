package com.acuver.autwit.internal.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * ConfigFileReader - Configuration access from properties file.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-internal
 * Package: com.acuver.autwit.internal.config
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class ConfigFileReader {

    private static final Logger logger = LogManager.getLogger(ConfigFileReader.class);

    private Properties properties;
    private final String BASE_DIRECTORY = "target/testData";

    public ConfigFileReader(long threadId) {
        BufferedReader reader = null;
        String configFilePath = BASE_DIRECTORY + "/Thread_" + threadId + "/config.properties";
        File sourceDirectory = new File(configFilePath);

        try {
            if (!configFilePath.startsWith(BASE_DIRECTORY + "/Thread_" + threadId)) {
                throw new RuntimeException("Thread ID mismatch: Cannot access file " + configFilePath);
            }

            if (!sourceDirectory.exists()) {
                logger.error("Configuration file not found: {}", configFilePath);
                throw new RuntimeException("Configuration file not found: " + configFilePath);
            }

            reader = new BufferedReader(new FileReader(sourceDirectory));
            properties = new Properties();
            properties.load(reader);
            reader.close();

        } catch (FileNotFoundException e) {
            logger.error("Configuration file not found at: {}", configFilePath, e);
            throw new RuntimeException("Configuration file not found at: " + configFilePath, e);
        } catch (IOException e) {
            logger.error("Error reading configuration file: {}", configFilePath, e);
            throw new RuntimeException("Error reading configuration file: " + configFilePath, e);
        } catch (Exception e) {
            logger.error("Unexpected error while reading the config file: {}", configFilePath, e);
            throw new RuntimeException("Unexpected error while reading the config file", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing BufferedReader for config file: {}", configFilePath, e);
                }
            }
        }
    }

    // ==========================================================================
    // URL CONFIGURATION
    // ==========================================================================

    public String getBaseUrl() {
        String url = properties.getProperty("baseUrl");
        if (url != null) return url;
        throw new RuntimeException("baseUrl not specified in Configuration.properties file.");
    }

    public String getEndPointUrl() {
        String url = properties.getProperty("endPointUrl");
        if (url != null) return url;
        throw new RuntimeException("endPointUrl not specified in Configuration.properties file.");
    }

    // ==========================================================================
    // PATH CONFIGURATION
    // ==========================================================================

    public String getInputXmlPath() {
        String inputXmlPath = properties.getProperty("inputXml");
        if (inputXmlPath != null) return inputXmlPath;
        throw new RuntimeException("inputXml not specified in Configuration.properties file.");
    }

    public String getResponseXmlPath() {
        String responseXmlPath = properties.getProperty("responseXmls");
        if (responseXmlPath != null) return responseXmlPath;
        throw new RuntimeException("responseXmls not specified in Configuration.properties file.");
    }

    public String getApiTemplatesXmlPath() {
        String apiTemplatesXmlPath = properties.getProperty("apiTemplatesXmlPath");
        if (apiTemplatesXmlPath != null) return apiTemplatesXmlPath;
        throw new RuntimeException("apiTemplatesXmlPath not specified in Configuration.properties file.");
    }

    public String getValidationExcelsPath() {
        String validationExcelsPath = properties.getProperty("validationExcels");
        if (validationExcelsPath != null) return validationExcelsPath;
        throw new RuntimeException("validationExcels not specified in Configuration.properties file.");
    }

    public String getTransferOrderInputXmls() {
        String inputXmlPath = properties.getProperty("transferOrderXmls");
        if (inputXmlPath != null) return inputXmlPath;
        throw new RuntimeException("transferOrderXmls not specified in Configuration.properties file.");
    }

    public String getPurchaseOrderXmls() {
        String purchaseOrderXmls = properties.getProperty("purchaseOrderXmls");
        if (purchaseOrderXmls != null) return purchaseOrderXmls;
        throw new RuntimeException("purchaseOrderXmls not specified in Configuration.properties file.");
    }

    // ==========================================================================
    // AUTHENTICATION
    // ==========================================================================

    public String getProgId() {
        String progId = properties.getProperty("YFSEnvironment.progId");
        if (progId != null) return progId;
        throw new RuntimeException("progId not specified in Configuration.properties file.");
    }

    public String getUserId() {
        String userId = properties.getProperty("userId");
        if (userId != null) return userId;
        throw new RuntimeException("userId not specified in Configuration.properties file.");
    }

    public String getPassword() {
        String password = properties.getProperty("password");
        if (password != null) return password;
        throw new RuntimeException("password not specified in Configuration.properties file.");
    }

    // ==========================================================================
    // TIMING
    // ==========================================================================

    public long getImplicitlyWait() {
        String implicitlyWait = properties.getProperty("implicitlyWait");
        if (implicitlyWait != null) return Long.parseLong(implicitlyWait);
        throw new RuntimeException("implicitlyWait not specified in Configuration.properties file.");
    }

    public int getDaysForReqDeliveryDate() {
        String strDaysForReqDeliveryDate = properties.getProperty("DaysForReqDeliveryDate");
        if (strDaysForReqDeliveryDate != null) return Integer.parseInt(strDaysForReqDeliveryDate);
        throw new RuntimeException("DaysForReqDeliveryDate not specified in Configuration.properties file.");
    }

    // ==========================================================================
    // JIRA INTEGRATION
    // ==========================================================================

    public String getJiraURL() {
        String jiraURL = properties.getProperty("JiraURL");
        if (jiraURL != null) return jiraURL;
        throw new RuntimeException("JiraURL not specified in Configuration.properties file.");
    }

    public String getJiraUserName() {
        String jiraUserName = properties.getProperty("JiraUserName");
        if (jiraUserName != null) return jiraUserName;
        throw new RuntimeException("JiraUserName not specified in Configuration.properties file.");
    }

    public String getJiraAPIToken() {
        String jiraAPIToken = properties.getProperty("JiraAPIToken");
        if (jiraAPIToken != null) return jiraAPIToken;
        throw new RuntimeException("JiraAPIToken not specified in Configuration.properties file.");
    }

    public String getJiraProjectName() {
        String jiraProjectName = properties.getProperty("JiraProjectName");
        if (jiraProjectName != null) return jiraProjectName;
        throw new RuntimeException("JiraProjectName not specified in Configuration.properties file.");
    }

    public boolean isTicketNeedsToBeLogged() {
        String isTicketNeedsToBeLogged = properties.getProperty("isTicketNeedsToBeLogged");
        if (isTicketNeedsToBeLogged != null) return Boolean.parseBoolean(isTicketNeedsToBeLogged);
        throw new RuntimeException("isTicketNeedsToBeLogged not specified in Configuration.properties file.");
    }
}