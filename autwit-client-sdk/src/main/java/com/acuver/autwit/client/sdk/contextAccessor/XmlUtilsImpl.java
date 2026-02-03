package com.acuver.autwit.client.sdk.contextAccessor;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
import com.acuver.autwit.client.sdk.Autwit;
import org.w3c.dom.Document;

/**
 * XML Utilities Implementation.
 *
 * <h2>MAPS TO</h2>
 * com.acuver.automation.api.utils.xmlUtils.XmlUpdater
 *
 * <h2>PURPOSE</h2>
 * Provides XML manipulation utilities for editing, reading, and validating XML files.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class XmlUtilsImpl implements Autwit.ContextAccessor.XmlUtils {

//    private static final Logger log = LogManager.getLogger(XmlUtilsImpl.class);

    // Cached class reference
    private Class<?> xmlUpdaterClass;

    /**
     * Get XmlUpdater class via reflection.
     */
    private Class<?> getXmlUpdaterClass() {
        if (xmlUpdaterClass == null) {
            try {
                // Try new package first
                xmlUpdaterClass = Class.forName("com.acuver.autwit.internal.xml.XmlUpdater");
            } catch (ClassNotFoundException e1) {
                try {
                    // Fall back to old package
                    xmlUpdaterClass = Class.forName("com.acuver.automation.api.utils.xmlUtils.XmlUpdater");
                } catch (ClassNotFoundException e2) {
                    throw new RuntimeException("XmlUpdater not found in either package", e2);
                }
            }
        }
        return xmlUpdaterClass;
    }

    @Override
    public void editXmlFile(String tagName, int nodeCount, String attrName, String attrValue, String filePath) {
//        log.debug("Editing XML: tag={}, node={}, attr={}, value={}, file={}",
//                tagName, nodeCount, attrName, attrValue, filePath);
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod(
                    "editXmlFile", String.class, int.class, String.class, String.class, String.class);
            method.invoke(null, tagName, nodeCount, attrName, attrValue, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to edit XML file: " + filePath, e);
        }
    }

    @Override
    public void updateOrderNumber(String filePath) {
//        log.debug("Updating order number in: {}", filePath);
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod("updateOrderNumber", String.class);
            method.invoke(null, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update order number in: " + filePath, e);
        }
    }

    @Override
    public int orderLineCount(String filePath) {
//        log.debug("Getting order line count from: {}", filePath);
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod("orderLineCount", String.class);
            return (int) method.invoke(null, filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get order line count from: " + filePath, e);
        }
    }

    @Override
    public int getNumberOfNodes(String filePath, String xpathExpression) throws Exception {
//        log.debug("Getting node count for XPath: {} from {}", xpathExpression, filePath);
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod("getNumberOfNodes", String.class, String.class);
            return (int) method.invoke(null, filePath, xpathExpression);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get node count: " + xpathExpression, e);
        }
    }

    @Override
    public void validateAndSaveXmlResponse(String filePath, String response) {
//        log.debug("Validating and saving response to: {}", filePath);
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod("validateAndSaveXmlResponse", String.class, String.class);
            method.invoke(null, filePath, response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate and save XML: " + filePath, e);
        }
    }

    @Override
    public String getXMLString(Document document) throws Exception {
//        log.debug("Converting Document to XML string");
        try {
            Class<?> clazz = getXmlUpdaterClass();
            java.lang.reflect.Method method = clazz.getMethod("getXMLString", Document.class);
            return (String) method.invoke(null, document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert Document to XML string", e);
        }
    }
}