# AUTWIT FACADE ARCHITECTURAL MAPPING

**Date:** 2026-01-05  
**Purpose:** Map all client-facing functionality to single `Autwit` facade  
**Mode:** Analysis Only (No Code Changes)

---

## 1️⃣ ARCHITECTURAL MAPPING TABLE

| Current Class | Current Method | Used by Client? | Category | New Location | Reason |
|---------------|----------------|-----------------|----------|--------------|--------|
| **SDK Classes (Already Public)** |
| `Autwit` | `expectEvent(String, String)` | ✅ YES | Event Expectation | `Autwit` (keep) | Core event expectation API |
| `Autwit` | `step()` | ✅ YES | Step Lifecycle | `Autwit` (keep) | Step status tracking |
| `Autwit` | `context()` | ✅ YES | Scenario Lifecycle | `Autwit` (keep) | Context access |
| `EventExpectation` | `assertSatisfied()` | ✅ YES | Event Expectation | `EventExpectation` (keep) | Event verification |
| `ScenarioStepStatus` | `markStepSuccess()` | ✅ YES | Step Lifecycle | `Autwit.step()` (keep) | Step completion tracking |
| `ScenarioStepStatus` | `markStepFailed(String)` | ✅ YES | Step Lifecycle | `Autwit.step()` (keep) | Step failure tracking |
| `ContextAccessor` | `setCurrentStep(String)` | ✅ YES | Step Lifecycle | `Autwit.context()` (keep) | Step name tracking |
| `ContextAccessor` | `set(String, T)` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (keep) | Context data storage |
| `ContextAccessor` | `get(String)` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (keep) | Context data retrieval |
| **Internal Testkit Classes (Leaking to Clients)** |
| `ScenarioContext` | `get(String)` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (delegate) | ThreadLocal access - MUST hide |
| `ScenarioContext` | `set(String, T)` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (delegate) | ThreadLocal access - MUST hide |
| `ScenarioContext` | `api()` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (new method) | API client access - MUST hide |
| `ScenarioMDC` | `setOrderId(String)` | ✅ YES | Scenario Lifecycle | `Autwit.context()` (new method) | MDC logging - MUST hide |
| `ScenarioMDC` | `setScenario(String)` | ⚠️ INDIRECT | Scenario Lifecycle | Internal Only | Set by Hooks, not client |
| `ApiCalls` | `createOrder(Map)` | ✅ YES | Scenario Lifecycle | `Autwit.context().api()` (new) | API client - MUST hide |
| `ApiCalls` | (other API methods) | ✅ YES | Scenario Lifecycle | `Autwit.context().api()` (new) | API client - MUST hide |
| `SoftAssertUtils` | `getSoftAssert()` | ✅ YES | Scenario Lifecycle | `Autwit.assertions()` (new) | Soft assertions - MUST hide |
| **Engine Ports (Currently Leaking)** |
| `EventContextPort` | (all methods) | ⚠️ COMMENTED OUT | Internal Engine | Internal Only | Direct port access - FORBIDDEN |
| `EventMatcherPort` | (all methods) | ⚠️ COMMENTED OUT | Internal Engine | Internal Only | Direct port access - FORBIDDEN |
| `ScenarioStatePort` | (all methods) | ⚠️ COMMENTED OUT | Internal Engine | Internal Only | Direct port access - FORBIDDEN |
| **Internal Testkit (Resume Logic - FORBIDDEN)** |
| `ReusableStepDefsImpl` | `skipIfAlreadyPassed()` | ⚠️ COMMENTED OUT | Resume/Pause | Internal Only | Resume logic - MUST hide |
| `ScenarioStateTracker` | `isStepAlreadySuccessful()` | ⚠️ COMMENTED OUT | Resume/Pause | Internal Only | Resume logic - MUST hide |
| `ScenarioStateTracker` | `getStepData()` | ⚠️ COMMENTED OUT | Resume/Pause | Internal Only | Resume logic - MUST hide |

---

## 2️⃣ FINAL PUBLIC API PROPOSAL

### Core Interface

```java
package com.acuver.autwit.client.sdk;

/**
 * The ONLY public facade for AUTWIT client code.
 * 
 * Client code must depend ONLY on this interface.
 * All internals are hidden behind this facade.
 */
public interface Autwit {
    
    /**
     * Declare an event expectation.
     * 
     * @param orderId Business key (e.g., order ID, correlation ID)
     * @param eventType Event type to expect (e.g., "ORDER_CREATED")
     * @return EventExpectation for verification
     */
    EventExpectation expectEvent(String orderId, String eventType);
    
    /**
     * Access step lifecycle operations.
     * 
     * @return ScenarioStepStatus for marking step success/failure
     */
    ScenarioStepStatus step();
    
    /**
     * Access scenario context (data storage, API clients, assertions).
     * 
     * @return ContextAccessor for scenario-scoped operations
     */
    ContextAccessor context();
}
```

### Supporting Interfaces

```java
package com.acuver.autwit.client.sdk;

/**
 * Event expectation for verification.
 */
public interface EventExpectation {
    /**
     * Assert that the expected event is satisfied.
     * If event not found, scenario pauses automatically.
     * 
     * @throws SkipException if event not available (scenario pauses)
     * @throws RuntimeException if event verification fails
     */
    void assertSatisfied();
}

/**
 * Step lifecycle operations.
 */
public interface ScenarioStepStatus {
    /**
     * Mark current step as successfully completed.
     * Step name is automatically extracted from context.
     */
    void markStepSuccess();
    
    /**
     * Mark current step as failed.
     * 
     * @param reason Failure reason
     */
    void markStepFailed(String reason);
}

/**
 * Scenario-scoped context access.
 * Provides data storage, API clients, and assertions.
 */
public interface ContextAccessor {
    /**
     * Set the current step name (for step tracking).
     * 
     * @param stepName Current step name
     */
    void setCurrentStep(String stepName);
    
    /**
     * Store a value in scenario context.
     * 
     * @param key Context key
     * @param value Value to store
     * @param <T> Value type
     */
    <T> void set(String key, T value);
    
    /**
     * Retrieve a value from scenario context.
     * 
     * @param key Context key
     * @param <T> Value type
     * @return Stored value, or null if not found
     */
    <T> T get(String key);
    
    /**
     * Access API client for making HTTP calls.
     * 
     * @return ApiClient for REST API operations
     */
    ApiClient api();
    
    /**
     * Access soft assertions for validation.
     * 
     * @return SoftAssertions for non-fatal validations
     */
    SoftAssertions assertions();
    
    /**
     * Set order ID in context and MDC (for logging correlation).
     * Convenience method that sets both context and MDC.
     * 
     * @param orderId Order ID to set
     */
    void setOrderId(String orderId);
}
```

### New Supporting Interfaces (to be added)

```java
package com.acuver.autwit.client.sdk;

/**
 * API client for making HTTP calls.
 * Wraps internal ApiCalls.
 */
public interface ApiClient {
    /**
     * Create an order via REST API.
     * 
     * @param payload Order payload
     * @return Response object
     */
    Response createOrder(Map<String, Object> payload);
    
    // Other API methods as needed
}

/**
 * Soft assertions for non-fatal validations.
 * Wraps internal SoftAssertUtils.
 */
public interface SoftAssertions {
    /**
     * Get the soft assert instance.
     * 
     * @return SoftAssert instance
     */
    SoftAssert getSoftAssert();
    
    /**
     * Assert all collected soft assertions.
     * Throws if any assertion failed.
     */
    void assertAll();
}
```

---

## 3️⃣ INTERNAL DELEGATION SKETCH

### AutwitImpl Internal Structure

**AutwitImpl** will internally delegate to:

1. **Event Expectation → EventMatcherPort**
   - `expectEvent()` creates `EventExpectationImpl`
   - `EventExpectationImpl` uses `EventMatcherPort.match()`
   - If event not found, throws `SkipException` (pauses scenario)
   - Client never sees `EventMatcherPort`

2. **Step Tracking → ScenarioStatePort**
   - `step()` returns `ScenarioStepStatusImpl`
   - `ScenarioStepStatusImpl` uses `ScenarioStatePort.markStep()`
   - Extracts scenarioKey and stepName from internal context
   - Client never sees `ScenarioStatePort` or step tracking mechanics

3. **Context Access → Internal ThreadLocal + Adapters**
   - `context()` returns `ContextAccessorImpl`
   - `ContextAccessorImpl` delegates to `ScenarioContextAccessPort`
   - `ScenarioContextAccessPort` wraps internal `ScenarioContext` (ThreadLocal)
   - Client never sees ThreadLocal or internal context mechanics

4. **API Client → Internal ApiCalls**
   - `context().api()` returns `ApiClientImpl`
   - `ApiClientImpl` wraps internal `ApiCalls`
   - `ApiCalls` is retrieved from internal `ScenarioContext.api()`
   - Client never sees `ApiCalls` or internal API mechanics

5. **Soft Assertions → Internal SoftAssertUtils**
   - `context().assertions()` returns `SoftAssertionsImpl`
   - `SoftAssertionsImpl` wraps internal `SoftAssertUtils`
   - Client never sees `SoftAssertUtils` or assertion mechanics

6. **MDC Logging → Internal ScenarioMDC**
   - `context().setOrderId()` sets both context and MDC
   - Internally calls `ScenarioMDC.setOrderId()`
   - Client never sees `ScenarioMDC` or logging mechanics

**Key Principle:**
- All internal mechanics are hidden
- Client sees only intent-based APIs
- Internal changes do not affect client code
- No direct access to ports, adapters, or internal testkit

---

## 4️⃣ EXPLICIT NON-GOALS

### What MUST NOT Be Added to Autwit

1. **Engine Ports:**
   - ❌ `EventContextPort`
   - ❌ `EventMatcherPort`
   - ❌ `ScenarioStatePort`
   - ❌ `ScenarioContextPort`
   - ❌ Any port interface

2. **Resume/Pause APIs:**
   - ❌ `pause()`
   - ❌ `resume()`
   - ❌ `isPaused()`
   - ❌ `isResumeReady()`
   - ❌ `skipIfAlreadyPassed()`
   - ❌ Any resume-related method

3. **Internal Testkit Classes:**
   - ❌ `ScenarioContext` (direct access)
   - ❌ `ScenarioMDC` (direct access)
   - ❌ `ApiCalls` (direct access)
   - ❌ `SoftAssertUtils` (direct access)
   - ❌ `ReusableStepDefs` (direct access)

4. **Time-Based APIs:**
   - ❌ `expectEventWithin(timeout)`
   - ❌ `waitFor(timeout)`
   - ❌ `pollUntil(condition, timeout)`
   - ❌ Any timeout or time-based method

5. **Database/Kafka APIs:**
   - ❌ `findEvent()`
   - ❌ `queryDatabase()`
   - ❌ `pollKafka()`
   - ❌ Any direct data access

6. **Execution Control:**
   - ❌ `executeStep()`
   - ❌ `skipStep()`
   - ❌ `retryStep()`
   - ❌ Any execution control method

7. **State Queries:**
   - ❌ `getScenarioState()`
   - ❌ `isStepSuccessful()`
   - ❌ `getStepData()`
   - ❌ Any state query method

8. **Multiple Facades:**
   - ❌ `EventFacade`
   - ❌ `StepFacade`
   - ❌ `ContextFacade`
   - ❌ Any additional facade interfaces

---

## 5️⃣ MIGRATION IMPACT ANALYSIS

### Current Client Code Patterns

**Pattern 1: Event Expectation (✅ Already Compliant)**
```java
autwit.expectEvent(orderId, eventType).assertSatisfied();
```
**Status:** ✅ No change needed

**Pattern 2: Step Tracking (✅ Already Compliant)**
```java
autwit.step().markStepSuccess();
autwit.step().markStepFailed(reason);
```
**Status:** ✅ No change needed

**Pattern 3: Context Access (⚠️ Needs Update)**
```java
// CURRENT (leaks internals):
ScenarioContext.set("orderId", orderId);
String orderId = ScenarioContext.get("orderId");

// TARGET (facade only):
autwit.context().set("orderId", orderId);
String orderId = autwit.context().get("orderId");
```
**Status:** ⚠️ Requires migration

**Pattern 4: API Client (⚠️ Needs Update)**
```java
// CURRENT (leaks internals):
ApiCalls api = ScenarioContext.api();
Response response = api.createOrder(payload);

// TARGET (facade only):
Response response = autwit.context().api().createOrder(payload);
```
**Status:** ⚠️ Requires migration

**Pattern 5: Soft Assertions (⚠️ Needs Update)**
```java
// CURRENT (leaks internals):
SoftAssertUtils.getSoftAssert().assertEquals(...);

// TARGET (facade only):
autwit.context().assertions().getSoftAssert().assertEquals(...);
```
**Status:** ⚠️ Requires migration

**Pattern 6: MDC Logging (⚠️ Needs Update)**
```java
// CURRENT (leaks internals):
ScenarioMDC.setOrderId(orderId);

// TARGET (facade only):
autwit.context().setOrderId(orderId);
```
**Status:** ⚠️ Requires migration

**Pattern 7: Current Step Tracking (✅ Already Compliant)**
```java
autwit.context().setCurrentStep(stepName);
```
**Status:** ✅ No change needed

---

## 6️⃣ COMPLETENESS CHECKLIST

### Required Facade Methods

| Client Need | Current Status | Required Method | Priority |
|-------------|----------------|-----------------|----------|
| Event expectation | ✅ Exists | `expectEvent()` | ✅ Complete |
| Step success tracking | ✅ Exists | `step().markStepSuccess()` | ✅ Complete |
| Step failure tracking | ✅ Exists | `step().markStepFailed()` | ✅ Complete |
| Context data storage | ✅ Exists | `context().set()` | ✅ Complete |
| Context data retrieval | ✅ Exists | `context().get()` | ✅ Complete |
| Current step name | ✅ Exists | `context().setCurrentStep()` | ✅ Complete |
| API client access | ❌ Missing | `context().api()` | 🔴 HIGH |
| Soft assertions | ❌ Missing | `context().assertions()` | 🔴 HIGH |
| MDC order ID | ❌ Missing | `context().setOrderId()` | 🟡 MEDIUM |

---

## 7️⃣ SUMMARY

### Current State
- ✅ Core facade exists (`Autwit` interface)
- ✅ Event expectation API complete
- ✅ Step tracking API complete
- ⚠️ Context API incomplete (missing API client, assertions, MDC)
- ❌ Clients still import internal testkit classes

### Target State
- ✅ Single `Autwit` facade
- ✅ All client needs met through facade
- ✅ Zero internal testkit imports
- ✅ Zero engine port imports
- ✅ Complete encapsulation

### Required Additions
1. `ContextAccessor.api()` → Returns `ApiClient`
2. `ContextAccessor.assertions()` → Returns `SoftAssertions`
3. `ContextAccessor.setOrderId(String)` → Sets context + MDC

### Required Removals
1. Remove direct `ScenarioContext` imports from clients
2. Remove direct `ScenarioMDC` imports from clients
3. Remove direct `ApiCalls` imports from clients
4. Remove direct `SoftAssertUtils` imports from clients

---

**END OF ANALYSIS**

