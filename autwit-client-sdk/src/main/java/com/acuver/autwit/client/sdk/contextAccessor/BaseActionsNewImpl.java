package com.acuver.autwit.client.sdk.contextAccessor;

import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.core.domain.ApiContextEntities;
import io.restassured.response.Response;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BaseActionsNew Facade Implementation (v2.0).
 *
 * <h2>DELEGATES TO</h2>
 * com.acuver.autwit.internal.helper.BaseActionsNew
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class BaseActionsNewImpl implements Autwit.ContextAccessor.BaseActionsNew {

    private Class<?> baseActionsNewClass;

    /**
     * Get BaseActionsNew class via reflection.
     * Located at: com.acuver.autwit.internal.helper.BaseActionsNew
     */
    private Class<?> getBaseActionsNewClass() {
        if (baseActionsNewClass == null) {
            try {
                baseActionsNewClass = Class.forName("com.acuver.autwit.internal.helper.BaseActionsNew");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "BaseActionsNew not found at com.acuver.autwit.internal.helper.BaseActionsNew. " +
                                "Ensure the class is in autwit-internal-testkit module.", e);
            }
        }
        return baseActionsNewClass;
    }

    // ==========================================================================
    // API CALL METHODS
    // ==========================================================================

    @Override
    public Response makeAPICall(String apiName, String httpMethod, String inputXml, String outputTemplate) throws Exception {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "makeAPICall", String.class, String.class, String.class, String.class);
            return (Response) method.invoke(null, apiName, httpMethod, inputXml, outputTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to make API call: " + apiName, e);
        }
    }

    @Override
    public Response makeAPICallWithTemplate(String apiName, Map<String, String> parameters) throws Exception {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("makeAPICallWithTemplate", String.class, Map.class);
            return (Response) method.invoke(null, apiName, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Failed to make API call with template: " + apiName, e);
        }
    }

    @Override
    public Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws Exception {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "makeServiceCall", String.class, String.class, String.class);
            return (Response) method.invoke(null, serviceName, httpMethod, inputXml);
        } catch (Exception e) {
            throw new RuntimeException("Failed to make service call: " + serviceName, e);
        }
    }

    // ==========================================================================
    // STEP-LEVEL RETRIEVAL
    // ==========================================================================

    @Override
    public String getLastResponseFromCurrentStep(String apiName) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getLastResponseFromCurrentStep", String.class);
            return (String) method.invoke(null, apiName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get last response from current step: " + apiName, e);
        }
    }

    @Override
    public String getResponseFromStep(String stepName, String apiName) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getResponseFromStep", String.class, String.class);
            return (String) method.invoke(null, stepName, apiName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get response from step: " + stepName, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ApiContextEntities> getAllCallsFromCurrentStep() {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getAllCallsFromCurrentStep");
            return (List<ApiContextEntities>) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get all calls from current step", e);
        }
    }

    @Override
    public String extractFromStep(String stepName, String apiName, String path) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "extractFromStep", String.class, String.class, String.class);
            return (String) method.invoke(null, stepName, apiName, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract from step: " + stepName, e);
        }
    }

    // ==========================================================================
    // ORDER CORRELATION
    // ==========================================================================

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> getResponseByOrderNo(String orderNo) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getResponseByOrderNo", String.class);
            return (Optional<String>) method.invoke(null, orderNo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get response by order number: " + orderNo, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ApiContextEntities> getOrderLifecycle(String orderNo) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getOrderLifecycle", String.class);
            return (List<ApiContextEntities>) method.invoke(null, orderNo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get order lifecycle: " + orderNo, e);
        }
    }

    // ==========================================================================
    // LEGACY METHODS
    // ==========================================================================

    @Override
    @Deprecated
    public String getLastResponse(String apiName) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getLastResponse", String.class);
            return (String) method.invoke(null, apiName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get last response: " + apiName, e);
        }
    }

    @Override
    @Deprecated
    public String getResponseByCallIndex(String apiName, int callIndex) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getResponseByCallIndex", String.class, int.class);
            return (String) method.invoke(null, apiName, callIndex);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get response by call index: " + apiName, e);
        }
    }

    @Override
    @Deprecated
    @SuppressWarnings("unchecked")
    public List<String> getAllResponses(String apiName) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getAllResponses", String.class);
            return (List<String>) method.invoke(null, apiName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get all responses: " + apiName, e);
        }
    }

    // ==========================================================================
    // REQUEST & EXTRACTION
    // ==========================================================================

    @Override
    public String getLastRequest(String apiName) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getLastRequest", String.class);
            return (String) method.invoke(null, apiName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get last request: " + apiName, e);
        }
    }

    @Override
    public String extractFromLastResponse(String apiName, String path) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("extractFromLastResponse", String.class, String.class);
            return (String) method.invoke(null, apiName, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract from last response: " + apiName, e);
        }
    }

    @Override
    public String extractFromResponse(String apiName, int callIndex, String path) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "extractFromResponse", String.class, int.class, String.class);
            return (String) method.invoke(null, apiName, callIndex, path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract from response: " + apiName, e);
        }
    }

    // ==========================================================================
    // FILE OPERATIONS
    // ==========================================================================

    @Override
    public String generateStrFromRes(String filePath) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("GenerateStrFromRes", String.class);
            return (String) method.invoke(null, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void saveResponseAsXML(String filePath, String xmlStr) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
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
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("XMLXpathReader", String.class, String.class);
            return (String) method.invoke(null, filePath, xpath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read XPath: " + xpath, e);
        }
    }

    @Override
    public String getXmlRootName(String xmlStr) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getXmlRootName", String.class);
            return (String) method.invoke(null, xmlStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get XML root name", e);
        }
    }

    @Override
    public void editXmlSingleNode(String nodeName, String nodeValue, String filePath) {
        try {
            Class<?> clazz = getBaseActionsNewClass();
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
            Class<?> clazz = getBaseActionsNewClass();
            java.lang.reflect.Method method = clazz.getMethod("getDocumentFromXmlString", String.class);
            return (Document) method.invoke(null, xmlStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML to Document", e);
        }
    }
}