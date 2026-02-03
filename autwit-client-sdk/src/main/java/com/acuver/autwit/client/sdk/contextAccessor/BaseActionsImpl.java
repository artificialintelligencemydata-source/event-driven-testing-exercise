package com.acuver.autwit.client.sdk.contextAccessor;

import com.acuver.autwit.client.sdk.Autwit;
import io.restassured.response.Response;
import org.w3c.dom.Document;

/**
 * BaseActions Facade Implementation.
 *
 * <h2>DELEGATES TO</h2>
 * com.acuver.autwit.internal.api.BaseActions
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class BaseActionsImpl implements Autwit.ContextAccessor.BaseActions {

    private Class<?> baseActionsClass;

    /**
     * Get BaseActions class.
     * Located at: com.acuver.autwit.internal.api.BaseActions
     */
    private Class<?> getBaseActionsClass() {
        if (baseActionsClass == null) {
            try {
                baseActionsClass = Class.forName("com.acuver.autwit.internal.api.BaseActions");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "BaseActions not found at com.acuver.autwit.internal.api.BaseActions. " +
                                "Ensure the class is in autwit-internal module.", e);
            }
        }
        return baseActionsClass;
    }

    // ==========================================================================
    // API CALL METHODS
    // ==========================================================================

    @Override
    public Response makeAPICall(String apiName, String httpMethod, String inputXml, String outputTemplate) throws Exception {
        //log.debug("Making API call: {} ({}) with template: {}", apiName, httpMethod,
                //outputTemplate != null && !outputTemplate.isEmpty() ? "Yes" : "No");
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "makeAPICall", String.class, String.class, String.class, String.class);
            return (Response) method.invoke(null, apiName, httpMethod, inputXml, outputTemplate);
        } catch (Exception e) {
            //log.error("Failed to make API call {}: {}", apiName, e.getMessage());
            throw new RuntimeException("Failed to make API call: " + apiName, e);
        }
    }

    @Override
    public Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws Exception {
        //log.debug("Making service call: {} ({})", serviceName, httpMethod);
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "makeServiceCall", String.class, String.class, String.class);
            return (Response) method.invoke(null, serviceName, httpMethod, inputXml);
        } catch (Exception e) {
            //log.error("Failed to make service call {}: {}", serviceName, e.getMessage());
            throw new RuntimeException("Failed to make service call: " + serviceName, e);
        }
    }

    // ==========================================================================
    // FILE OPERATIONS
    // ==========================================================================

    @Override
    public String generateStrFromRes(String filePath) {
        //log.debug("Reading file: {}", filePath);
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod("GenerateStrFromRes", String.class);
            return (String) method.invoke(null, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void saveResponseAsXML(String filePath, String xmlStr) {
        //log.debug("Saving XML to: {}", filePath);
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod("SaveResponseAsXML", String.class, String.class);
            method.invoke(null, filePath, xmlStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save XML to: " + filePath, e);
        }
    }

    // ==========================================================================
    // XML OPERATIONS
    // ==========================================================================

    @Override
    public String xmlXpathReader(String filePath, String xpath) {
        //log.debug("Reading XPath: {} from {}", xpath, filePath);
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod("XMLXpathReader", String.class, String.class);
            return (String) method.invoke(null, filePath, xpath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read XPath: " + xpath, e);
        }
    }

    @Override
    public String getXmlRootName(String xmlStr) {
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod("getXmlRootName", String.class);
            return (String) method.invoke(null, xmlStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get XML root name", e);
        }
    }

    @Override
    public void editXmlSingleNode(String nodeName, String nodeValue, String filePath) {
        //log.debug("Editing XML node: {} = {} in {}", nodeName, nodeValue, filePath);
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "editXmlSingleNode", String.class, String.class, String.class);
            method.invoke(null, nodeName, nodeValue, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to edit XML node: " + nodeName, e);
        }
    }

    @Override
    public Document getDocumentFromXmlString(String xmlStr) {
        try {
            Class<?> clazz = getBaseActionsClass();
            java.lang.reflect.Method method = clazz.getMethod("getDocumentFromXmlString", String.class);
            return (Document) method.invoke(null, xmlStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML to Document", e);
        }
    }
}