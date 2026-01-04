package com.acuver.autwit.internal.config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class ConfigFileReader {

    private static final Logger LOG = LogManager.getLogger(ConfigFileReader.class);
    private final Properties properties;
    private final String configFilePath;

    public ConfigFileReader(String configFilePath) {
        this.configFilePath = configFilePath == null ? "src/test/resources/configuration.properties" : configFilePath;
        this.properties = new Properties();
        try (InputStream in = new FileInputStream(this.configFilePath)) {
            this.properties.load(in);
        } catch (FileNotFoundException e) {
            LOG.warn("Config file not found: {} — using empty properties", this.configFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed reading config", e);
        }
    }

    public String get(String key, String defaultValue){
        return properties.getProperty(key, defaultValue);
    }

    public Integer getInt(String key, int defaultValue){
        String v = properties.getProperty(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }
//    public String getExecutionStartTime() {
//        // If property exists in properties file:
//        String val = properties.getProperty("execution.start.time");
//        if (val != null && !val.isBlank()) {
//            return val;
//        }
//
//        // Otherwise generate one
//        return String.valueOf(System.currentTimeMillis());
//    }

    public String getExecutionStartTime() {
        String executionStartTime = properties.getProperty("executionStartTime");
        if (executionStartTime != null) return executionStartTime;
        else throw new RuntimeException("executionStartTime not specified in the configuration file.");
    }
    public String getJiraURL() {
        String jiraURL = properties.getProperty("JiraURL");
        if(jiraURL != null) return jiraURL;
        else throw new RuntimeException("JiraURL not specified in the Configuration.properties file.");
    }
    public String getJiraUserName() {
        String jiraUserName = properties.getProperty("JiraUserName");
        if(jiraUserName != null) return jiraUserName;
        else throw new RuntimeException("JiraUserName not specified in the Configuration.properties file.");
    }
    public String getJiraAPIToken() {
        String jiraAPIToken = properties.getProperty("JiraAPIToken");
        if(jiraAPIToken != null) return jiraAPIToken;
        else throw new RuntimeException("JiraAPIToken not specified in the Configuration.properties file.");
    }
    public String getJiraProjectName() {
        String jiraProjectName = properties.getProperty("JiraProjectName");
        if(jiraProjectName != null) return jiraProjectName;
        else throw new RuntimeException("JiraProjectName not specified in the Configuration.properties file.");
    }
    public boolean isTicketNeedsToBeLogged() {
        String isTicketNeedsToBeLogged = properties.getProperty("isTicketNeedsToBeLogged");
        if(isTicketNeedsToBeLogged!= null) return Boolean.parseBoolean(isTicketNeedsToBeLogged);
        else throw new RuntimeException("isTicketNeedsToBeLogged not specified in the Configuration.properties file.");
    }
    public String getCreateOrderEndPoint() {
        String createOrderURI = properties.getProperty("createOrderURI");
        if (createOrderURI != null) return createOrderURI;
        else throw new RuntimeException("progID not specified in the Config.properties file.");
    }
    public String getShipmentBaseUri() {
        String baseURI = properties.getProperty("shipmentBaseURI");
        if (baseURI != null) return baseURI;
        else throw new RuntimeException("shipmentBaseURI not specified in the Config.properties file.");
    }

    public String pickupShipmentEndPoint() {
        String pickupShipmentURI = properties.getProperty("pickupShipmentURI");
        if (pickupShipmentURI != null) return pickupShipmentURI;
        else throw new RuntimeException("progID not specified in the Config.properties file.");
    }
}
