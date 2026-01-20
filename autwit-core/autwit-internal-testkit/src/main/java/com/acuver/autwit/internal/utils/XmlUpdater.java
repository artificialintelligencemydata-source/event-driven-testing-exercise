package com.acuver.autwit.internal.utils;


import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.helper.BaseActions;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

/**
 * XmlUpdater - XML manipulation utilities.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-internal
 * Package: com.acuver.autwit.internal.xml
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class XmlUpdater {

    private static final Logger logger = LogManager.getLogger(XmlUpdater.class);
    private static final String ERRORS = "Errors";

    // ==========================================================================
    // XML EDITING
    // ==========================================================================

    /**
     * Edit XML file attribute.
     *
     * @param tagName   Tag name
     * @param nodeCount Node index (1-based)
     * @param nodeName  Attribute name
     * @param nodeValue New attribute value
     * @param filepath  XML file path
     */
    public static void editXmlFile(String tagName, int nodeCount, String nodeName, String nodeValue, String filepath) {
        try {
            logger.debug("Editing XML: tag={}, node={}, attr={}, value={}, file={}",
                    tagName, nodeCount, nodeName, nodeValue, filepath);

            File xmlFile = new File(filepath);
            if (!xmlFile.exists() || xmlFile.length() == 0) {
                logger.error("XML file is empty or does not exist: {}", xmlFile.getAbsolutePath());
                return;
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);

            NodeList nodeList = doc.getElementsByTagName(tagName);
            if (nodeCount <= 0 || nodeCount > nodeList.getLength()) {
                logger.error("Node count out of range. Requested: {}, Total: {}", nodeCount, nodeList.getLength());
                return;
            }

            Node node = nodeList.item(nodeCount - 1);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                logger.error("Node is not an element node: {}", node.getNodeName());
                return;
            }

            NamedNodeMap attr = node.getAttributes();
            Node nodeDetails = attr.getNamedItem(nodeName);
            if (nodeDetails == null) {
                logger.error("Attribute '{}' not found in node '{}'", nodeName, node.getNodeName());
                return;
            }

            nodeDetails.setTextContent(nodeValue);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filepath));
            transformer.transform(source, result);

            logger.info("Successfully modified XML: {} = {}", nodeName, nodeValue);

        } catch (ParserConfigurationException | TransformerException | IOException | SAXException e) {
            logger.error("Error editing XML file: {}", e.getMessage());
        }
    }

    /**
     * Update order number in XML file.
     *
     * @param xmlFilePath XML file path
     */
    public static void updateOrderNumber(String xmlFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(xmlFilePath));

            Node orderNode = document.getDocumentElement();
            NamedNodeMap attributes = orderNode.getAttributes();
            Node orderNumberAttribute = attributes.getNamedItem("OrderNo");

            if (orderNumberAttribute != null) {
                String newOrderNumber = generateOrderNumber();
                orderNumberAttribute.setTextContent(newOrderNumber);

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "no");
                DOMSource source = new DOMSource(document);
                StreamResult result = new StreamResult(new File(xmlFilePath));
                transformer.transform(source, result);

                logger.info("Order number updated to: {}", newOrderNumber);
            } else {
                logger.warn("Order number element not found in XML");
            }
        } catch (Exception e) {
            logger.error("Error updating order number: {}", e.getMessage());
        }
    }

    // ==========================================================================
    // XML READING
    // ==========================================================================

    /**
     * Read XML value using XPath.
     *
     * @param filepath XML file path
     * @param xpath    XPath expression
     * @return Value at XPath or null
     */
    public static String XMLXpathReader(String filepath, String xpath) {
        return BaseActions.XMLXpathReader(filepath, xpath);
    }

    /**
     * Get order line count from XML.
     *
     * @param path XML file path
     * @return Number of OrderLine elements
     */
    public static int orderLineCount(String path) {
        int orderLineCount = 0;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(path);

            Node orderNode = document.getDocumentElement();
            NodeList orderLinesList = orderNode.getChildNodes();
            Node orderLinesNode = null;

            for (int i = 0; i < orderLinesList.getLength(); i++) {
                Node node = orderLinesList.item(i);
                if (node.getNodeName().equals("OrderLines")) {
                    orderLinesNode = node;
                    break;
                }
            }

            if (orderLinesNode != null) {
                NodeList orderLineList = orderLinesNode.getChildNodes();
                for (int i = 0; i < orderLineList.getLength(); i++) {
                    Node node = orderLineList.item(i);
                    if (node.getNodeName().equals("OrderLine")) {
                        orderLineCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error counting order lines: {}", e.getMessage());
        }
        return orderLineCount;
    }

    /**
     * Get number of nodes matching XPath.
     *
     * @param path            XML file path
     * @param xpathExpression XPath expression
     * @return Number of matching nodes
     */
    public static int getNumberOfNodes(String path, String xpathExpression) throws Exception {
        int nodeCount = 0;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(path);

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            XPathExpression expr = xpath.compile(xpathExpression);

            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            nodeCount = nodes.getLength();
        } catch (XPathExpressionException e) {
            logger.error("Error evaluating XPath: {}", e.getMessage());
        }
        return nodeCount;
    }

    // ==========================================================================
    // XML SAVING
    // ==========================================================================

    /**
     * Validate and save XML response.
     * Checks for error response and saves appropriately.
     *
     * @param filePath Output file path
     * @param response XML response string
     */
    public static void validateAndSaveXmlResponse(String filePath, String response) {
        String rootName = BaseActions.getXmlRootName(response);
        if (!rootName.equals(ERRORS)) {
            logger.debug("Saving valid response to: {}", filePath);
            BaseActions.SaveResponseAsXML(filePath, response);
        } else {
            String errorPath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath() + "ErrorResponse.xml";
            BaseActions.SaveResponseAsXML(errorPath, response);
            Allure.addAttachment("ErrorResponse", "application/xml", response, "xml");
            logger.error("Error response received, saved to: {}", errorPath);
        }
    }

    /**
     * Get XML string from Document.
     *
     * @param document Document object
     * @return XML string
     */
    public static String getXMLString(Document document) throws Exception {
        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }

    // ==========================================================================
    // UTILITY
    // ==========================================================================

    private static String generateOrderNumber() {
        return "ORD" + System.currentTimeMillis();
    }
}