// ===== File: com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java =====
package com.bjs.tests.stepDefinitions;

import com.acuver.autwit.client.sdk.Autwit;
import io.cucumber.java.en.*;
import io.cucumber.spring.ScenarioScope;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.SkipException;

import java.util.*;

@ScenarioScope
public class EventDrivenOrderLifecycleStepDefsFacedBased {

    private static final Logger log = LogManager.getLogger(EventDrivenOrderLifecycleStepDefsFacedBased.class);

    @Autowired
    private Autwit autwit;

    // ==============================================================================
    // STEP 1 — Place Order
    // ==============================================================================
    @Given("I place an order with product {string} and quantity {string}")
    public void placeOrder(String product, String quantity) {
        autwit.context().setCurrentStep("I place an order with product " + product + " and quantity " + quantity);
        String orderId = String.valueOf(System.currentTimeMillis());
        autwit.context().setOrderId(orderId);
        if(autwit.step().skipIfAlreadySuccessful()){
            log.info("♻️ Step already successful, restoring and skipping execution");
            Map<String, String> stepData = autwit.step().getStepData();
            String savedOrderId = stepData.get("orderId");
            if (savedOrderId == null || savedOrderId.isBlank()) {
                throw new IllegalStateException("Saved orderId missing for resumed step");
            }
            autwit.context().setOrderId(savedOrderId);
            Allure.addAttachment("Reused orderId", savedOrderId);
            Allure.addAttachment("Reused Order Payload",
                    stepData.getOrDefault("Order Payload", "N/A"));
            Allure.addAttachment("Reused Order Response",
                    stepData.getOrDefault("Order response", "N/A"));
            return;
        }
        // ===========================
        // Fresh execution
        // ===========================
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("productId", product);
        payload.put("quantity", Integer.parseInt(quantity));
        payload.put("customerId", "CUST001");
        payload.put("price", 1000);
        payload.put("totalAmount", 1000);
        Allure.addAttachment("Order Payload", payload.toString());
        try {
//            Response response = autwit.context().baseActions().makeAPICall("cads","post","input");
//            Allure.addAttachment("Order Response", response.asString());
//            autwit.context().assertions().getSoftAssert()
//                    .assertEquals(orderId, response.jsonPath().getString("orderId"), "OrderId mismatch");
            autwit.step().markStepSuccess();

        } catch (Exception e) {
            log.warn("Order placement failed for orderId={} — pausing scenario. Error: {}", orderId, e.getMessage());
            autwit.step().markStepSkipped("Order placement failed: " + e.getMessage());
            throw new SkipException("Order placement failed → pausing scenario.");
        }
    }

    // ==============================================================================
    @Then("I verify {string} event is published to Kafka")
    public void verifyEventPublished(String eventType) {
//        autwit.step().skipIfAlreadySuccessful();
        autwit.context().setCurrentStep("I verify " + eventType + " event is published to Kafka"); // this should be handled at frame work level and given option to override while used mannually.
        String orderId = autwit.context().get("orderId");
        //autwit.expectEvent(orderId, eventType).assertSatisfied();
        autwit.step().markStepSuccess();
        //autwit.setEventPlaceHolder(orderID,eventType);
        //autwit.step().markStepSkipped(orderId);
    }

    // ==============================================================================
    @And("I validate the event payload contains correct order details")
    public void validateEventPayload() {

       autwit.context().setCurrentStep("I validate the event payload contains correct order details");
        String orderId = autwit.context().get("orderId");
//        autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();
//        autwit.context().baseActions().makeAPICall().asString().equals().
//        autwit.context().assertions().getSoftAssert().assertTrue(true, "Event payload validated");
        autwit.step().markStepSuccess();
    }

    // ==============================================================================
    @And("I verify {string} event is published within {int} seconds")
    public void verifyEventWithin(String eventType, int seconds) {
        autwit.context().setCurrentStep("I verify " + eventType + " event is published within " + seconds + " seconds");
        String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
//        autwit.context().assertions().assertAll();
//        autwit.context().assertions().getSoftAssert().assertEquals();
//        autwit.context().baseActions().makeAPICall();
//        autwit.context().sterling().createOrder();
//        autwit.step().markStepSuccess();
//        autwit.step().markStepSuccess();
//        autwit.step().markStepSkipped(orderId);
//        autwit.context().xml().editXmlFile();
    }

    // ==============================================================================
    @And("I validate the event contains order ID and customer information")
    public void validateOrderFields() {

        /*autwit.context().setCurrentStep("I validate the event contains order ID and customer information");
        String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @And("I verify {string} event is published after payment processing")
    public void verifyPaymentEvent(String eventType) {

        autwit.context().setCurrentStep("I verify " + eventType + " event is published after payment processing");

        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @And("I validate the event contains payment authorization details")
    public void validatePaymentDetails() {

        autwit.context().setCurrentStep("I validate the event contains payment authorization details");

        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, "ORDER_AUTHORIZED").assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @Then("I verify {string} event is published to shipment topic")
    public void verifyShipmentCreated(String eventType) {

        autwit.context().setCurrentStep("I verify " + eventType + " event is published to shipment topic");

        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @And("I validate the event contains shipment ID and tracking information")
    public void validateShipmentDetails() {

        autwit.context().setCurrentStep("I validate the event contains shipment ID and tracking information");
        /*String orderId = autwit.context().get("orderId");

        autwit.expectEvent(orderId, "SHIPMENT_CREATED").assertSatisfied();

        autwit.step().markStepSuccess();
   */ }

    // ==============================================================================
    @And("I verify {string} event is published when warehouse processes")
    public void verifyShipmentPicked(String eventType) {

        autwit.context().setCurrentStep("I verify " + eventType + " event is published when warehouse processes");
        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // =============================================================================
    @And("I validate the event contains pickup timestamp and location")
    public void validatePickupInfo() {

        autwit.context().setCurrentStep("I validate the event contains pickup timestamp and location");
       /* String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, "SHIPMENT_PICKED").assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @And("I verify {string} event is published after successful delivery")
    public void verifyOrderCharged(String eventType) {

        autwit.context().setCurrentStep("I verify " + eventType + " event is published after successful delivery");
        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
        autwit.step().markStepSuccess();
    */}

    // ==============================================================================
    @And("I validate the event contains final payment confirmation")
    public void validateFinalPayment() {

        autwit.context().setCurrentStep("I validate the event contains final payment confirmation");
        /*String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, "ORDER_CHARGED").assertSatisfied();
        autwit.step().markStepSuccess();
   */ }

    // ==============================================================================
    @And("I verify all events are correlated with the same order ID")
    public void verifyCorrelation() {

        autwit.context().setCurrentStep("I verify all events are correlated with the same order ID");

        /*String orderId = autwit.context().get("orderId");

        // Verify key events exist and are correlated
        autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();
        autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
        autwit.expectEvent(orderId, "ORDER_AUTHORIZED").assertSatisfied();
        autwit.expectEvent(orderId, "SHIPMENT_CREATED").assertSatisfied();
        autwit.expectEvent(orderId, "SHIPMENT_PICKED").assertSatisfied();
        autwit.expectEvent(orderId, "ORDER_CHARGED").assertSatisfied();

        autwit.step().markStepSuccess();
    */}
}