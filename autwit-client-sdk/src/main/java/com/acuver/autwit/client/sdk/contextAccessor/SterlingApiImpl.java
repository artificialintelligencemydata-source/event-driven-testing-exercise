package com.acuver.autwit.client.sdk.contextAccessor;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

import com.acuver.autwit.client.sdk.Autwit;

/**
 * Sterling API Implementation.
 *
 * <h2>MAPS TO</h2>
 * com.acuver.automation.api.helpers.SterlingApiCalls
 *
 * <h2>PURPOSE</h2>
 * Provides Sterling OMS specific API wrappers.
 * These methods internally use BaseActions.makeAPICall().
 *
 * @author AUTWIT Framework
 * @since 2.0.0
 */
class SterlingApiImpl implements Autwit.ContextAccessor.SterlingApi {

//    private static final Logger log = LogManager.getLogger(SterlingApiImpl.class);

    // Cached instance
    private Object sterlingApiCalls;

    /**
     * Get SterlingApiCalls instance.
     * Located at: com.acuver.autwit.internal.api.SterlingApiCalls
     */
    private Object getSterlingApiCalls() {
        if (sterlingApiCalls == null) {
            try {
                Class<?> clazz = Class.forName("com.acuver.autwit.internal.api.SterlingApiCalls");
                sterlingApiCalls = clazz.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "SterlingApiCalls not found at com.acuver.autwit.internal.api.SterlingApiCalls. " +
                                "Ensure the class is copied to autwit-internal module.", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate SterlingApiCalls", e);
            }
        }
        return sterlingApiCalls;
    }

    /**
     * Invoke method with no arguments.
     */
    private void invokeVoid(String methodName) throws Exception {
        Object api = getSterlingApiCalls();
        java.lang.reflect.Method method = api.getClass().getMethod(methodName);
        method.invoke(api);
    }

    /**
     * Invoke method with single String argument.
     */
    private void invokeVoidWithString(String methodName, String arg) throws Exception {
        Object api = getSterlingApiCalls();
        java.lang.reflect.Method method = api.getClass().getMethod(methodName, String.class);
        method.invoke(api, arg);
    }

    /**
     * Invoke method with two String arguments.
     */
    private void invokeVoidWithTwoStrings(String methodName, String arg1, String arg2) throws Exception {
        Object api = getSterlingApiCalls();
        java.lang.reflect.Method method = api.getClass().getMethod(methodName, String.class, String.class);
        method.invoke(api, arg1, arg2);
    }

    /**
     * Invoke method that returns int.
     */
    private int invokeInt(String methodName) throws Exception {
        Object api = getSterlingApiCalls();
        java.lang.reflect.Method method = api.getClass().getMethod(methodName);
        return (int) method.invoke(api);
    }

    // ==========================================================================
    // ORDER MANAGEMENT
    // ==========================================================================

    @Override
    public void createOrder(String filePath) throws Exception {
//        log.debug("Creating order from: {}", filePath);
        invokeVoidWithString("createOrder", filePath);
    }

    @Override
    public void scheduleOrder() throws Exception {
//        log.debug("Scheduling order");
        invokeVoid("scheduleOrder");
    }

    @Override
    public void releaseOrder() throws Exception {
//        log.debug("Releasing order");
        invokeVoid("releaseOrder");
    }

    @Override
    public void changeOrder(String filePath) throws Exception {
//        log.debug("Changing order from: {}", filePath);
        invokeVoidWithString("changeOrder", filePath);
    }

    @Override
    public int getOrderReleaseList() throws Exception {
//        log.debug("Getting order release list");
        return invokeInt("getOrderReleaseList");
    }

    @Override
    public void getOrderList() throws Exception {
//        log.debug("Getting order list");
        invokeVoid("getOrderList");
    }

    @Override
    public void getOrderDetails() throws Exception {
//        log.debug("Getting order details");
        invokeVoid("getOrderDetails");
    }

    @Override
    public void getOrderHoldTypeList() throws Exception {
//        log.debug("Getting order hold type list");
        invokeVoid("getOrderHoldTypeList");
    }

    @Override
    public void changeOrderStatus(String filePath) throws Exception {
//        log.debug("Changing order status from: {}", filePath);
        invokeVoidWithString("changeOrderStatus", filePath);
    }

    // ==========================================================================
    // SHIPMENT MANAGEMENT
    // ==========================================================================

    @Override
    public void createShipment(String filePath) throws Exception {
//        log.debug("Creating shipment from: {}", filePath);
        invokeVoidWithString("createShipment", filePath);
    }

    @Override
    public void confirmShipment(String filePath, String apiName) throws Exception {
//        log.debug("Confirming shipment: {} with API: {}", filePath, apiName);
        invokeVoidWithTwoStrings("confirmShipment", filePath, apiName);
    }

    @Override
    public void changeShipment(String filePath) throws Exception {
//        log.debug("Changing shipment from: {}", filePath);
        invokeVoidWithString("changeShipment", filePath);
    }

    @Override
    public void changeShipmentStatus(String filePath) throws Exception {
//        log.debug("Changing shipment status from: {}", filePath);
        invokeVoidWithString("changeShipmentStatus", filePath);
    }

    @Override
    public void getShipmentListForOrder() throws Exception {
//        log.debug("Getting shipment list for order");
        invokeVoid("getShipmentListForOrder");
    }

    @Override
    public void getShipmentDetails(String shipmentKey) throws Exception {
//        log.debug("Getting shipment details for: {}", shipmentKey);
        invokeVoidWithString("getShipmentDetails", shipmentKey);
    }

    @Override
    public void createShipmentInvoice() throws Exception {
//        log.debug("Creating shipment invoice");
        invokeVoid("createShipmentInvoice");
    }

    // ==========================================================================
    // INVENTORY MANAGEMENT
    // ==========================================================================

    @Override
    public void getATP() throws Exception {
//        log.debug("Getting ATP");
        invokeVoid("getATP");
    }

    @Override
    public void adjustInventory() throws Exception {
//        log.debug("Adjusting inventory");
        invokeVoid("adjustInventory");
    }

    @Override
    public void reserveItemInventory() throws Exception {
//        log.debug("Reserving item inventory");
        invokeVoid("reserveItemInventory");
    }

    @Override
    public void getReservation() throws Exception {
//        log.debug("Getting reservation");
        invokeVoid("getReservation");
    }

    @Override
    public void cancelReservation() throws Exception {
//        log.debug("Canceling reservation");
        invokeVoid("cancelReservation");
    }

    @Override
    public void getItemList(String itemId) throws Exception {
//        log.debug("Getting item list for: {}", itemId);
        invokeVoidWithString("getItemList", itemId);
    }

    // ==========================================================================
    // RECEIPT MANAGEMENT
    // ==========================================================================

    @Override
    public void receiveOrder(String filePath) throws Exception {
//        log.debug("Receiving order from: {}", filePath);
        invokeVoidWithString("receiveOrder", filePath);
    }

    @Override
    public void startReceipt() throws Exception {
//        log.debug("Starting receipt");
        invokeVoid("startReceipt");
    }

    @Override
    public void closeReceipt() throws Exception {
//        log.debug("Closing receipt");
        invokeVoid("closeReceipt");
    }

    // ==========================================================================
    // RELEASE MANAGEMENT
    // ==========================================================================

    @Override
    public void changeRelease() throws Exception {
//        log.debug("Changing release");
        invokeVoid("changeRelease");
    }

    // ==========================================================================
    // RETURN ORDER
    // ==========================================================================

    @Override
    public void authorizeReturnOrder() throws Exception {
//        log.debug("Authorizing return order");
        invokeVoid("authorizeReturnOrder");
    }

    @Override
    public void confirmDraftOrder() throws Exception {
//        log.debug("Confirming draft order");
        invokeVoid("confirmDraftOrder");
    }

    @Override
    public void createOrderInvoice(String filePath) throws Exception {
//        log.debug("Creating order invoice from: {}", filePath);
        invokeVoidWithString("createOrderInvoice", filePath);
    }

    @Override
    public void getOrderInvoiceDetailList() throws Exception {
//        log.debug("Getting order invoice detail list");
        invokeVoid("getOrderInvoiceDetailList");
    }
}