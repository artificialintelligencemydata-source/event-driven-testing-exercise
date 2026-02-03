package com.acuver.autwit.internal.helper;

import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.ports.ApiContextPort;
import com.acuver.autwit.core.ports.runtime.RuntimeContextPort;
import com.acuver.autwit.internal.config.FileReaderManager;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.config.XmlConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.annotation.PostConstruct;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * BaseActionsNew - Complete API call helper with database storage and retrieval.
 *
 * <h2>FEATURES</h2>
 * <ul>
 *   <li>Makes API calls using REST Assured</li>
 *   <li>Automatically stores metadata to database with scenario isolation</li>
 *   <li>Retrieves stored responses for assertions and reference</li>
 *   <li>Supports multiple calls to same API within one scenario</li>
 *   <li>Thread-safe for parallel execution</li>
 * </ul>
 *
 * <h2>USAGE - MAKING CALLS</h2>
 * <pre>
 * // API Call
 * Response response = BaseActionsNew.makeAPICall("getOrderDetails", "POST", inputXml, templateXml);
 *
 * // Service Call
 * Response response = BaseActionsNew.makeServiceCall("CreateOrder", "POST", inputXml);
 * </pre>
 *
 * <h2>USAGE - RETRIEVING RESPONSES</h2>
 * <pre>
 * // Get last response
 * String lastResponse = BaseActionsNew.getLastResponse("getOrder");
 *
 * // Get specific call's response
 * String response1 = BaseActionsNew.getResponseByCallIndex("getOrder", 0);
 * String response2 = BaseActionsNew.getResponseByCallIndex("getOrder", 1);
 *
 * // Extract value from last response
 * String orderNo = BaseActionsNew.extractFromLastResponse("getOrder", "/Order/@OrderNo");
 *
 * // Get all responses for an API
 * List<String> allResponses = BaseActionsNew.getAllResponses("getOrder");
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
public class BaseActionsNew {

    private static final Logger logger = LogManager.getLogger(BaseActionsNew.class);
    private static final String ERRORS = "Errors";

    // ✅ ThreadLocal for REST Assured config
    private static final ThreadLocal<RestAssuredConfig> threadLocalConfig = ThreadLocal.withInitial(() ->
            RestAssured.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000))
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                    .xmlConfig(XmlConfig.xmlConfig().disableLoadingOfExternalDtd()));

    // ✅ ThreadLocal for call index tracking (supports multiple calls to same API)
    /**
     * Thread-local storage for call index tracking.
     * Key format: stepKey + ":" + apiName
     * Value: call index counter
     *
     * <h3>CRITICAL CHANGE</h3>
     * - OLD: Tracked at scenario level (scenarioKey + apiName)
     * - NEW: Tracked at step level (stepKey + apiName)
     * - REASON: Prevents cross-step contamination in parallel execution
     */
    private static final ThreadLocal<Map<String, Integer>> callIndexTracker =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    @Autowired(required = false)
    @Qualifier("apiContextService")
    private ApiContextPort apiContextService;

    @Autowired(required = false)
    private RuntimeContextPort runtimeContext;

    private static BaseActionsNew instance;

    @PostConstruct
    public void init() {
        instance = this;
        if (apiContextService != null) {
            logger.info("BaseActionsNew initialized with database storage");
        } else {
            logger.warn("ApiContextService not available - API metadata will not be stored");
        }

        if (runtimeContext == null) {
            logger.warn("RuntimeContext not available - scenario isolation may not work properly");
        }
    }

    // ==========================================================================
    // API CALL METHODS (MAKING CALLS)
    // ==========================================================================

    /**
     * Make a generic API call.
     *
     * @param apiName           API name
     * @param httpMethod        HTTP method ("GET", "POST", "PUT", "DELETE", "PATCH")
     * @param apiInputXml       Input XML
     * @param outputTemplateXml Output template XML (can be empty)
     * @return Response
     */
    public static Response makeAPICall(String apiName, String httpMethod, String apiInputXml, String outputTemplateXml) throws IOException {
        return instance.executeCall(apiName, httpMethod, apiInputXml, outputTemplateXml, false);
    }

    /**
     * Make a service/flow call (IsFlow=Y).
     *
     * @param serviceName Service name
     * @param httpMethod  HTTP method
     * @param inputXml    Input XML
     * @return Response
     */
    public static Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws IOException {
        return instance.executeCall(serviceName, httpMethod, inputXml, "", true);
    }

    /**
     * Execute the API/Service call and store to database.
     */
    private Response executeCall(String name, String httpMethod, String inputXml, String outputTemplate, boolean isService) throws IOException {

        logger.info("══════════════════════════════════════════════════════════════");
        logger.info("{} to be invoked: {} | HTTP Method: {}", isService ? "Service" : "API", name, httpMethod);
        logger.info("══════════════════════════════════════════════════════════════");
        logger.debug("Request: {}\n{}", name, inputXml);

        // Build request
        String template = (outputTemplate != null) ? outputTemplate : "";
        String isFlowValue = isService ? "Y" : "N";

        // Special handling for PostConfirmShipmentMessage
        if (name.equals("PostConfirmShipmentMessage")) {
            isFlowValue = "Y";
        }

        RequestSpecification request = given().config(threadLocalConfig.get());
        setupCommonRequest(request, name, isFlowValue);
        request.queryParam("InteropApiData", inputXml);

        if (!template.isEmpty()) {
            request.queryParam("TemplateData", template);
            logger.debug("Output Template provided for: {}", name);
        }

        // Execute request
        Response response = executeHttpRequest(request, httpMethod);
        Response xmlResponse = response.then().assertThat().statusCode(200).and().extract().response();

        // Log response
        String responseBody = xmlResponse.asString();
        String rootName = getXmlRootName(responseBody);

        if (!ERRORS.equals(rootName)) {
            logger.info("Response: {}\n{}", name, responseBody);
        } else {
            logger.error("ERROR Response: {}\n{}", name, responseBody);
        }

        // ✅ Store to database with scenario isolation and call index
        storeToDatabase(name, httpMethod, inputXml, responseBody, template, isService);

        return xmlResponse;
    }

    /**
     * Execute HTTP request based on method.
     */
    private Response executeHttpRequest(RequestSpecification request, String httpMethod) {
        String endpoint = FileReaderManager.getInstance().getConfigReader().getEndPointUrl();

        return switch (httpMethod.toUpperCase()) {
            case "GET" -> request.get(endpoint);
            case "DELETE" -> request.delete(endpoint);
            case "PUT" -> request.put(endpoint);
            case "PATCH" -> request.patch(endpoint);
            default -> request.post(endpoint);
        };
    }

    /**
     * Setup common request parameters for Sterling API.
     */
    private static void setupCommonRequest(RequestSpecification request, String name, String isFlowValue) {
        String serviceName = isFlowValue.equals("Y") ? name : "";
        String apiName = isFlowValue.equals("Y") ? "" : name;

        var configReader = FileReaderManager.getInstance().getConfigReader();

        request.baseUri(configReader.getBaseUrl());
        request.queryParam("YFSEnvironment.progId", configReader.getProgId());
        request.queryParam("InteropApiName", name);
        request.queryParam("IsFlow", isFlowValue);
        request.queryParam("ServiceName", serviceName);
        request.queryParam("ApiName", apiName);
        request.queryParam("YFSEnvironment.userId", configReader.getUserId());
        request.queryParam("YFSEnvironment.password", configReader.getPassword());
        request.queryParam("YFSEnvironment.version", "");
        request.queryParam("YFSEnvironment.locale", "");
        request.header("Accept", "text/xml");
        request.contentType("text/xml");

        logger.debug("Request configured for: {}", name);
    }

    // ==========================================================================
    // DATABASE STORAGE (WITH SCENARIO ISOLATION)
    // ==========================================================================

    /**
     * Store API context to database with step-level isolation and call index tracking.
     *
     * <h2>STEP-LEVEL ISOLATION (v2.0)</h2>
     * - stepKey: Unique identifier for each step execution
     * - stepName: Human-readable step name (e.g., "I create an order")
     * - stepExecutionIndex: Rerun counter (0, 1, 2...)
     * - callIndex: API call index within the step
     *
     * <h2>BUSINESS ENTITY CORRELATION</h2>
     * - Extracts orderNo from response XML
     * - Extracts orderHeaderKey from response XML
     * - Enables cross-scenario order tracking
     */
    private void storeToDatabase(String apiName, String httpMethod, String request,
                                 String response, String template, boolean isService) {
        if (apiContextService == null) {
            logger.debug("ApiContextService not available - skipping database storage");
            return;
        }

        if (runtimeContext == null) {
            logger.warn("RuntimeContext not available - cannot store with scenario isolation");
            return;
        }

        try {
            // Get scenario context from RuntimeContextPort
            String scenarioKey = runtimeContext.get("scenarioKey");
            String testCaseId = runtimeContext.get("exampleKey");
            String exampleId = runtimeContext.get("testCaseId");

            // Get step context (set automatically by @BeforeMethod)
            String stepKey = runtimeContext.get("stepKey");
            String stepName = runtimeContext.get("stepName");
            Integer stepExecutionIndex = runtimeContext.get("currentStepExecutionIndex");


            // Validate required fields
            if (scenarioKey == null || scenarioKey.isBlank()) {
                logger.error("ScenarioKey is missing - cannot store API context. " +
                        "Ensure Hooks.setupScenarioContext() has been called.");
                return;
            }

            if (stepKey == null || stepKey.isBlank()) {
                logger.error("StepKey is missing - cannot store API context. " +
                        "Ensure Hooks.setupStepContext() has been called (@BeforeMethod).");
                return;
            }

            if (stepName == null || stepName.isBlank()) {
                logger.warn("StepName is missing - using 'Unknown Step'");
                stepName = "Unknown Step";
            }

            // Get and increment call index for this scenario + API combination
            //int callIndex = getAndIncrementCallIndex(scenarioKey, apiName);

            // Get and increment call index for this STEP + API combination
            // Note: Call index is now scoped to the step, not the scenario
            int callIndex = getAndIncrementCallIndex(stepKey, apiName);

            // Extract business entity correlation from response
            String orderNo = extractOrderNo(response);
            String orderHeaderKey = extractOrderHeaderKey(response);


            // Parse HTTP method
            ApiContextEntities.HttpMethod method;
            try {
                method = ApiContextEntities.HttpMethod.valueOf(httpMethod.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid HTTP method: {}, defaulting to POST", httpMethod);
                method = ApiContextEntities.HttpMethod.POST;
            }

            // Build API context entity
            ApiContextEntities context = ApiContextEntities.builder()
                    .scenarioKey(scenarioKey)
                    .testCaseId(testCaseId)
                    .exampleId(exampleId)
                    .stepKey(stepKey)
                    .stepName(stepName)
                    .stepExecutionIndex(stepExecutionIndex != null ? stepExecutionIndex : 0)
                    .apiName(apiName)
                    .callIndex(callIndex)
                    .httpMethod(method)
                    .apiTemplate(template != null && !template.isEmpty() ? template : "N/A")
                    .dataRepresentation("XML")
                    .requestPayload(request)
                    .responsePayload(response)
                    .isService(isService)
                    .serviceName(isService ? apiName : null)
                    .orderNo(orderNo)
                    .orderHeaderKey(orderHeaderKey)
                    .build();

            // Save to database
            apiContextService.save(context);

            logger.debug("Stored API context: step={}, stepName='{}', api={}, callIndex={}, orderNo={}, orderHeaderKey={}",
                    stepKey, stepName, apiName, callIndex, orderNo, orderHeaderKey);

        } catch (Exception e) {
            logger.error("Failed to store API context for {}: {}", apiName, e.getMessage(), e);
            // Don't rethrow - storage failure shouldn't break tests
        }
    }

    /**
     * Get and increment call index for scenario+API combination.
     */
//    private int getAndIncrementCallIndex(String scenarioKey, String apiName) {
//        String key = scenarioKey + ":" + apiName;
//        Map<String, Integer> tracker = callIndexTracker.get();
//
//        int currentIndex = tracker.getOrDefault(key, 0);
//        tracker.put(key, currentIndex + 1);
//
//        return currentIndex;
//    }
// ==========================================================================
// CALL INDEX TRACKING (Updated for Step Isolation)
// ==========================================================================
    /**
     * Get and increment call index for this step + API combination.
     *
     * @param stepKey Step execution identifier
     * @param apiName API name
     * @return Current call index (before increment)
     */
    private int getAndIncrementCallIndex(String stepKey, String apiName) {
        Map<String, Integer> tracker = callIndexTracker.get();
        String key = stepKey + ":" + apiName;

        int currentIndex = tracker.getOrDefault(key, 0);
        tracker.put(key, currentIndex + 1);

        logger.trace("Call index for step={}, api={}: {}", stepKey, apiName, currentIndex);
        return currentIndex;
    }

    /**
     * Clear call index tracker (called from Hooks.cleanupScenarioContext).
     */
    public static void clearCallIndexTracker() {
        callIndexTracker.remove();
        logger.trace("Call index tracker cleared");
    }
    // ==========================================================================
    // RESPONSE RETRIEVAL METHODS
    // ==========================================================================

    /**
     * Get the LAST response for an API in current scenario.
     *
     * @param apiName API name (e.g., "getOrder")
     * @return Response payload or null if not found
     */
    public static String getLastResponse(String apiName) {
        return instance.getLastResponseInternal(apiName);
    }

    private String getLastResponseInternal(String apiName) {
        if (runtimeContext == null || apiContextService == null) {
            logger.error("Services not available");
            return null;
        }

        String scenarioKey = runtimeContext.get("scenarioKey");
        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.error("scenarioKey not available - cannot retrieve response");
            return null;
        }

        // Get current call index for this API
        int lastCallIndex = getCurrentCallIndex(scenarioKey, apiName) - 1;

        if (lastCallIndex < 0) {
            logger.warn("No calls to {} found in scenario {}", apiName, scenarioKey);
            return null;
        }

        return getResponseByCallIndexInternal(apiName, lastCallIndex);
    }

    /**
     * Get response for specific call index.
     *
     * @param apiName   API name
     * @param callIndex Call index (0-based)
     * @return Response payload or null if not found
     */
    public static String getResponseByCallIndex(String apiName, int callIndex) {
        return instance.getResponseByCallIndexInternal(apiName, callIndex);
    }

    private String getResponseByCallIndexInternal(String apiName, int callIndex) {
        if (runtimeContext == null || apiContextService == null) {
            logger.error("Services not available");
            return null;
        }

        String scenarioKey = runtimeContext.get("scenarioKey");
        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.error("scenarioKey not available");
            return null;
        }

        Optional<ApiContextEntities> context =
                apiContextService.findByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, callIndex);

        if (context.isPresent()) {
            logger.debug("Retrieved response: scenario={}, api={}, callIndex={}",
                    scenarioKey, apiName, callIndex);
            return context.get().getResponsePayload();
        } else {
            logger.warn("No response found: scenario={}, api={}, callIndex={}",
                    scenarioKey, apiName, callIndex);
            return null;
        }
    }

    /**
     * Get ALL responses for an API in current scenario.
     *
     * @param apiName API name
     * @return List of response payloads (ordered by callIndex)
     */
    public static List<String> getAllResponses(String apiName) {
        return instance.getAllResponsesInternal(apiName);
    }

    private List<String> getAllResponsesInternal(String apiName) {
        if (runtimeContext == null || apiContextService == null) {
            logger.error("Services not available");
            return List.of();
        }

        String scenarioKey = runtimeContext.get("scenarioKey");
        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.error("scenarioKey not available");
            return List.of();
        }

        List<ApiContextEntities> contexts =
                apiContextService.findAllByScenarioKeyAndApiName(scenarioKey, apiName);

        return contexts.stream()
                .map(ApiContextEntities::getResponsePayload)
                .toList();
    }

    /**
     * Get request payload for last call.
     *
     * @param apiName API name
     * @return Request payload or null if not found
     */
    public static String getLastRequest(String apiName) {
        return instance.getLastRequestInternal(apiName);
    }

    private String getLastRequestInternal(String apiName) {
        if (runtimeContext == null || apiContextService == null) {
            logger.error("Services not available");
            return null;
        }

        String scenarioKey = runtimeContext.get("scenarioKey");
        if (scenarioKey == null || scenarioKey.isBlank()) {
            logger.error("scenarioKey not available");
            return null;
        }

        int lastCallIndex = getCurrentCallIndex(scenarioKey, apiName) - 1;

        if (lastCallIndex < 0) {
            return null;
        }

        Optional<ApiContextEntities> context =
                apiContextService.findByScenarioKeyAndApiNameAndCallIndex(scenarioKey, apiName, lastCallIndex);

        return context.map(ApiContextEntities::getRequestPayload).orElse(null);
    }

    /**
     * Extract value from last response using XPath.
     *
     * @param apiName API name
     * @param xpath   XPath expression
     * @return Extracted value or null
     */
    public static String extractFromLastResponse(String apiName, String xpath) {
        return instance.extractFromLastResponseInternal(apiName, xpath);
    }

    private String extractFromLastResponseInternal(String apiName, String xpath) {
        String response = getLastResponseInternal(apiName);

        if (response == null || response.isBlank()) {
            logger.warn("No response found for {}", apiName);
            return null;
        }

        return extractXPathValue(response, xpath);
    }

    /**
     * Extract value from specific call's response using XPath.
     *
     * @param apiName   API name
     * @param callIndex Call index
     * @param xpath     XPath expression
     * @return Extracted value or null
     */
    public static String extractFromResponse(String apiName, int callIndex, String xpath) {
        return instance.extractFromResponseInternal(apiName, callIndex, xpath);
    }

    private String extractFromResponseInternal(String apiName, int callIndex, String xpath) {
        String response = getResponseByCallIndexInternal(apiName, callIndex);

        if (response == null || response.isBlank()) {
            logger.warn("No response found for {} at callIndex {}", apiName, callIndex);
            return null;
        }

        return extractXPathValue(response, xpath);
    }

    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================

    private int getCurrentCallIndex(String scenarioKey, String apiName) {
        String key = scenarioKey + ":" + apiName;
        return callIndexTracker.get().getOrDefault(key, 0);
    }

    private String extractXPathValue(String xmlString, String xpath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpathObj = xpathFactory.newXPath();
            XPathExpression expr = xpathObj.compile(xpath);

            Node node = (Node) expr.evaluate(document, XPathConstants.NODE);

            if (node == null) {
                logger.warn("XPath did not match any element: {}", xpath);
                return null;
            }

            String value = node.getTextContent().trim();
            return value.isEmpty() ? null : value;

        } catch (Exception e) {
            logger.error("Failed to extract XPath value: {}", xpath, e);
            return null;
        }
    }

/// ==========================================================================
// BUSINESS ENTITY EXTRACTION (XML + JSON Support)
// ==========================================================================

    /**
     * Extract order number from response (supports XML and JSON).
     * Looks for common patterns in both formats.
     *
     * @param response Response string (XML or JSON)
     * @return Order number or null if not found
     */
    private String extractOrderNo(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        try {
            // Detect format
            String trimmed = response.trim();
            boolean isJson = trimmed.startsWith("{") || trimmed.startsWith("[");

            if (isJson) {
                return extractOrderNoFromJson(response);
            } else {
                return extractOrderNoFromXml(response);
            }

        } catch (Exception e) {
            logger.trace("Failed to extract OrderNo from response: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extract order number from XML response.
     *
     * @param xmlResponse XML response string
     * @return Order number or null if not found
     */
    private String extractOrderNoFromXml(String xmlResponse) {
        // Pattern 1: <OrderNo>ORD-12345</OrderNo>
        Pattern pattern1 = Pattern.compile("<OrderNo>([^<]+)</OrderNo>", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(xmlResponse);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // Pattern 2: <order_no>ORD-12345</order_no>
        Pattern pattern2 = Pattern.compile("<order_no>([^<]+)</order_no>", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(xmlResponse);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        // Pattern 3: OrderNo="ORD-12345"
        Pattern pattern3 = Pattern.compile("OrderNo=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(xmlResponse);
        if (matcher3.find()) {
            return matcher3.group(1).trim();
        }

        return null;
    }

    /**
     * Extract order number from JSON response.
     *
     * @param jsonResponse JSON response string
     * @return Order number or null if not found
     */
    private String extractOrderNoFromJson(String jsonResponse) {
        // Pattern 1: "OrderNo": "ORD-12345"
        Pattern pattern1 = Pattern.compile("\"OrderNo\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(jsonResponse);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // Pattern 2: "orderNo": "ORD-12345"
        Pattern pattern2 = Pattern.compile("\"orderNo\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(jsonResponse);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        // Pattern 3: "order_no": "ORD-12345"
        Pattern pattern3 = Pattern.compile("\"order_no\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(jsonResponse);
        if (matcher3.find()) {
            return matcher3.group(1).trim();
        }

        // Pattern 4: "order-no": "ORD-12345"
        Pattern pattern4 = Pattern.compile("\"order-no\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher4 = pattern4.matcher(jsonResponse);
        if (matcher4.find()) {
            return matcher4.group(1).trim();
        }

        return null;
    }

    /**
     * Extract order header key from response (supports XML and JSON).
     * Looks for common patterns in both formats.
     *
     * @param response Response string (XML or JSON)
     * @return Order header key or null if not found
     */
    private String extractOrderHeaderKey(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        try {
            // Detect format
            String trimmed = response.trim();
            boolean isJson = trimmed.startsWith("{") || trimmed.startsWith("[");

            if (isJson) {
                return extractOrderHeaderKeyFromJson(response);
            } else {
                return extractOrderHeaderKeyFromXml(response);
            }

        } catch (Exception e) {
            logger.trace("Failed to extract OrderHeaderKey from response: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extract order header key from XML response.
     *
     * @param xmlResponse XML response string
     * @return Order header key or null if not found
     */
    private String extractOrderHeaderKeyFromXml(String xmlResponse) {
        // Pattern 1: <OrderHeaderKey>202501280001</OrderHeaderKey>
        Pattern pattern1 = Pattern.compile("<OrderHeaderKey>([^<]+)</OrderHeaderKey>", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(xmlResponse);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // Pattern 2: <order_header_key>202501280001</order_header_key>
        Pattern pattern2 = Pattern.compile("<order_header_key>([^<]+)</order_header_key>", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(xmlResponse);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        // Pattern 3: OrderHeaderKey="202501280001"
        Pattern pattern3 = Pattern.compile("OrderHeaderKey=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(xmlResponse);
        if (matcher3.find()) {
            return matcher3.group(1).trim();
        }

        return null;
    }

    /**
     * Extract order header key from JSON response.
     *
     * @param jsonResponse JSON response string
     * @return Order header key or null if not found
     */
    private String extractOrderHeaderKeyFromJson(String jsonResponse) {
        // Pattern 1: "OrderHeaderKey": "202501280001"
        Pattern pattern1 = Pattern.compile("\"OrderHeaderKey\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(jsonResponse);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // Pattern 2: "orderHeaderKey": "202501280001"
        Pattern pattern2 = Pattern.compile("\"orderHeaderKey\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(jsonResponse);
        if (matcher2.find()) {
            return matcher2.group(1).trim();
        }

        // Pattern 3: "order_header_key": "202501280001"
        Pattern pattern3 = Pattern.compile("\"order_header_key\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pattern3.matcher(jsonResponse);
        if (matcher3.find()) {
            return matcher3.group(1).trim();
        }

        // Pattern 4: "order-header-key": "202501280001"
        Pattern pattern4 = Pattern.compile("\"order-header-key\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher4 = pattern4.matcher(jsonResponse);
        if (matcher4.find()) {
            return matcher4.group(1).trim();
        }

        return null;
    }
    // ==========================================================================
    // XML UTILITY METHODS (EXISTING)
    // ==========================================================================

    /**
     * Read file and return as string.
     */
    public static String GenerateStrFromRes(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            logger.error("Error reading file: {}", path, e);
            return exceptionsToString(e);
        }
    }

    /**
     * Save XML string to file.
     */
    public static void SaveResponseAsXML(String responseFilePath, String xmlStr) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(responseFilePath));
            transformer.transform(source, result);
            result.getOutputStream().close();

            logger.debug("Saved XML to: {}", responseFilePath);
        } catch (Exception e) {
            logger.error("Error saving XML to {}: {}", responseFilePath, e.getMessage());
        }
    }

    /**
     * Read XML value using XPath from file.
     */
    public static String XMLXpathReader(String filepath, String Xpath) {
        try {
            File file = new File(filepath);
            if (!file.exists()) {
                logger.error("File not found: {}", filepath);
                return null;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new FileInputStream(file));

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            XPathExpression expr = xpath.compile(Xpath);
            Node node = (Node) expr.evaluate(document, XPathConstants.NODE);

            if (node == null) {
                logger.error("XPath did not match any element: {}", Xpath);
                return null;
            }

            String value = node.getTextContent().trim();
            return value.isEmpty() ? null : value;
        } catch (Exception e) {
            logger.error("Error reading XML using XPath: {}", Xpath, e);
        }
        return null;
    }

    /**
     * Get root element name from XML string.
     */
    public static String getXmlRootName(String xmlStr) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlStr)));
            return document.getDocumentElement().getNodeName();
        } catch (Exception e) {
            logger.error("Error getting XML root name: {}", e.getMessage());
            return exceptionsToString(e);
        }
    }

    /**
     * Edit single node attribute in XML file.
     */
    public static void editXmlSingleNode(String nodeName, String nodeValue, String filepath) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);

            Node company = doc.getFirstChild();
            NamedNodeMap attr1 = company.getAttributes();
            Node orderDetails = attr1.getNamedItem(nodeName);
            orderDetails.setTextContent(nodeValue);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filepath));
            transformer.transform(source, result);

            logger.info("Updated {} = {} in {}", nodeName, nodeValue, filepath);
        } catch (ParserConfigurationException | TransformerException | IOException | SAXException e) {
            logger.error("Error editing XML: {}", e.getMessage());
        }
    }

    /**
     * Get Document from XML string.
     */
    public static Document getDocumentFromXmlString(String xmlStr) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlStr)));
        } catch (Exception e) {
            logger.error("Error parsing XML to Document: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert exception to string.
     */
    public static String exceptionsToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}