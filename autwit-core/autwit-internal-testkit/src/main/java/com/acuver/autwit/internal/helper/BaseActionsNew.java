package com.acuver.autwit.internal.helper;

import com.acuver.autwit.core.domain.ApiContextEntities;
import com.acuver.autwit.core.ports.ApiContextPort;
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

import static io.restassured.RestAssured.given;

/**
 * BaseActionsNew - Simplified API call helper with database storage.
 *
 * <h2>PURPOSE</h2>
 * Makes API calls using REST Assured and automatically stores metadata to database.
 *
 * <h2>CONFIGURATION</h2>
 * <pre>
 * autwit:
 *   database:
 *     type: postgresql  # h2, postgresql, mongodb
 * </pre>
 *
 * <h2>USAGE</h2>
 * <pre>
 * // API Call
 * Response response = BaseActionsNew.makeAPICall("getOrderDetails", "POST", inputXml, templateXml);
 *
 * // Service Call
 * Response response = BaseActionsNew.makeServiceCall("CreateOrder", "POST", inputXml);
 * </pre>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
@Component
public class BaseActionsNew {

    private static final Logger logger = LogManager.getLogger(BaseActionsNew.class);
    private static final String ERRORS = "Errors";

    private static final ThreadLocal<RestAssuredConfig> threadLocalConfig = ThreadLocal.withInitial(() ->
            RestAssured.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000))
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                    .xmlConfig(XmlConfig.xmlConfig().disableLoadingOfExternalDtd()));

    @Autowired(required = false)
    @Qualifier("apiContextService")
    private ApiContextPort apiContextService;

    private static BaseActionsNew instance;

    @PostConstruct
    public void init() {
        instance = this;
        if (apiContextService != null) {
            logger.info("BaseActionsNew initialized with database storage");
        } else {
            logger.warn("ApiContextService not available - API metadata will not be stored");
        }
    }

    // ==========================================================================
    // API CALL METHODS
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

        // Store to database
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
    // DATABASE STORAGE
    // ==========================================================================

    /**
     * Store API context to database.
     */
    private void storeToDatabase(String apiName, String httpMethod, String request,
                                 String response, String template, boolean isService) {
        if (apiContextService == null) {
            logger.debug("ApiContextService not available - skipping database storage");
            return;
        }

        try {
            ApiContextEntities.HttpMethod method = ApiContextEntities.HttpMethod.valueOf(httpMethod.toUpperCase());

            ApiContextEntities context = ApiContextEntities.builder()
                    .apiName(apiName)
                    .httpMethod(method)
                    .apiTemplate(template != null && !template.isEmpty() ? template : "N/A")
                    .dataRepresentation("XML")
                    .requestPayload(request)
                    .responsePayload(response)
                    .isService(isService)
                    .serviceName(isService ? apiName : null)
                    .build();

            apiContextService.save(context);
            logger.debug("Stored API context to database: {}", apiName);

        } catch (Exception e) {
            logger.error("Failed to store API context for {}: {}", apiName, e.getMessage(), e);
        }
    }

    // ==========================================================================
    // XML UTILITY METHODS
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
     * Read XML value using XPath.
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