package com.acuver.autwit.internal.api;

import com.acuver.autwit.internal.config.FileReaderManager;
import com.acuver.autwit.internal.helper.BaseActions;
import com.acuver.autwit.internal.utils.XmlUpdater;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * SterlingApiCalls - Sterling OMS specific API wrappers.
 *
 * <h2>LOCATION</h2>
 * Module: autwit-internal
 * Package: com.acuver.autwit.internal.api
 *
 * <h2>NOTE</h2>
 * All methods use {@link BaseActions#makeAPICall(String, String, String, String)} internally.
 * This class provides Sterling-specific convenience wrappers.
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
public class SterlingApiCalls {

    private static final Logger logger = LogManager.getLogger(SterlingApiCalls.class);

    // ==========================================================================
    // ORDER MANAGEMENT
    // ==========================================================================

    public void createOrder(String filePath) throws Exception {
        String apiInputXml = Files.readString(Paths.get(filePath));
        Response response = BaseActions.makeAPICall("createOrder", "POST", apiInputXml, "");

        String documentType = findXpathValue(response.asString(), "//@DocumentType");
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        if (documentType != null) {
            if (documentType.equals("0003")) {
                XmlUpdater.validateAndSaveXmlResponse(responsePath + "returnOrderResponse.xml", response.asString());
            } else {
                XmlUpdater.validateAndSaveXmlResponse(responsePath + "createOrderResponse.xml", response.asString());
            }
        } else {
            XmlUpdater.validateAndSaveXmlResponse(responsePath + "ErrorResponse.xml", response.asString());
        }
    }

    public void scheduleOrder() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "scheduleOrder.xml"));
        Response response = BaseActions.makeAPICall("scheduleOrder", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "scheduleOrderResponse.xml", response.asString());
    }

    public void releaseOrder() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "releaseOrder.xml"));
        Response response = BaseActions.makeAPICall("releaseOrder", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "releaseOrderResponse.xml", response.asString());
    }

    public void changeOrder(String filePath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(filePath));
        Response response = BaseActions.makeAPICall("changeOrder", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "changeOrderResponse.xml", response.asString());
    }

    public int getOrderReleaseList() throws Exception {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String inputXml = Files.readString(Paths.get(inputPath + "getOrderReleaseList.xml"));
        String response = BaseActions.makeAPICall("getOrderReleaseList", "POST", inputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getOrderReleaseListResponse.xml", response);

        return XmlUpdater.getNumberOfNodes(responsePath + "getOrderReleaseListResponse.xml", "/OrderReleaseList/OrderRelease");
    }

    public void getOrderList() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "getOrderList.xml"));
        Response response = BaseActions.makeAPICall("getOrderList", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getOrderListResponse.xml", response.asString());
    }

    public void getOrderDetails() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "getOrderDetails.xml"));
        Response response = BaseActions.makeAPICall("getOrderDetails", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getOrderDetailsResponse.xml", response.asString());
    }

    public void getOrderHoldTypeList() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String orderHeaderKey = BaseActions.XMLXpathReader(responsePath + "createOrderResponse.xml", "//@OrderHeaderKey");
        XmlUpdater.editXmlFile("OrderHoldType", 1, "OrderHeaderKey", orderHeaderKey, inputPath + "getOrderHoldTypeList.xml");

        String apiInputXml = Files.readString(Paths.get(inputPath + "getOrderHoldTypeList.xml"));
        Response response = BaseActions.makeAPICall("getOrderHoldTypeList", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getOrderHoldTypeListResponse.xml", response.asString());
    }

    public void changeOrderStatus(String filePath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String changeOrderStatusInputXml = Files.readString(Paths.get(filePath));
        String response = BaseActions.makeAPICall("changeOrderStatus", "POST", changeOrderStatusInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "changeOrderStatusResponse.xml", response);
    }

    // ==========================================================================
    // SHIPMENT MANAGEMENT
    // ==========================================================================

    public void createShipment(String createShipmentInputXmlPath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String createShipmentInputXml = Files.readString(Paths.get(createShipmentInputXmlPath));
        String response = BaseActions.makeAPICall("createShipment", "POST", createShipmentInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "createShipmentResponse.xml", response);
    }

    public void confirmShipment(String confirmShipmentInputXmlPath, String apiName) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String confirmShipmentInputXml = Files.readString(Paths.get(confirmShipmentInputXmlPath));
        String response = BaseActions.makeAPICall(apiName, "POST", confirmShipmentInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "confirmShipmentResponse.xml", response);
    }

    public void changeShipment(String changeShipmentXmlPath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String changeShipmentInputXml = Files.readString(Paths.get(changeShipmentXmlPath));
        String response = BaseActions.makeAPICall("changeShipment", "POST", changeShipmentInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "changeShipmentResponse.xml", response);
    }

    public void changeShipmentStatus(String filePath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String changeShipmentStatusInputXml = Files.readString(Paths.get(filePath));
        String response = BaseActions.makeAPICall("changeShipmentStatus", "POST", changeShipmentStatusInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "changeShipmentStatusResponse.xml", response);
    }

    public void getShipmentListForOrder() throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(responsePath + "createOrderResponse.xml"));
        Response apiResponse = BaseActions.makeAPICall("getShipmentListForOrder", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getShipmentListForOrderResponse.xml", apiResponse.asString());
    }

    public void getShipmentDetails(String shipmentKey) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        XmlUpdater.editXmlFile("Shipment", 1, "ShipmentKey", shipmentKey, responsePath + "changeShipmentStatusResponse.xml");
        String apiInputXml = Files.readString(Paths.get(responsePath + "changeShipmentStatusResponse.xml"));
        Response apiResponse = BaseActions.makeAPICall("getShipmentDetails", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getShipmentDetailsResponse.xml", apiResponse.asString());
    }

    public void createShipmentInvoice() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String shipmentKey = XmlUpdater.XMLXpathReader(responsePath + "confirmShipmentResponse.xml", "//@ShipmentKey");
        XmlUpdater.editXmlFile("Shipment", 1, "ShipmentKey", shipmentKey, inputPath + "createShipmentInvoiceInput.xml");

        String createShipmentInvoiceInputXml = Files.readString(Paths.get(inputPath + "createShipmentInvoiceInput.xml"));
        String response = BaseActions.makeAPICall("createShipmentInvoice", "POST", createShipmentInvoiceInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "createShipmentInvoiceResponse.xml", response);
    }

    // ==========================================================================
    // INVENTORY MANAGEMENT
    // ==========================================================================

    public void getATP() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "getATP.xml"));
        Response response = BaseActions.makeAPICall("getATP", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getATPResponse.xml", response.asString());
    }

    public void adjustInventory() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "AdjustInventory.xml"));
        Response response = BaseActions.makeAPICall("adjustInventory", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "AdjustInventoryOutput.xml", response.asString());
    }

    public void reserveItemInventory() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "reserveItemInventory.xml"));
        Response response = BaseActions.makeAPICall("reserveItemInventory", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "reserveItemInventoryResponse.xml", response.asString());
    }

    public void getReservation() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "getReservation.xml"));
        Response response = BaseActions.makeAPICall("getReservation", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getReservationResponse.xml", response.asString());
    }

    public void cancelReservation() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "cancelReservation.xml"));
        Response response = BaseActions.makeAPICall("cancelReservation", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "cancelReservationResponse.xml", response.asString());
    }

    public void getItemList(String itemId) throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        XmlUpdater.editXmlFile("Item", 1, "ItemID", itemId, inputPath + "getItemList.xml");
        String apiInputXml = Files.readString(Paths.get(inputPath + "getItemList.xml"));
        Response response = BaseActions.makeAPICall("getItemList", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getItemListResponse.xml", response.asString());
    }

    // ==========================================================================
    // RECEIPT MANAGEMENT
    // ==========================================================================

    public void receiveOrder(String receiveOrderInputXmlPath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String receiveOrderInputXml = Files.readString(Paths.get(receiveOrderInputXmlPath));
        String response = BaseActions.makeAPICall("receiveOrder", "POST", receiveOrderInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "receiveOrderResponse.xml", response);
    }

    public void startReceipt() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "startReceipt.xml"));
        Response response = BaseActions.makeAPICall("startReceipt", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "startReceiptResponse.xml", response.asString());
    }

    public void closeReceipt() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "closeReceipt.xml"));
        Response response = BaseActions.makeAPICall("closeReceipt", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "closeReceiptResponse.xml", response.asString());
    }

    // ==========================================================================
    // RELEASE MANAGEMENT
    // ==========================================================================

    public void changeRelease() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String changeReleaseInputXml = Files.readString(Paths.get(inputPath + "changeRelease.xml"));
        String response = BaseActions.makeAPICall("changeRelease", "POST", changeReleaseInputXml, "").asString();
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "changeReleaseResponse.xml", response);
    }

    // ==========================================================================
    // RETURN ORDER
    // ==========================================================================

    public void authorizeReturnOrder() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "authorizeReturnOrder.xml"));
        Response response = BaseActions.makeAPICall("multiApi", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "authorizeReturnResponse.xml", response.asString());
    }

    public void confirmDraftOrder() throws IOException {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "createReturnOrder.xml"));
        Response response = BaseActions.makeAPICall("confirmDraftOrder", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "confirmDraftOrderResponse.xml", response.asString());
    }

    public void createOrderInvoice(String filePath) throws IOException {
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(filePath));
        Response response = BaseActions.makeAPICall("createOrderInvoice", "POST", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "returnOrderInvoiceResponse.xml", response.asString());
    }

    public void getOrderInvoiceDetailList() throws Exception {
        String inputPath = FileReaderManager.getInstance().getConfigReader().getInputXmlPath();
        String responsePath = FileReaderManager.getInstance().getConfigReader().getResponseXmlPath();

        String apiInputXml = Files.readString(Paths.get(inputPath + "getOrderInvoiceDetailsList.xml"));
        Response response = BaseActions.makeAPICall("getOrderInvoiceDetailList", "GET", apiInputXml, "");
        XmlUpdater.validateAndSaveXmlResponse(responsePath + "getOrderInvoiceDetailsListResponse.xml", response.asString());
    }

    // ==========================================================================
    // UTILITY
    // ==========================================================================

    private String findXpathValue(String xmlStr, String xpath) {
        try {
            return BaseActions.XMLXpathReader(xmlStr, xpath);
        } catch (Exception e) {
            return null;
        }
    }
}