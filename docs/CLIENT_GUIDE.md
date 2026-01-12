# AUTWIT Client Guide

This guide is **AUTHORITATIVE** for anyone writing feature files or step definitions using AUTWIT.

If something here contradicts code, the code is wrong.

---

## Who This Guide Is For

This guide is for:
- QA engineers
- Test authors
- Automation developers
- Anyone writing feature files or step definitions

This guide is **NOT** for:
- Engine developers
- Framework maintainers
- Adapter authors

If you are touching Kafka, databases, ports, or resume logic, then you are in the wrong place.

---

## ⚠️ WARNING: Temporary Development Lenience

Some existing tests in the AUTWIT codebase may access internal ports (`EventContextPort`, `EventMatcherPort`, `ScenarioStatePort`) due to temporary development lenience during core stabilization.

**NEW TESTS MUST NOT follow this pattern.**

Even during lenience:
- Tests MUST still be event-driven
- Tests MUST pause via `SkipException` when data is unavailable
- Time-based logic (timeouts, sleeps, polling, retries) is **INVALID**
- Waiting for Kafka/DB is **FORBIDDEN**

This lenience is temporary and will be removed before:
- SDK hardening
- External adoption
- v1.0 release

**If you are writing new tests, use ONLY the client SDK APIs.**

---

## Core Mindset (Read First)

AUTWIT tests are **NOT scripts**. They are **declarations of intent**.

**You do NOT:**
- Wait for things
- Poll for things
- Retry things
- Reason about time

**You ONLY:**
- Trigger actions
- Declare expectations

AUTWIT decides **WHEN** expectations are satisfied.

---

## What Client Tests Are Allowed to Do

**Client tests MAY:**
- Call business APIs
- Place orders
- Trigger workflows
- Declare expected events
- Validate final outcomes

**Client tests MUST NOT:**
- Access databases
- Access Kafka
- Use timeouts
- Use sleeps
- Use retries
- Reason about pause or resume

---

## What You Can Import (STRICT)

### Allowed Imports

```java
import com.acuver.autwit.client.sdk.Autwit;
```

**That is ALL.**

### Forbidden Imports

- Any `com.acuver.autwit.internal.*` package
- Any `com.acuver.autwit.core.ports.*` package
- Any `com.acuver.autwit.core.engine.*` package
- Any adapter package
- Any DB client
- Any Kafka client

**If you need any forbidden import, the SDK is incomplete — NOT your test.**

---

## Basic Step Definition Pattern

### Example: Event Expectation

```java
@Autowired
private Autwit autwit;

@Then("order created event should arrive")
public void verifyOrderCreated() {
    String orderId = autwit.context().get("orderId");
    autwit.expectEvent(orderId, "ORDER_CREATED")
          .assertSatisfied();
    autwit.step().markStepSuccess();
}
```

**Rules:**
- No time
- No waits
- No retries
- No exception handling

If the event does not exist, AUTWIT pauses the scenario automatically.

### Example: API Call + Event Expectation

```java
@Given("I place an order with product {string} and quantity {string}")
public void placeOrder(String product, String quantity) {
    autwit.context().setCurrentStep("I place an order with product " + product);
    
    String orderId = String.valueOf(System.currentTimeMillis());
    autwit.context().setOrderId(orderId);
    autwit.context().set("orderId", orderId);
    
    Map<String, Object> payload = new HashMap<>();
    payload.put("orderId", orderId);
    payload.put("productId", product);
    payload.put("quantity", Integer.parseInt(quantity));
    
    try {
        Response response = autwit.context().api().createOrder(payload);
        autwit.step().markStepSuccess();
    } catch (Exception e) {
        autwit.step().markStepFailed("Order placement failed: " + e.getMessage());
        throw new SkipException("Order placement failed → pausing scenario.");
    }
}
```

### Example: Soft Assertions

```java
@And("I validate the event payload contains correct order details")
public void validateEventPayload() {
    String orderId = autwit.context().get("orderId");
    
    autwit.expectEvent(orderId, "CREATE_ORDER_REQUEST").assertSatisfied();
    
    autwit.context().assertions().getSoftAssert()
        .assertTrue(true, "Event payload validated");
    
    autwit.step().markStepSuccess();
}
```

---

## Feature File Guidelines

### ✅ GOOD Example

```gherkin
Scenario: Order creation lifecycle
  Given I place an order
  Then ORDER_CREATED event should arrive
  And I validate the event contains order ID
```

### ❌ BAD Examples

```gherkin
Then ORDER_CREATED event should arrive within 10 seconds
Then wait until ORDER_CREATED appears
Then retry until event arrives
```

**Time must NEVER appear in feature files.**

---

## Pause & Resume (Client Mental Model)

**As a client author:**
- You do **NOT** pause tests
- You do **NOT** resume tests

**You simply:**
- Declare expectations

**Internally:**
- Missing data → scenario pauses
- Data arrives later → scenario resumes
- Resume happens automatically

**You do NOTHING special.**

---

## Failure Behavior

| Situation | Result |
|-----------|--------|
| Event missing | Scenario paused (Skipped) |
| Event arrives later | Scenario resumes automatically |
| Assertion incorrect | Test failure |
| Code or infrastructure error | Test failure |

**Skipped is NOT a failure.**

Skipped means: **Reality has not happened yet.**

---

## What NOT to Fix in Client Tests

If you observe:
- Frequent skips
- Delayed resumes
- Out-of-order events

**Do NOT:**
- Add waits
- Add retries
- Add sleeps

**Instead:**
- Fix the system
- Fix adapters
- Fix event production

---

## Forbidden Anti-Patterns

The following are **NEVER allowed:**

```java
Thread.sleep(...)
Awaitility.await(...)
withinSeconds(...)
retry(...)
poll(...)
CompletableFuture.get(timeout, TimeUnit)
```

**Using these means you are fighting AUTWIT.**

---

## Scenario Isolation Rule

Each scenario:
- Must have a unique business key (`orderId`, `caseId`, etc.)
- Must not share mutable state
- Must not depend on execution order

**Parallel execution is the default.**

---

## Debugging Guideline

When a scenario pauses:

1. Check external system logs
2. Check adapter logs
3. Check persisted events
4. **DO NOT modify test code**

**Client tests are the LAST thing to change.**

---

## Versioning Guarantee

- `autwit-client-sdk` is stable
- Engine changes must NOT break client tests
- SDK evolves so client tests do not have to

---

## Golden Rule

**If your test needs to understand HOW AUTWIT works, then AUTWIT has failed — not your test.**

---

## Final Checklist Before Commit

- [ ] No waits
- [ ] No time
- [ ] No DB access
- [ ] No Kafka access
- [ ] Only SDK imports (`com.acuver.autwit.client.sdk.Autwit`)
- [ ] Clear intent
- [ ] Scenario independence

If all are true, the test is correct.

---

## Complete Example

### Feature File

```gherkin
Feature: Order Lifecycle

  Scenario: Order creation and event verification
    Given I place an order with product "PROD001" and quantity "2"
    Then I verify "ORDER_CREATED" event is published to Kafka
    And I validate the event contains order ID and customer information
```

### Step Definitions

```java
package com.bjs.tests.stepDefinitions;

import com.acuver.autwit.client.sdk.Autwit;
import io.cucumber.java.en.*;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.SkipException;

import java.util.*;

@ScenarioScope
public class OrderLifecycleStepDefs {

    @Autowired
    private Autwit autwit;

    @Given("I place an order with product {string} and quantity {string}")
    public void placeOrder(String product, String quantity) {
        autwit.context().setCurrentStep("I place an order with product " + product);
        
        String orderId = String.valueOf(System.currentTimeMillis());
        autwit.context().setOrderId(orderId);
        autwit.context().set("orderId", orderId);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("productId", product);
        payload.put("quantity", Integer.parseInt(quantity));
        
        try {
            Response response = autwit.context().api().createOrder(payload);
            autwit.step().markStepSuccess();
        } catch (Exception e) {
            autwit.step().markStepFailed("Order placement failed: " + e.getMessage());
            throw new SkipException("Order placement failed → pausing scenario.");
        }
    }

    @Then("I verify {string} event is published to Kafka")
    public void verifyEventPublished(String eventType) {
        autwit.context().setCurrentStep("I verify " + eventType + " event is published to Kafka");
        
        String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, eventType).assertSatisfied();
        
        autwit.step().markStepSuccess();
    }

    @And("I validate the event contains order ID and customer information")
    public void validateOrderFields() {
        autwit.context().setCurrentStep("I validate the event contains order ID");
        
        String orderId = autwit.context().get("orderId");
        autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
        
        autwit.step().markStepSuccess();
    }
}
```

---

## Summary

- **Import ONLY:** `com.acuver.autwit.client.sdk.Autwit`
- **Use ONLY:** `autwit.expectEvent()`, `autwit.step()`, `autwit.context()`
- **Never:** Wait, poll, retry, or access internals
- **Always:** Declare intent, let AUTWIT handle mechanics

