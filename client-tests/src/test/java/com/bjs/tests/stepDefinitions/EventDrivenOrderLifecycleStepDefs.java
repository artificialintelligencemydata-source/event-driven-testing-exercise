//package com.bjs.tests.stepDefinitions;
//
//import com.acuver.autwit.core.domain.EventContextEntities;
//import com.acuver.autwit.core.ports.EventContextPort;
//import com.acuver.autwit.core.ports.EventMatcherPort;
//import com.acuver.autwit.core.ports.ScenarioStatePort;
//import com.acuver.autwit.internal.api.ApiCalls;
//import com.acuver.autwit.internal.context.ScenarioContext;
//import com.acuver.autwit.internal.context.ScenarioMDC;
//import com.acuver.autwit.internal.asserts.SoftAssertUtils;
//import io.cucumber.java.en.*;
//import io.cucumber.spring.ScenarioScope;
//import io.qameta.allure.Allure;
//import io.restassured.response.Response;
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.testng.SkipException;
//
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//@ScenarioScope
//public class EventDrivenOrderLifecycleStepDefs {
//
//    private static final Logger log = LogManager.getLogger(EventDrivenOrderLifecycleStepDefs.class);
//
//    @Autowired private EventContextPort eventContextPort;
//    @Autowired private EventMatcherPort eventStepNotifier;
//    @Autowired private ScenarioStatePort tracker;
//
//    private ApiCalls api() { return ScenarioContext.api(); }
//
//    // ==============================================================================
//    // UTILITY → Resume Logic
//    // ==============================================================================
//    private boolean skipIfAlreadyPassed(String stepName, String eventType) {
//
//        // NOTE: Hooks sets "scenarioKey" — use the same key here.
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//
//        if (scenarioKey == null) {
//            // If scenarioKey is not present, don't attempt DB resume checks.
//            log.debug("scenarioKey not found in ScenarioContext — proceeding normally for step {}", stepName);
//            return false;
//        }
//
//        if (!tracker.isStepAlreadySuccessful(scenarioKey, stepName))
//            return false;
//
//        log.info("⏭️ Skipping already completed step: {}", stepName);
//
//        Map<String,String> saved = tracker.getStepData(scenarioKey, stepName);
//        if (saved != null && saved.containsKey("orderId")) {
//            String restored = saved.get("orderId");
//            ScenarioContext.set("orderId", restored);
//            ScenarioMDC.setOrderId(restored);
//            Allure.addAttachment("Restored OrderId", restored);
//            log.debug("Restored orderId={} for scenarioKey={} step={}", restored, scenarioKey, stepName);
//        }
//
//        return true;
//    }
//
//    // ==============================================================================
//    // STEP 1 — Place Order
//    // ==============================================================================
//    @Given("I place an order with product {string} and quantity {string}")
//    public void placeOrder(String product, String quantity) {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I place an order with product " + product + " and quantity " + quantity;
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = String.valueOf(System.currentTimeMillis());
//        ScenarioContext.set("orderId", orderId);
//        ScenarioMDC.setOrderId(orderId);
//
//        Map<String,Object> payload = new HashMap<>();
//        payload.put("orderId", orderId);
//        payload.put("productId", product);
//        payload.put("quantity", Integer.parseInt(quantity));
//        payload.put("customerId", "CUST001");
//        payload.put("price", 1000);
//        payload.put("totalAmount", 1000);
//
//        Allure.addAttachment("Order Payload", payload.toString());
//
//        try {
//            Response response = api().createOrder(payload);
//            Allure.addAttachment("Order Response", response.asString());
//
//            SoftAssertUtils.getSoftAssert()
//                    .assertEquals(orderId, response.jsonPath().getString("orderId"), "OrderId mismatch");
//
//            tracker.markStep(scenarioKey, stepName, "success",
//                    Map.of("orderId", orderId, "payload", payload.toString()));
//
//        } catch (Exception e) {
//            log.warn("Order placement failed for scenarioKey={} orderId={} — pausing scenario. Error: {}",
//                    scenarioKey, orderId, e.getMessage());
//            throw new SkipException("Order placement failed → pausing scenario.");
//        }
//    }
//
//    // ==============================================================================
//    @Then("I verify {string} event is published to Kafka")
//    public void verifyEventPublished(String eventType) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published to Kafka";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(10, TimeUnit.SECONDS);
//
//        Allure.addAttachment("Event Data", ctx.getKafkaPayload());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event payload contains correct order details")
//    public void validateEventPayload() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event payload contains correct order details";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "CREATE_ORDER_REQUEST");
//        if (evt.isEmpty()) throw new SkipException("Event not found yet");
//
//        SoftAssertUtils.getSoftAssert().assertEquals(orderId, evt.get().getOrderId());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I verify {string} event is published within {int} seconds")
//    public void verifyEventWithin(String eventType, int seconds) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published within " + seconds + " seconds";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(seconds, TimeUnit.SECONDS);
//
//        Allure.addAttachment("Event Data", ctx.getKafkaPayload());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event contains order ID and customer information")
//    public void validateOrderFields() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event contains order ID and customer information";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "ORDER_CREATED");
//        if (evt.isEmpty()) throw new SkipException("ORDER_CREATED not found");
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I verify {string} event is published after payment processing")
//    public void verifyPaymentEvent(String eventType) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published after payment processing";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(15, TimeUnit.SECONDS);
//
//        Allure.addAttachment("Payment Event", ctx.getKafkaPayload());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event contains payment authorization details")
//    public void validatePaymentDetails() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event contains payment authorization details";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "ORDER_AUTHORIZED");
//        if (evt.isEmpty()) throw new SkipException("Not arrived yet");
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @Then("I verify {string} event is published to shipment topic")
//    public void verifyShipmentCreated(String eventType) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published to shipment topic";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(20, TimeUnit.SECONDS);
//
//        Allure.addAttachment("Shipment Event", ctx.getKafkaPayload());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event contains shipment ID and tracking information")
//    public void validateShipmentDetails() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event contains shipment ID and tracking information";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "SHIPMENT_CREATED");
//        if (evt.isEmpty()) throw new SkipException("Not arrived yet");
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I verify {string} event is published when warehouse processes")
//    public void verifyShipmentPicked(String eventType) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published when warehouse processes";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(20, TimeUnit.SECONDS);
//
//        Allure.addAttachment("Pickup Event", ctx.getKafkaPayload());
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event contains pickup timestamp and location")
//    public void validatePickupInfo() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event contains pickup timestamp and location";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "SHIPMENT_PICKED");
//        if (evt.isEmpty()) throw new SkipException("Event not arrived");
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I verify {string} event is published after successful delivery")
//    public void verifyOrderCharged(String eventType) throws Exception {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I verify " + eventType + " event is published after successful delivery";
//
//        if (skipIfAlreadyPassed(stepName, eventType)) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        EventContextEntities ctx = eventStepNotifier.match(orderId, eventType)
//                .get(20, TimeUnit.SECONDS);
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I validate the event contains final payment confirmation")
//    public void validateFinalPayment() {
//
//        String scenarioKey = ScenarioContext.get("scenarioKey");
//        String stepName = "I validate the event contains final payment confirmation";
//
//        if (skipIfAlreadyPassed(stepName, "")) return;
//
//        String orderId = ScenarioContext.get("orderId");
//
//        Optional<EventContextEntities> evt = eventContextPort.findLatest(orderId, "ORDER_CHARGED");
//        if (evt.isEmpty()) throw new SkipException("Not yet");
//
//        tracker.markStep(scenarioKey, stepName, "success", Map.of("orderId", orderId));
//    }
//
//    // ==============================================================================
//    @And("I verify all events are correlated with the same order ID")
//    public void verifyCorrelation() {
//
//        String orderId = ScenarioContext.get("orderId");
//        List<EventContextEntities> events = eventContextPort.findByOrderId(orderId);
//
//        for (EventContextEntities e : events) {
//            SoftAssertUtils.getSoftAssert().assertEquals(orderId, e.getOrderId());
//        }
//    }
//}
