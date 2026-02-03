package com.acuver.autwit.client.sdk;

import com.acuver.autwit.core.domain.ApiCallStatistics;
import com.acuver.autwit.core.domain.ApiContextEntities;
import io.restassured.response.Response;
import org.testng.asserts.SoftAssert;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AUTWIT Client SDK - Main Facade Interface.
 *
 * @author AUTWIT Framework
 * @version 2.0.0
 * @since 1.0.0
 */
public interface Autwit {

    // ==========================================================================
    // EVENT EXPECTATION
    // ==========================================================================

    interface EventExpectation {
        void assertSatisfied();
        EventExpectation assertPayload(java.util.function.Function<Map<String, Object>, Boolean> validator);
        EventExpectation withPayloadField(String fieldName, String expectedValue);
        EventExpectation withPayloadField(String fieldName, java.util.function.Predicate<String> validator);
    }

    // ==========================================================================
    // STEP STATUS
    // ==========================================================================

    interface ScenarioStepStatus {
        void markStepSuccess();
        void markStepFailed(String reason);
        void markStepSkipped(String reason);
        boolean skipIfAlreadySuccessful();
        Map<String, String> getStepData();
    }

    // ==========================================================================
    // CONTEXT ACCESSOR (MAIN ENTRY)
    // ==========================================================================

    interface ContextAccessor {

        void setCurrentStep(String stepName);
        <T> void set(String key, T value);
        <T> T get(String key);
        void setOrderId(String orderId);

        BaseActions baseActions();
        BaseActionsNew baseActionsNew();
        SterlingApi sterling();
        XmlUtils xml();
        ConfigReader config();
        SoftAssertions assertions();

        // ======================================================================
        // BASE ACTIONS (LEGACY)
        // ======================================================================

        interface BaseActions {
            Response makeAPICall(String apiName, String httpMethod, String inputXml, String outputTemplate) throws Exception;

            default Response makeAPICall(String apiName, String httpMethod, String inputXml) throws Exception {
                return makeAPICall(apiName, httpMethod, inputXml, "");
            }

            default Response post(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "POST", inputXml, "");
            }

            default Response post(String apiName, String inputXml, String outputTemplate) throws Exception {
                return makeAPICall(apiName, "POST", inputXml, outputTemplate);
            }

            default Response get(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "GET", inputXml, "");
            }

            default Response delete(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "DELETE", inputXml, "");
            }

            Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws Exception;

            String generateStrFromRes(String filePath);
            void saveResponseAsXML(String filePath, String xmlStr);

            String xmlXpathReader(String filePath, String xpath);
            String getXmlRootName(String xmlStr);
            void editXmlSingleNode(String nodeName, String nodeValue, String filePath);
            Document getDocumentFromXmlString(String xmlStr);
        }

        // ======================================================================
        // BASE ACTIONS NEW (v2.0)
        // ======================================================================

        interface BaseActionsNew {

            Response makeAPICall(String apiName, String httpMethod,
                                 String inputXml, String outputTemplate) throws Exception;

            default Response makeAPICall(String apiName, String httpMethod, String inputXml) throws Exception {
                return makeAPICall(apiName, httpMethod, inputXml, "");
            }

            Response makeAPICallWithTemplate(String apiName, Map<String, String> parameters) throws Exception;

            Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws Exception;

            default Response post(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "POST", inputXml, "");
            }

            default Response get(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "GET", inputXml, "");
            }

            default Response delete(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "DELETE", inputXml, "");
            }

            // Step-level retrieval
            String getLastResponseFromCurrentStep(String apiName);
            String getResponseFromStep(String stepName, String apiName);
            List<ApiContextEntities> getAllCallsFromCurrentStep();
            String extractFromStep(String stepName, String apiName, String path);

            // Order correlation
            Optional<String> getResponseByOrderNo(String orderNo);
            List<ApiContextEntities> getOrderLifecycle(String orderNo);

            // Legacy
            @Deprecated String getLastResponse(String apiName);
            @Deprecated String getResponseByCallIndex(String apiName, int callIndex);
            @Deprecated List<String> getAllResponses(String apiName);

            String getLastRequest(String apiName);
            String extractFromLastResponse(String apiName, String path);
            String extractFromResponse(String apiName, int callIndex, String path);

            String generateStrFromRes(String filePath);
            void saveResponseAsXML(String filePath, String xmlStr);
            String xmlXpathReader(String filePath, String xpath);
            String getXmlRootName(String xmlStr);
            void editXmlSingleNode(String nodeName, String nodeValue, String filePath);
            Document getDocumentFromXmlString(String xmlStr);
        }

        // ======================================================================
        // API CONTEXT (v2.0)
        // ======================================================================

        interface ApiContext {
            Optional<String> getLastResponseFromStep(String stepKey, String apiName);
            List<String> getAllResponsesFromStep(String stepKey);
            Optional<String> getResponseFromPreviousStep(String scenarioKey, String stepName, String apiName);
            boolean hasCalledApi(String stepKey, String apiName);
            long getCallCount(String stepKey);

            Optional<String> getResponseByOrderNo(String orderNo);
            List<String> getAllResponsesForOrder(String orderNo);
            Optional<String> getOrderNoFromLastCall(String stepKey, String apiName);
            List<ApiContextEntities> trackOrderLifecycle(String orderNo);

            Optional<String> getLastResponseFromScenario(String scenarioKey, String apiName);
            List<String> getAllResponsesFromScenario(String scenarioKey);

            ApiContextEntities save(ApiContextEntities context);
            Optional<ApiContextEntities> getContext(String stepKey, String apiName);
            List<ApiContextEntities> getAllContexts(String stepKey);
        }

        // ======================================================================
        // API TEMPLATE (v2.0)
        // ======================================================================

        interface ApiTemplate {
            ApiTemplate createTemplate(String apiName, String httpMethod,
                                       String endpointTemplate, String requestTemplate,
                                       String dataRepresentation);

            ApiTemplate createServiceTemplate(String serviceName, String httpMethod,
                                              String endpointTemplate, String requestTemplate,
                                              String dataRepresentation);

            Optional<ApiTemplate> getTemplate(String apiName);
            boolean templateExists(String apiName);
            ApiTemplate updateTemplate(ApiTemplate template);
            void deleteTemplate(String apiName);

            List<ApiTemplate> getAllTemplates();
            List<ApiTemplate> getApiTemplates();
            List<ApiTemplate> getServiceTemplates();
            List<ApiTemplate> getTemplatesByHttpMethod(String httpMethod);

            Optional<String> buildRequest(String apiName, Map<String, String> parameters);
            Optional<String> buildRequest(String apiName, String paramName, String paramValue);
            Optional<String> buildRequest(String apiName,
                                          String p1, String v1,
                                          String p2, String v2);

            long getTemplateCount();
            long getApiTemplateCount();
            long getServiceTemplateCount();

            boolean validateTemplate(String apiName, Map<String, String> parameters);
            List<String> getMissingParameters(String apiName, Map<String, String> parameters);

            List<ApiTemplate> createTemplates(List<ApiTemplate> templates);
            void deleteAllTemplates();
        }

        // ======================================================================
        // STERLING API
        // ======================================================================

        interface SterlingApi {
            // Order Management
            void createOrder(String filePath) throws Exception;
            void scheduleOrder() throws Exception;
            void releaseOrder() throws Exception;
            void changeOrder(String filePath) throws Exception;
            int getOrderReleaseList() throws Exception;
            void getOrderList() throws Exception;
            void getOrderDetails() throws Exception;
            void getOrderHoldTypeList() throws Exception;
            void changeOrderStatus(String filePath) throws Exception;

            // Shipment Management
            void createShipment(String filePath) throws Exception;
            void confirmShipment(String filePath, String apiName) throws Exception;
            void changeShipment(String filePath) throws Exception;
            void changeShipmentStatus(String filePath) throws Exception;
            void getShipmentListForOrder() throws Exception;
            void getShipmentDetails(String shipmentKey) throws Exception;
            void createShipmentInvoice() throws Exception;

            // Inventory Management
            void getATP() throws Exception;
            void adjustInventory() throws Exception;
            void reserveItemInventory() throws Exception;
            void getReservation() throws Exception;
            void cancelReservation() throws Exception;
            void getItemList(String itemId) throws Exception;

            // Receipt Management
            void receiveOrder(String filePath) throws Exception;
            void startReceipt() throws Exception;
            void closeReceipt() throws Exception;

            // Release Management
            void changeRelease() throws Exception;

            // Return Order
            void authorizeReturnOrder() throws Exception;
            void confirmDraftOrder() throws Exception;
            void createOrderInvoice(String filePath) throws Exception;
            void getOrderInvoiceDetailList() throws Exception;
        }

        // ======================================================================
        // XML UTILS
        // ======================================================================

        interface XmlUtils {
            void editXmlFile(String tagName, int nodeCount, String attrName, String attrValue, String filePath);
            void updateOrderNumber(String filePath);
            int orderLineCount(String filePath);
            int getNumberOfNodes(String filePath, String xpathExpression) throws Exception;
            void validateAndSaveXmlResponse(String filePath, String response);
            String getXMLString(Document document) throws Exception;
        }

        // ======================================================================
        // CONFIG READER
        // ======================================================================

        interface ConfigReader {
            // URL Configuration
            String getBaseUrl();
            String getEndPointUrl();

            // Path Configuration
            String getInputXmlPath();
            String getResponseXmlPath();
            String getApiTemplatesXmlPath();
            String getValidationExcelsPath();
            String getTransferOrderInputXmls();
            String getPurchaseOrderXmls();

            // Authentication
            String getUserId();
            String getPassword();
            String getProgId();

            // Timing
            int getDaysForReqDeliveryDate();
            long getImplicitlyWait();

            // JIRA Integration
            String getJiraURL();
            String getJiraUserName();
            String getJiraAPIToken();
            String getJiraProjectName();
            boolean isTicketNeedsToBeLogged();
        }

        // ======================================================================
        // SOFT ASSERTIONS
        // ======================================================================

        interface SoftAssertions {
            SoftAssert getSoftAssert();
            void assertAll();
        }
    }

    // ==========================================================================
    // ROOT FACADE
    // ==========================================================================

    EventExpectation expectEvent(String orderId, String eventType);
    void pauseUntilEvent(String orderId, String eventType);
    ScenarioStepStatus step();
    ContextAccessor context();
}