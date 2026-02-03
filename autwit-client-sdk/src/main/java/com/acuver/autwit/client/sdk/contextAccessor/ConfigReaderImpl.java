package com.acuver.autwit.client.sdk;


/**
 * Configuration Reader Implementation.
 *
 * <h2>MAPS TO</h2>
 * com.acuver.automation.api.utils.configuration.ConfigFileReader
 * via FileReaderManager.getInstance().getConfigReader()
 *
 * <h2>PURPOSE</h2>
 * Provides access to configuration properties from config.properties file.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class ConfigReaderImpl implements Autwit.ContextAccessor.ConfigReader {

    // Cached config reader
    private Object configReader;

    /**
     * Get ConfigFileReader instance via FileReaderManager.
     */
    private Object getConfigReader() {
        if (configReader == null) {
            try {
                // Try new package first
                Class<?> managerClass = Class.forName("com.acuver.autwit.internal.config.FileReaderManager");
                Object manager = managerClass.getMethod("getInstance").invoke(null);
                configReader = managerClass.getMethod("getConfigReader").invoke(manager);
            } catch (ClassNotFoundException e1) {
                try {
                    // Fall back to old package
                    Class<?> managerClass = Class.forName("com.acuver.automation.api.utils.configuration.FileReaderManager");
                    Object manager = managerClass.getMethod("getInstance").invoke(null);
                    configReader = managerClass.getMethod("getConfigReader").invoke(manager);
                } catch (Exception e2) {
                    throw new RuntimeException("FileReaderManager not found in either package", e2);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get ConfigReader", e);
            }
        }
        return configReader;
    }

    /**
     * Invoke a String getter method.
     */
    private String invokeStringGetter(String methodName) {
        try {
            Object reader = getConfigReader();
            java.lang.reflect.Method method = reader.getClass().getMethod(methodName);
            return (String) method.invoke(reader);
        } catch (Exception e) {
            ///log.warn("Failed to get config property via {}: {}", methodName, e.getMessage());
            return null;
        }
    }

    /**
     * Invoke an int getter method.
     */
    private int invokeIntGetter(String methodName) {
        try {
            Object reader = getConfigReader();
            java.lang.reflect.Method method = reader.getClass().getMethod(methodName);
            return (int) method.invoke(reader);
        } catch (Exception e) {
            //log.warn("Failed to get config property via {}: {}", methodName, e.getMessage());
            return 0;
        }
    }

    /**
     * Invoke a long getter method.
     */
    private long invokeLongGetter(String methodName) {
        try {
            Object reader = getConfigReader();
            java.lang.reflect.Method method = reader.getClass().getMethod(methodName);
            return (long) method.invoke(reader);
        } catch (Exception e) {
            //log.warn("Failed to get config property via {}: {}", methodName, e.getMessage());
            return 0L;
        }
    }

    /**
     * Invoke a boolean getter method.
     */
    private boolean invokeBooleanGetter(String methodName) {
        try {
            Object reader = getConfigReader();
            java.lang.reflect.Method method = reader.getClass().getMethod(methodName);
            return (boolean) method.invoke(reader);
        } catch (Exception e) {
            //log.warn("Failed to get config property via {}: {}", methodName, e.getMessage());
            return false;
        }
    }

    // ==========================================================================
    // URL CONFIGURATION
    // ==========================================================================

    @Override
    public String getBaseUrl() {
        return invokeStringGetter("getBaseUrl");
    }

    @Override
    public String getEndPointUrl() {
        return invokeStringGetter("getEndPointUrl");
    }

    // ==========================================================================
    // PATH CONFIGURATION
    // ==========================================================================

    @Override
    public String getInputXmlPath() {
        return invokeStringGetter("getInputXmlPath");
    }

    @Override
    public String getResponseXmlPath() {
        return invokeStringGetter("getResponseXmlPath");
    }

    @Override
    public String getApiTemplatesXmlPath() {
        return invokeStringGetter("getApiTemplatesXmlPath");
    }

    @Override
    public String getValidationExcelsPath() {
        return invokeStringGetter("getValidationExcelsPath");
    }

    @Override
    public String getTransferOrderInputXmls() {
        return invokeStringGetter("getTransferOrderInputXmls");
    }

    @Override
    public String getPurchaseOrderXmls() {
        return invokeStringGetter("getPurchaseOrderXmls");
    }

    // ==========================================================================
    // AUTHENTICATION
    // ==========================================================================

    @Override
    public String getUserId() {
        return invokeStringGetter("getUserId");
    }

    @Override
    public String getPassword() {
        return invokeStringGetter("getPassword");
    }

    @Override
    public String getProgId() {
        return invokeStringGetter("getProgId");
    }

    // ==========================================================================
    // TIMING
    // ==========================================================================

    @Override
    public int getDaysForReqDeliveryDate() {
        return invokeIntGetter("getDaysForReqDeliveryDate");
    }

    @Override
    public long getImplicitlyWait() {
        return invokeLongGetter("getImplicitlyWait");
    }

    // ==========================================================================
    // JIRA INTEGRATION
    // ==========================================================================

    @Override
    public String getJiraURL() {
        return invokeStringGetter("getJiraURL");
    }

    @Override
    public String getJiraUserName() {
        return invokeStringGetter("getJiraUserName");
    }

    @Override
    public String getJiraAPIToken() {
        return invokeStringGetter("getJiraAPIToken");
    }

    @Override
    public String getJiraProjectName() {
        return invokeStringGetter("getJiraProjectName");
    }

    @Override
    public boolean isTicketNeedsToBeLogged() {
        return invokeBooleanGetter("isTicketNeedsToBeLogged");
    }
}