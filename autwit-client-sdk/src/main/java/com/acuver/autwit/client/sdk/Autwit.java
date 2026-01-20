package com.acuver.autwit.client.sdk;

import io.restassured.response.Response;
import org.testng.asserts.SoftAssert;
import org.w3c.dom.Document;

import java.util.Map;

/**
 * AUTWIT Client SDK - Main Facade Interface.
 *
 * <h2>STRUCTURE</h2>
 * <pre>
 * autwit.context()
 *     ├── .baseActions()   → BaseActions (generic API calls, XML utils)
 *     ├── .sterling()      → SterlingApiCalls (Sterling OMS specific)
 *     ├── .xml()           → XmlUpdater utilities
 *     ├── .config()        → ConfigFileReader
 *     └── .assertions()    → SoftAssertUtils
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
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
    // CONTEXT ACCESSOR
    // ==========================================================================

    interface ContextAccessor {

        void setCurrentStep(String stepName);

        <T> void set(String key, T value);
        <T> T get(String key);
        void setOrderId(String orderId);

        BaseActions baseActions();
        SterlingApi sterling();
        XmlUtils xml();
        ConfigReader config();
        SoftAssertions assertions();

        // ======================================================================
        // BASE ACTIONS - Generic API Calls & Utilities
        // ======================================================================

        interface BaseActions {

            // ==================================================================
            // FLEXIBLE API CALL METHODS
            // ==================================================================

            /**
             * Make a Sterling API call with all parameters.
             *
             * @param apiName         API name (e.g., "createOrder")
             * @param httpMethod      HTTP method ("GET", "POST", "DELETE")
             * @param inputXml        Input XML string
             * @param outputTemplate  Output template XML (can be null/empty)
             * @return Response object
             */
            Response makeAPICall(String apiName, String httpMethod, String inputXml, String outputTemplate) throws Exception;

            /**
             * Make a Sterling API call without output template.
             *
             * @param apiName    API name
             * @param httpMethod HTTP method
             * @param inputXml   Input XML string
             * @return Response object
             */
            default Response makeAPICall(String apiName, String httpMethod, String inputXml) throws Exception {
                return makeAPICall(apiName, httpMethod, inputXml, "");
            }

            /**
             * Make a POST API call (most common).
             *
             * @param apiName  API name
             * @param inputXml Input XML string
             * @return Response object
             */
            default Response post(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "POST", inputXml, "");
            }

            /**
             * Make a POST API call with output template.
             *
             * @param apiName        API name
             * @param inputXml       Input XML string
             * @param outputTemplate Output template XML
             * @return Response object
             */
            default Response post(String apiName, String inputXml, String outputTemplate) throws Exception {
                return makeAPICall(apiName, "POST", inputXml, outputTemplate);
            }

            /**
             * Make a GET API call.
             *
             * @param apiName  API name
             * @param inputXml Input XML string
             * @return Response object
             */
            default Response get(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "GET", inputXml, "");
            }

            /**
             * Make a GET API call with output template.
             *
             * @param apiName        API name
             * @param inputXml       Input XML string
             * @param outputTemplate Output template XML
             * @return Response object
             */
            default Response get(String apiName, String inputXml, String outputTemplate) throws Exception {
                return makeAPICall(apiName, "GET", inputXml, outputTemplate);
            }

            /**
             * Make a DELETE API call.
             *
             * @param apiName  API name
             * @param inputXml Input XML string
             * @return Response object
             */
            default Response delete(String apiName, String inputXml) throws Exception {
                return makeAPICall(apiName, "DELETE", inputXml, "");
            }

            /**
             * Make a service/flow call (IsFlow=Y).
             *
             * @param serviceName Service name
             * @param httpMethod  HTTP method
             * @param inputXml    Input XML string
             * @return Response object
             */
            Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws Exception;

            // ==================================================================
            // FILE OPERATIONS
            // ==================================================================

            /**
             * Read file and return as string.
             */
            String generateStrFromRes(String filePath);

            /**
             * Save XML string to file.
             */
            void saveResponseAsXML(String filePath, String xmlStr);

            // ==================================================================
            // XML OPERATIONS
            // ==================================================================

            /**
             * Read XML value using XPath.
             */
            String xmlXpathReader(String filePath, String xpath);

            /**
             * Get root element name from XML string.
             */
            String getXmlRootName(String xmlStr);

            /**
             * Edit single node attribute in XML file.
             */
            void editXmlSingleNode(String nodeName, String nodeValue, String filePath);

            /**
             * Get Document from XML string.
             */
            Document getDocumentFromXmlString(String xmlStr);
        }

        // ======================================================================
        // STERLING API - Sterling OMS Specific
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
        // XML UTILITIES
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
        // CONFIGURATION
        // ======================================================================

        interface ConfigReader {
            String getBaseUrl();
            String getEndPointUrl();
            String getInputXmlPath();
            String getResponseXmlPath();
            String getApiTemplatesXmlPath();
            String getUserId();
            String getPassword();
            String getProgId();
            String getValidationExcelsPath();
            String getTransferOrderInputXmls();
            String getPurchaseOrderXmls();
            int getDaysForReqDeliveryDate();
            long getImplicitlyWait();
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
    // FACADE METHODS
    // ==========================================================================

    EventExpectation expectEvent(String orderId, String eventType);
    ScenarioStepStatus step();
    ContextAccessor context();
}