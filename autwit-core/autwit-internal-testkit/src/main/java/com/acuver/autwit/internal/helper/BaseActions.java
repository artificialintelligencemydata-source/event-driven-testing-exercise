package com.acuver.autwit.internal.helper;

import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.reporting.AllureAttachmentUtils;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.config.XmlConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
 * BaseActions - Generic API calls and XML utilities.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-internal
 * Package: com.acuver.autwit.internal.api
 *
 * <h2>KEY METHODS</h2>
 * <ul>
 *   <li>{@link #makeAPICall(String, String, String, String)} - Generic API call</li>
 *   <li>{@link #makeServiceCall(String, String, String)} - Service/Flow call</li>
 *   <li>{@link #XMLXpathReader(String, String)} - Read XML using XPath</li>
 *   <li>{@link #SaveResponseAsXML(String, String)} - Save response to file</li>
 * </ul>
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class BaseActions {

    public static final Logger logger = LogManager.getLogger(BaseActions.class);

    private static final ThreadLocal<RestAssuredConfig> threadLocalConfig = ThreadLocal.withInitial(() ->
            RestAssured.config()
                    .httpClient(HttpClientConfig.httpClientConfig()
                            .setParam(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000))
                    .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                    .xmlConfig(XmlConfig.xmlConfig().disableLoadingOfExternalDtd()));

    private static final String ERRORS = "Errors";

    // ==========================================================================
    // API CALL - Main Method
    // ==========================================================================

    /**
     * Make a generic Sterling API call.
     * This is the core method for all Sterling API interactions.
     *
     * @param apiName           API name (e.g., "createOrder", "getOrderDetails")
     * @param httpMethod        HTTP method ("GET", "POST", "DELETE")
     * @param apiInputXml       Input XML string
     * @param outputTemplateXml Output template XML (can be null/empty)
     * @return Response object
     */
    public static Response makeAPICall(String apiName, String httpMethod, String apiInputXml, String outputTemplateXml) throws IOException {
        return makeCall(apiName, httpMethod, apiInputXml, outputTemplateXml, false);
    }

    /**
     * Make a service/flow call (IsFlow=Y).
     *
     * @param serviceName Service name
     * @param httpMethod  HTTP method
     * @param inputXml    Input XML string
     * @return Response object
     */
    public static Response makeServiceCall(String serviceName, String httpMethod, String inputXml) throws IOException {
        return makeCall(serviceName, httpMethod, inputXml, "", true);
    }

    /**
     * Internal method to make API or Service call.
     *
     * @param apiOrServiceName API or Service name
     * @param httpMethod       HTTP method
     * @param inputXml         Input XML string
     * @param outputTemplate   Output template XML
     * @param isFlow           true for Service/Flow call, false for API call
     * @return Response object
     */
    private static Response makeCall(String apiOrServiceName, String httpMethod, String inputXml,
                                     String outputTemplate, boolean isFlow) throws IOException {

        String template = (outputTemplate != null) ? outputTemplate : "";
        StringBuilder logMessage = new StringBuilder();

        logger.info("══════════════════════════════════════════════════════════════");
        logger.info("{} to be invoked: {} | HTTP Method: {}", isFlow ? "Service" : "API", apiOrServiceName, httpMethod);
        logger.info("══════════════════════════════════════════════════════════════");

        logMessage.append("Request: ").append(apiOrServiceName).append("\n").append(inputXml);
        logger.debug(logMessage.toString());

        // Attach to Allure report
        try {
//            AllureManager.attachXmlToReport(apiOrServiceName + " Input", inputXml);
        } catch (Exception e) {
            logger.trace("Allure attachment skipped: {}", e.getMessage());
        }

        // Build request
        RequestSpecification request = given().config(threadLocalConfig.get());

        // Special handling for PostConfirmShipmentMessage
        String isFlowValue = isFlow ? "Y" : "N";
        if (apiOrServiceName.equals("PostConfirmShipmentMessage")) {
            isFlowValue = "Y";
        }

        setupCommonRequest(request, apiOrServiceName, isFlowValue);
        request.queryParam("InteropApiData", inputXml);

        if (!template.isEmpty()) {
            request.queryParam("TemplateData", template);
            logger.debug("Output Template provided for: {}", apiOrServiceName);
        }

        // Execute request
        Response response = executeRequest(request, httpMethod);
        Response xmlResponse = response.then().assertThat().statusCode(200).and().extract().response();

        // Attach response to Allure
        try {
//            AllureManager.attachXmlToReport(apiOrServiceName + " Response", xmlResponse.asString());
        } catch (Exception e) {
            logger.trace("Allure attachment skipped: {}", e.getMessage());
        }

        // Log response
        String rootName = getXmlRootName(xmlResponse.asString());
        logMessage.setLength(0);

        if (!rootName.equals(ERRORS)) {
            logMessage.append("Response: ").append(apiOrServiceName).append("\n").append(xmlResponse.asString());
            logger.info(logMessage.toString());
        } else {
            logMessage.append("ERROR Response: ").append(apiOrServiceName).append("\n").append(xmlResponse.asString());
            logger.error(logMessage.toString());
        }

        return xmlResponse;
    }

    /**
     * Execute HTTP request based on method.
     */
    private static Response executeRequest(RequestSpecification request, String httpMethod) {
        String endpoint = FileReaderManager.getInstance().getConfigReader().getEndPointUrl();

        return switch (httpMethod.toUpperCase()) {
            case "GET" -> request.get(endpoint);
            case "DELETE" -> request.delete(endpoint);
            case "PUT" -> request.put(endpoint);
            case "PATCH" -> request.patch(endpoint);
            default -> request.post(endpoint);  // Default to POST
        };
    }

    /**
     * Setup common request parameters for Sterling API.
     */
    public static RequestSpecification setupCommonRequest(RequestSpecification request, String apiOrServiceName, String isFlowValue) {
        String serviceName;
        String apiName;

        if (isFlowValue.equals("Y")) {
            serviceName = apiOrServiceName;
            apiName = "";
        } else {
            apiName = apiOrServiceName;
            serviceName = "";
        }

        var configReader = FileReaderManager.getInstance().getConfigReader();

        request.baseUri(configReader.getBaseUrl());
        request.queryParam("YFSEnvironment.progId", configReader.getProgId());
        request.queryParam("InteropApiName", apiOrServiceName);
        request.queryParam("IsFlow", isFlowValue);
        request.queryParam("ServiceName", serviceName);
        request.queryParam("ApiName", apiName);
        request.queryParam("YFSEnvironment.userId", configReader.getUserId());
        request.queryParam("YFSEnvironment.password", configReader.getPassword());
        request.queryParam("YFSEnvironment.version", "");
        request.queryParam("YFSEnvironment.locale", "");
        request.header("Accept", "text/xml");
        request.contentType("text/xml");

        logger.debug("Request configured for: {}", apiOrServiceName);
        return request;
    }

    // ==========================================================================
    // FILE OPERATIONS
    // ==========================================================================

    /**
     * Read file and return as string.
     *
     * @param path File path
     * @return File content as string
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
     *
     * @param responseFilePath Output file path
     * @param xmlStr           XML content
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

    // ==========================================================================
    // XML OPERATIONS
    // ==========================================================================

    /**
     * Read XML value using XPath.
     *
     * @param filepath XML file path
     * @param Xpath    XPath expression
     * @return Value at XPath or null
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
     *
     * @param xmlStr XML content
     * @return Root element name
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
     *
     * @param nodeName  Attribute name
     * @param nodeValue New attribute value
     * @param filepath  XML file path
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
     *
     * @param xmlStr XML content
     * @return Document object
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

    // ==========================================================================
    // UTILITY
    // ==========================================================================

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