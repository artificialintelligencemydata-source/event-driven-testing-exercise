# CLIENT-TESTS MODULE IMPORT VIOLATIONS

**Date:** 2026-01-05  
**Purpose:** Identify all forbidden imports in client-tests module  
**Mode:** Analysis Only (No Code Changes)

---

## 1️⃣ VIOLATIONS FOUND

### File: `EventDrivenOrderLifecycleStepDefsFacedBased.java`

**Location:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefsFacedBased.java`

**Violations:**

| Line | Import | Type | Status |
|------|--------|------|--------|
| 5 | `import com.acuver.autwit.client.sdk.EventExpectation;` | SDK Interface | ❌ **VIOLATION** |
| 6 | `import com.acuver.autwit.internal.api.ApiCalls;` | Internal Testkit | ❌ **VIOLATION** |
| 7 | `import com.acuver.autwit.internal.asserts.SoftAssertUtils;` | Internal Testkit | ❌ **VIOLATION** |
| 8 | `import com.acuver.autwit.internal.context.ScenarioContext;` | Internal Testkit | ❌ **VIOLATION** |
| 9 | `import com.acuver.autwit.internal.context.ScenarioMDC;` | Internal Testkit | ❌ **VIOLATION** |

**Allowed Import:**
- ✅ Line 4: `import com.acuver.autwit.client.sdk.Autwit;` (CORRECT)

---

## 2️⃣ USAGE ANALYSIS

### EventExpectation Usage

**Lines:** 79, 93, 109, 123, 137, 151, 165, 179, 193, 207, 221, 235

**Pattern:**
```java
EventExpectation expectation = autwit.expectEvent(orderId, eventType);
expectation.assertSatisfied();
```

**Replacement:** Can use method chaining (no variable needed)

---

### ApiCalls Usage

**Line 29-31:**
```java
private ApiCalls api() {
    return ScenarioContext.api();
}
```

**Line 56:**
```java
Response response = api().createOrder(payload);
```

**Replacement:** Use `autwit.context().api().createOrder(payload)`

---

### SoftAssertUtils Usage

**Line 59:**
```java
SoftAssertUtils.getSoftAssert()
    .assertEquals(orderId, response.jsonPath().getString("orderId"), "OrderId mismatch");
```

**Line 96:**
```java
SoftAssertUtils.getSoftAssert().assertTrue(true, "Event payload validated");
```

**Replacement:** Use `autwit.context().assertions().getSoftAssert()`

---

### ScenarioContext Usage

**Line 30:**
```java
return ScenarioContext.api();
```

**Replacement:** Use `autwit.context().api()` directly

---

### ScenarioMDC Usage

**Line 43:**
```java
ScenarioMDC.setOrderId(orderId);
```

**Replacement:** Use `autwit.context().setOrderId(orderId)`

---

## 3️⃣ SUGGESTED REPLACEMENTS

### Complete Refactored File

**File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefsFacedBased.java`

**Changes:**

#### Imports Section (Lines 4-9)

**BEFORE:**
```java
import com.acuver.autwit.client.sdk.Autwit;
import com.acuver.autwit.client.sdk.EventExpectation;
import com.acuver.autwit.internal.api.ApiCalls;
import com.acuver.autwit.internal.asserts.SoftAssertUtils;
import com.acuver.autwit.internal.context.ScenarioContext;
import com.acuver.autwit.internal.context.ScenarioMDC;
```

**AFTER:**
```java
import com.acuver.autwit.client.sdk.Autwit;
```

---

#### Remove api() Helper Method (Lines 29-31)

**BEFORE:**
```java
private ApiCalls api() {
    return ScenarioContext.api();
}
```

**AFTER:**
```java
// REMOVED - Use autwit.context().api() directly
```

---

#### placeOrder() Method (Lines 36-69)

**BEFORE:**
```java
@Given("I place an order with product {string} and quantity {string}")
public void placeOrder(String product, String quantity) {

    autwit.context().setCurrentStep("I place an order with product " + product + " and quantity " + quantity);

    String orderId = String.valueOf(System.currentTimeMillis());
    autwit.context().set("orderId", orderId);
    ScenarioMDC.setOrderId(orderId);

    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", orderId);
    payload.put("productId", product);
    payload.put("quantity", Integer.parseInt(quantity));
    payload.put("customerId", "CUST001");
    payload.put("price", 1000);
    payload.put("totalAmount", 1000);

    Allure.addAttachment("Order Payload", payload.toString());

    try {
        Response response = api().createOrder(payload);
        Allure.addAttachment("Order Response", response.asString());

        SoftAssertUtils.getSoftAssert()
                .assertEquals(orderId, response.jsonPath().getString("orderId"), "OrderId mismatch");

        autwit.step().markStepSuccess();

    } catch (Exception e) {
        log.warn("Order placement failed for orderId={} — pausing scenario. Error: {}", orderId, e.getMessage());
        autwit.step().markStepFailed("Order placement failed: " + e.getMessage());
        throw new SkipException("Order placement failed → pausing scenario.");
    }
}
```

**AFTER:**
```java
@Given("I place an order with product {string} and quantity {string}")
public void placeOrder(String product, String quantity) {

    autwit.context().setCurrentStep("I place an order with product " + product + " and quantity " + quantity);

    String orderId = String.valueOf(System.currentTimeMillis());
    autwit.context().setOrderId(orderId);  // Sets both context and MDC

    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", orderId);
    payload.put("productId", product);
    payload.put("quantity", Integer.parseInt(quantity));
    payload.put("customerId", "CUST001");
    payload.put("price", 1000);
    payload.put("totalAmount", 1000);

    Allure.addAttachment("Order Payload", payload.toString());

    try {
        Response response = autwit.context().api().createOrder(payload);
        Allure.addAttachment("Order Response", response.asString());

        autwit.context().assertions().getSoftAssert()
                .assertEquals(orderId, response.jsonPath().getString("orderId"), "OrderId mismatch");

        autwit.step().markStepSuccess();

    } catch (Exception e) {
        log.warn("Order placement failed for orderId={} — pausing scenario. Error: {}", orderId, e.getMessage());
        autwit.step().markStepFailed("Order placement failed: " + e.getMessage());
        throw new SkipException("Order placement failed → pausing scenario.");
    }
}
```

**Changes:**
- Line 43: `ScenarioMDC.setOrderId(orderId)` → `autwit.context().setOrderId(orderId)`
- Line 56: `api().createOrder(payload)` → `autwit.context().api().createOrder(payload)`
- Line 59: `SoftAssertUtils.getSoftAssert()` → `autwit.context().assertions().getSoftAssert()`

---

#### verifyEventPublished() Method (Lines 72-83)

**BEFORE:**
```java
@Then("I verify {string} event is published to Kafka")
public void verifyEventPublished(String eventType) {

    autwit.context().setCurrentStep("I verify " + eventType + " event is published to Kafka");

    String orderId = autwit.context().get("orderId");

    EventExpectation expectation = autwit.expectEvent(orderId, eventType);
    expectation.assertSatisfied();

    autwit.step().markStepSuccess();
}
```

**AFTER:**
```java
@Then("I verify {string} event is published to Kafka")
public void verifyEventPublished(String eventType) {

    autwit.context().setCurrentStep("I verify " + eventType + " event is published to Kafka");

    String orderId = autwit.context().get("orderId");

    autwit.expectEvent(orderId, eventType).assertSatisfied();

    autwit.step().markStepSuccess();
}
```

**Changes:**
- Lines 79-80: Remove `EventExpectation` variable, use method chaining

---

#### validateEventPayload() Method (Lines 86-99)

**BEFORE:**
```java
@And("I validate the event payload contains correct order details")
public void validateEventPayload() {

    autwit.context().setCurrentStep("I validate the event payload contains correct order details");

    String orderId = autwit.context().get("orderId");

    EventExpectation expectation = autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST");
    expectation.assertSatisfied();

    SoftAssertUtils.getSoftAssert().assertTrue(true, "Event payload validated");

    autwit.step().markStepSuccess();
}
```

**AFTER:**
```java
@And("I validate the event payload contains correct order details")
public void validateEventPayload() {

    autwit.context().setCurrentStep("I validate the event payload contains correct order details");

    String orderId = autwit.context().get("orderId");

    autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();

    autwit.context().assertions().getSoftAssert().assertTrue(true, "Event payload validated");

    autwit.step().markStepSuccess();
}
```

**Changes:**
- Lines 93-94: Remove `EventExpectation` variable, use method chaining
- Line 96: `SoftAssertUtils.getSoftAssert()` → `autwit.context().assertions().getSoftAssert()`

---

#### All Other verifyEvent* Methods (Lines 102-238)

**Pattern:** All follow same pattern as `verifyEventPublished()`

**BEFORE:**
```java
EventExpectation expectation = autwit.expectEvent(orderId, eventType);
expectation.assertSatisfied();
```

**AFTER:**
```java
autwit.expectEvent(orderId, eventType).assertSatisfied();
```

**Affected Methods:**
- `verifyEventWithin()` - Line 109
- `validateOrderFields()` - Line 123
- `verifyPaymentEvent()` - Line 137
- `validatePaymentDetails()` - Line 151
- `verifyShipmentCreated()` - Line 165
- `validateShipmentDetails()` - Line 179
- `verifyShipmentPicked()` - Line 193
- `validatePickupInfo()` - Line 207
- `verifyOrderCharged()` - Line 221
- `validateFinalPayment()` - Line 235

---

#### verifyCorrelation() Method (Lines 242-258)

**BEFORE:**
```java
@And("I verify all events are correlated with the same order ID")
public void verifyCorrelation() {

    autwit.context().setCurrentStep("I verify all events are correlated with the same order ID");

    String orderId = autwit.context().get("orderId");

    // Verify key events exist and are correlated
    autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_AUTHORIZED").assertSatisfied();
    autwit.expectEvent(orderId, "SHIPMENT_CREATED").assertSatisfied();
    autwit.expectEvent(orderId, "SHIPMENT_PICKED").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_CHARGED").assertSatisfied();

    autwit.step().markStepSuccess();
}
```

**AFTER:**
```java
@And("I verify all events are correlated with the same order ID")
public void verifyCorrelation() {

    autwit.context().setCurrentStep("I verify all events are correlated with the same order ID");

    String orderId = autwit.context().get("orderId");

    // Verify key events exist and are correlated
    autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_AUTHORIZED").assertSatisfied();
    autwit.expectEvent(orderId, "SHIPMENT_CREATED").assertSatisfied();
    autwit.expectEvent(orderId, "SHIPMENT_PICKED").assertSatisfied();
    autwit.expectEvent(orderId, "ORDER_CHARGED").assertSatisfied();

    autwit.step().markStepSuccess();
}
```

**Changes:**
- ✅ No changes needed (already using method chaining)

---

## 4️⃣ SUMMARY OF CHANGES

### Import Changes

| Before | After | Count |
|--------|-------|-------|
| `import com.acuver.autwit.client.sdk.EventExpectation;` | **REMOVED** | 1 |
| `import com.acuver.autwit.internal.api.ApiCalls;` | **REMOVED** | 1 |
| `import com.acuver.autwit.internal.asserts.SoftAssertUtils;` | **REMOVED** | 1 |
| `import com.acuver.autwit.internal.context.ScenarioContext;` | **REMOVED** | 1 |
| `import com.acuver.autwit.internal.context.ScenarioMDC;` | **REMOVED** | 1 |

**Result:** Only `import com.acuver.autwit.client.sdk.Autwit;` remains

---

### Code Changes

| Pattern | Before | After | Occurrences |
|---------|--------|-------|-------------|
| EventExpectation variable | `EventExpectation expectation = autwit.expectEvent(...); expectation.assertSatisfied();` | `autwit.expectEvent(...).assertSatisfied();` | 11 |
| ApiCalls usage | `api().createOrder(payload)` | `autwit.context().api().createOrder(payload)` | 1 |
| SoftAssertUtils usage | `SoftAssertUtils.getSoftAssert()` | `autwit.context().assertions().getSoftAssert()` | 2 |
| ScenarioContext.api() | `ScenarioContext.api()` | `autwit.context().api()` | 1 |
| ScenarioMDC.setOrderId() | `ScenarioMDC.setOrderId(orderId)` | `autwit.context().setOrderId(orderId)` | 1 |

---

## 5️⃣ FEASIBILITY CONFIRMATION

### ✅ YES - Client Code CAN Depend ONLY on Autwit

**Confirmation:**

1. **EventExpectation:** ✅ Can be eliminated via method chaining
   - No need to store in variable
   - Direct method chaining works: `autwit.expectEvent(...).assertSatisfied()`

2. **ApiCalls:** ✅ Available via `autwit.context().api()`
   - SDK already provides `ContextAccessor.api()` method
   - Returns `ApiClient` interface with `createOrder()` method

3. **SoftAssertUtils:** ✅ Available via `autwit.context().assertions()`
   - SDK already provides `ContextAccessor.assertions()` method
   - Returns `SoftAssertions` interface with `getSoftAssert()` method

4. **ScenarioContext:** ✅ Replaced by `autwit.context()`
   - All `ScenarioContext` operations available via `ContextAccessor`
   - `get()`, `set()`, `api()` all available

5. **ScenarioMDC:** ✅ Replaced by `autwit.context().setOrderId()`
   - SDK provides `ContextAccessor.setOrderId()` method
   - Sets both context and MDC internally

---

## 6️⃣ COMPLETE REFACTORED FILE STRUCTURE

### Final Imports (Only 1)

```java
import com.acuver.autwit.client.sdk.Autwit;
```

### All Other Imports (Standard Java/Cucumber/Spring)

```java
import io.cucumber.java.en.*;
import io.cucumber.spring.ScenarioScope;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.SkipException;
import java.util.*;
```

---

## 7️⃣ VERIFICATION CHECKLIST

### Pre-Refactor
- [ ] Backup current file
- [ ] Run tests to establish baseline

### Refactor Steps
- [ ] Remove `EventExpectation` import
- [ ] Remove all `EventExpectation` variable declarations (11 occurrences)
- [ ] Replace with method chaining
- [ ] Remove `ApiCalls` import
- [ ] Remove `api()` helper method
- [ ] Replace `api().createOrder()` with `autwit.context().api().createOrder()`
- [ ] Remove `SoftAssertUtils` import
- [ ] Replace `SoftAssertUtils.getSoftAssert()` with `autwit.context().assertions().getSoftAssert()`
- [ ] Remove `ScenarioContext` import
- [ ] Remove `ScenarioMDC` import
- [ ] Replace `ScenarioMDC.setOrderId()` with `autwit.context().setOrderId()`

### Post-Refactor
- [ ] Verify only `Autwit` import remains
- [ ] Run tests to verify behavior unchanged
- [ ] Verify compilation succeeds
- [ ] Verify no runtime errors

---

## 8️⃣ RISK ASSESSMENT

### Overall Risk: ✅ **VERY LOW**

**Reasons:**
1. All replacements are direct API equivalents
2. No behavior changes
3. Method chaining is simpler (fewer lines)
4. All required SDK methods already exist
5. No new dependencies introduced

**Potential Issues:**
- None identified - all replacements are straightforward

---

## 9️⃣ FILES REQUIRING CHANGES

### Single File to Refactor

**File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefsFacedBased.java`

**Total Changes:**
- 5 imports removed
- 1 helper method removed
- 16 code replacements (11 EventExpectation, 1 ApiCalls, 2 SoftAssertUtils, 1 ScenarioContext, 1 ScenarioMDC)

**Estimated Effort:** 15-20 minutes

---

## 🔟 FINAL STATE

### After Refactor

**Imports:**
```java
import com.acuver.autwit.client.sdk.Autwit;  // ONLY AUTWIT IMPORT
```

**Usage Patterns:**
```java
// Event expectations
autwit.expectEvent(orderId, eventType).assertSatisfied();

// Context operations
autwit.context().set("key", value);
String value = autwit.context().get("key");
autwit.context().setOrderId(orderId);

// API calls
Response response = autwit.context().api().createOrder(payload);

// Soft assertions
autwit.context().assertions().getSoftAssert().assertEquals(...);

// Step tracking
autwit.step().markStepSuccess();
autwit.step().markStepFailed(reason);
```

**Result:** ✅ **100% Autwit-only dependency**

---

**END OF ANALYSIS**

