# AUTWIT Architecture Correction Analysis

**Date:** 2026-01-06  
**Purpose:** Complete analysis and correction plan for context-related architecture violations

---

## EXECUTIVE SUMMARY

The AUTWIT codebase has **critical architectural violations** that must be corrected:

1. **Port defined in wrong module** — `RuntimeContextPort` in `client-sdk` (should be in `core`)
2. **Missing implementation** — `RuntimeContextPort` has no Spring bean implementation
3. **Dependency violation** — `client-sdk` depends on `internal-testkit` (compile-time)
4. **Name collision** — Two `ScenarioContext` classes with different purposes
5. **Missing payload validation** — No safe way to access event payloads
6. **Boundary leakage** — Internal mechanics exposed to SDK

---

## PART 1: CURRENT STATE ANALYSIS

### 1.1 Context-Related Artifacts Inventory

#### A) Runtime Scenario Context (ThreadLocal)

| Class | Module | Purpose | Status |
|-------|--------|---------|--------|
| `ScenarioContext` | `autwit-internal-testkit` | ThreadLocal runtime context | ✅ CORRECT location |
| `ScenarioMDC` | `autwit-internal-testkit` | MDC logging correlation | ✅ CORRECT location |
| `RuntimeContextPort` | `autwit-client-sdk` | ❌ **WRONG MODULE** | ❌ VIOLATION |

**Problem:** `RuntimeContextPort` is defined in `client-sdk` but should be in `autwit-core/ports`.

#### B) Persisted Scenario State (Database)

| Class | Module | Purpose | Status |
|-------|--------|---------|--------|
| `ScenarioContext` (domain) | `autwit-core/autwit-domain` | Persisted scenario state | ✅ CORRECT location |
| `ScenarioContextPort` | `autwit-core/autwit-domain/ports` | DB persistence port | ✅ CORRECT location |
| `MongoScenarioContextAdapter` | `autwit-adapter-mongo` | Mongo implementation | ✅ CORRECT location |
| `PostgresScenarioContextAdapter` | `autwit-adapter-postgres` | Postgres implementation | ✅ CORRECT location |
| `H2ScenarioContextAdapter` | `autwit-adapter-h2` | H2 implementation | ✅ CORRECT location |

**Problem:** Name collision — `ScenarioContext` exists in both runtime and persistence layers.

#### C) Client-Facing Context Access

| Class | Module | Purpose | Status |
|-------|--------|---------|--------|
| `Autwit.ContextAccessor` | `autwit-client-sdk` | Client-facing context API | ✅ CORRECT location |
| `ContextAccessorImpl` | `autwit-client-sdk` | Implementation | ⚠️ Uses `RuntimeContextPort` |
| `ApiClientImpl` | `autwit-client-sdk` | API client wrapper | ⚠️ Uses `RuntimeContextPort` |

**Problem:** SDK depends on port that doesn't exist in correct location.

---

### 1.2 Dependency Violations

#### Violation 1: Port in Wrong Module

```
autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/RuntimeContextPort.java
```

**Issue:** Ports MUST be in `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/`

**Current:** Port defined in SDK  
**Should be:** Port defined in core

#### Violation 2: SDK Depends on Internal-Testkit

```xml
<!-- autwit-client-sdk/pom.xml -->
<dependency>
    <groupId>com.acuver</groupId>
    <artifactId>autwit-internal-testkit</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

**Issue:** SDK MUST NOT have compile-time dependency on internal-testkit.

**Current:** Direct compile dependency  
**Should be:** Runtime-only (reflection) or port-based

#### Violation 3: Missing Implementation

`RuntimeContextPort` has no Spring bean implementation, causing:
```
Field contextAccess in com.acuver.autwit.client.sdk.AutwitImpl required a bean 
of type 'com.acuver.autwit.client.sdk.RuntimeContextPort' that could not be found.
```

**Issue:** Port exists but no adapter implements it.

---

### 1.3 Name Collision Analysis

| Class Name | Module | Type | Purpose |
|------------|--------|------|---------|
| `ScenarioContext` | `autwit-internal-testkit` | ThreadLocal utility | Runtime context (per-thread) |
| `ScenarioContext` | `autwit-core/autwit-domain` | Domain model | Persisted state (DB) |

**Problem:** Same name, different purposes, different modules.

**Solution:** Rename one to clarify intent.

---

### 1.4 Missing Payload Validation

**Current State:**
```java
// EventExpectationImpl.java
EventContext ctx = matcher.match(orderId, eventType).getNow(null);
if (ctx == null) {
    throw new SkipException("Event not yet available");
}
// ❌ No way to access payload for validation
```

**Required:**
```java
autwit.expectEvent(orderId, "ORDER_CREATED")
    .assertPayload(p -> p.getStatus().equals("CREATED"));
```

**Problem:** No safe way to access payload without exposing `EventContext` domain model.

---

## PART 2: CORRECTED ARCHITECTURE

### 2.1 Clean Conceptual Model

#### A) Runtime Scenario Context (ThreadLocal)

**Module:** `autwit-internal-testkit`  
**Purpose:** Per-scenario, per-thread execution state  
**Access:** Via port from internal-testkit  
**Visibility:** Internal only

**Classes:**
- `RuntimeScenarioContext` (rename from `ScenarioContext`) — ThreadLocal utility
- `ScenarioMDC` — MDC logging correlation

**Port:**
- `RuntimeContextPort` (move to `autwit-core/ports`) — Interface for runtime context access

**Implementation:**
- `RuntimeContextAdapter` (in `autwit-internal-testkit`) — Implements port, accesses ThreadLocal

#### B) Persisted Scenario State (Database)

**Module:** `autwit-core/autwit-domain`  
**Purpose:** Scenario state persisted for resume  
**Access:** Via `ScenarioContextPort` (already correct)  
**Visibility:** Internal only

**Classes:**
- `PersistedScenarioState` (rename from `ScenarioContext`) — Domain model for DB

**Port:**
- `ScenarioContextPort` (keep as-is) — Already in correct location

**Implementation:**
- `MongoScenarioContextAdapter`, `PostgresScenarioContextAdapter`, `H2ScenarioContextAdapter` (already correct)

#### C) Client-Facing Context Access

**Module:** `autwit-client-sdk`  
**Purpose:** Intent-level access for client tests  
**Access:** Via `Autwit.context()`  
**Visibility:** Public (only facade)

**Classes:**
- `Autwit.ContextAccessor` — Public interface
- `ContextAccessorImpl` — Implementation (uses `RuntimeContextPort`)

**No direct access to:**
- ThreadLocal
- Spring beans
- DB entities
- Internal models

---

### 2.2 Corrected Dependency Flow

```
client-tests
    ↓ (depends on)
autwit-client-sdk
    ↓ (depends on ports only)
autwit-core/autwit-domain (ports)
    ↑ (implemented by)
autwit-internal-testkit (RuntimeContextAdapter)
    ↑ (wired by)
autwit-runner
```

**Key Rules:**
1. `client-sdk` depends on `autwit-domain` (ports only)
2. `internal-testkit` implements ports from `autwit-domain`
3. `client-sdk` uses reflection OR port injection (no compile dependency on internal-testkit)
4. `runner` wires everything together

---

### 2.3 Final Class List

#### autwit-core/autwit-domain/ports

| Class | Purpose | Status |
|-------|---------|--------|
| `RuntimeContextPort` | Interface for runtime context access | ✅ **MOVE FROM SDK** |
| `ScenarioContextPort` | Interface for persisted state | ✅ Keep (already correct) |
| `EventMatcherPort` | Event matching | ✅ Keep (already correct) |
| `ScenarioStatePort` | Step state tracking | ✅ Keep (already correct) |
| `EventContextPort` | Event persistence | ✅ Keep (already correct) |

#### autwit-core/autwit-domain/domain

| Class | Purpose | Status |
|-------|---------|--------|
| `PersistedScenarioState` | Renamed from `ScenarioContext` | ✅ **RENAME** |
| `EventContext` | Event domain model | ✅ Keep (already correct) |

#### autwit-internal-testkit

| Class | Purpose | Status |
|-------|---------|--------|
| `RuntimeScenarioContext` | Renamed from `ScenarioContext` | ✅ **RENAME** |
| `RuntimeContextAdapter` | Implements `RuntimeContextPort` | ✅ **CREATE** |
| `ScenarioMDC` | MDC logging | ✅ Keep (already correct) |
| `ApiCalls` | API client | ✅ Keep (already correct) |
| `SoftAssertUtils` | Soft assertions | ✅ Keep (already correct) |

#### autwit-client-sdk

| Class | Purpose | Status |
|-------|---------|--------|
| `Autwit` | Public facade | ✅ Keep (enhance) |
| `AutwitImpl` | Implementation | ✅ Keep (fix dependencies) |
| `ContextAccessorImpl` | Context access implementation | ✅ Keep (fix dependencies) |
| `EventExpectationImpl` | Event expectation | ✅ Keep (enhance for payload) |
| `ApiClientImpl` | API client wrapper | ✅ Keep (fix dependencies) |
| `SoftAssertionsImpl` | Soft assertions wrapper | ✅ Keep (already uses reflection) |
| `ScenarioStepStatusImpl` | Step status | ✅ Keep (already correct) |

---

## PART 3: DELETION LIST

### Files to DELETE

1. **`autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/RuntimeContextPort.java`**
   - **Reason:** Port belongs in `autwit-core/ports`
   - **Action:** Move to `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/RuntimeContextPort.java`

### Files to RENAME

1. **`autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/domain/ScenarioContext.java`**
   - **New name:** `PersistedScenarioState.java`
   - **Reason:** Clarify it's persisted state, not runtime context

2. **`autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/context/ScenarioContext.java`**
   - **New name:** `RuntimeScenarioContext.java`
   - **Reason:** Clarify it's runtime ThreadLocal, not persisted state

### References to Update

After renaming, update all references:
- `ScenarioContextPort` → Update to use `PersistedScenarioState`
- All adapters (Mongo, Postgres, H2) → Update to use `PersistedScenarioState`
- `Hooks.java` → Update to use `PersistedScenarioState`
- `ScenarioStepStatusImpl.java` → Remove import of `ScenarioContext` (internal-testkit)

---

## PART 4: FINAL AUTWIT FACADE API

### 4.1 Enhanced Autwit Interface

```java
package com.acuver.autwit.client.sdk;

public interface Autwit {
    
    /**
     * Event expectation with optional payload validation.
     */
    interface EventExpectation {
        /**
         * Assert that the expected event is satisfied.
         * If event not found, scenario pauses automatically.
         * 
         * @throws org.testng.SkipException if event not available (scenario pauses)
         * @throws RuntimeException if event verification fails
         */
        void assertSatisfied();
        
        /**
         * Validate event payload using a read-only function.
         * 
         * @param validator Function that receives payload as Map and returns boolean
         * @return this for method chaining
         * @throws org.testng.SkipException if event not available
         * @throws RuntimeException if validation fails
         */
        EventExpectation assertPayload(java.util.function.Function<Map<String, Object>, Boolean> validator);
    }
    
    /**
     * Step lifecycle operations.
     */
    interface ScenarioStepStatus {
        void markStepSuccess();
        void markStepFailed(String reason);
    }
    
    /**
     * Scenario-scoped context access.
     */
    interface ContextAccessor {
        void setCurrentStep(String stepName);
        <T> void set(String key, T value);
        <T> T get(String key);
        ApiClient api();
        SoftAssertions assertions();
        void setOrderId(String orderId);
        
        interface ApiClient {
            Response createOrder(Map<String, Object> payload);
        }
        
        interface SoftAssertions {
            SoftAssert getSoftAssert();
            void assertAll();
        }
    }
    
    // Main methods
    EventExpectation expectEvent(String orderId, String eventType);
    ScenarioStepStatus step();
    ContextAccessor context();
}
```

### 4.2 Payload Validation Implementation

```java
// EventExpectationImpl.java
class EventExpectationImpl implements Autwit.EventExpectation {
    private final EventMatcherPort matcher;
    private final String orderId;
    private final String eventType;
    private java.util.function.Function<Map<String, Object>, Boolean> payloadValidator;
    
    @Override
    public void assertSatisfied() {
        EventContext ctx = matcher.match(orderId, eventType).getNow(null);
        if (ctx == null) {
            throw new SkipException("Event not yet available: " + eventType + " for orderId=" + orderId);
        }
        
        // Apply payload validation if provided
        if (payloadValidator != null) {
            Map<String, Object> payload = parsePayload(ctx.getKafkaPayload());
            if (!payloadValidator.apply(payload)) {
                throw new RuntimeException("Payload validation failed for event: " + eventType);
            }
        }
    }
    
    @Override
    public EventExpectation assertPayload(java.util.function.Function<Map<String, Object>, Boolean> validator) {
        this.payloadValidator = validator;
        return this;
    }
    
    private Map<String, Object> parsePayload(String json) {
        // Parse JSON to Map (read-only access)
        // Use Jackson or similar
    }
}
```

---

## PART 5: END-TO-END PAYLOAD VALIDATION FLOW

### 5.1 Client Usage

```java
@Autowired
private Autwit autwit;

@When("I create an order")
public void createOrder() {
    Map<String, Object> payload = Map.of("item", "book", "quantity", 1);
    Response response = autwit.context().api().createOrder(payload);
    String orderId = response.jsonPath().getString("orderId");
    autwit.context().setOrderId(orderId);
}

@Then("I verify the order was created with correct status")
public void verifyOrderCreated() {
    String orderId = autwit.context().get("orderId");
    
    autwit.expectEvent(orderId, "ORDER_CREATED")
        .assertPayload(payload -> {
            String status = (String) payload.get("status");
            return "CREATED".equals(status);
        })
        .assertSatisfied();
}
```

### 5.2 Execution Flow

1. **Client calls:** `autwit.expectEvent(orderId, "ORDER_CREATED")`
   - Returns `EventExpectationImpl` (not exposed to client)

2. **Client calls:** `.assertPayload(validator)`
   - Stores validator function in `EventExpectationImpl`
   - Returns `this` for chaining

3. **Client calls:** `.assertSatisfied()`
   - `EventExpectationImpl.assertSatisfied()` executes:
     - Calls `EventMatcherPort.match(orderId, eventType)`
     - Gets `EventContext` from DB (internal domain model)
     - If null → throws `SkipException` (pause)
     - If found → extracts `kafkaPayload` (JSON string)
     - Parses JSON to `Map<String, Object>` (read-only)
     - Applies validator function to payload map
     - If validator returns false → throws `RuntimeException` (failure)
     - If validator returns true → continues (success)

4. **Key Points:**
   - Client never sees `EventContext` domain model
   - Client only sees `Map<String, Object>` (read-only)
   - Payload parsing happens inside SDK
   - Validation is functional (no side effects)

---

## PART 6: IMPLEMENTATION PLAN

### Phase 1: Move Port to Core

1. Create `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/RuntimeContextPort.java`
2. Delete `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/RuntimeContextPort.java`
3. Update imports in `AutwitImpl`, `ContextAccessorImpl`, `ApiClientImpl`

### Phase 2: Create Implementation

1. Create `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/context/RuntimeContextAdapter.java`
2. Implement `RuntimeContextPort`
3. Use reflection or direct access to `RuntimeScenarioContext` (ThreadLocal)
4. Register as Spring `@Component`

### Phase 3: Rename Classes

1. Rename `ScenarioContext` (domain) → `PersistedScenarioState`
2. Rename `ScenarioContext` (internal-testkit) → `RuntimeScenarioContext`
3. Update all references

### Phase 4: Remove SDK Dependency on Internal-Testkit

1. Remove `<dependency>` from `autwit-client-sdk/pom.xml`
2. Ensure all access via reflection (already done for `ScenarioMDC`, `SoftAssertUtils`)
3. Ensure `RuntimeContextPort` is injected (not accessed directly)

### Phase 5: Add Payload Validation

1. Enhance `EventExpectation` interface
2. Implement `assertPayload()` in `EventExpectationImpl`
3. Add JSON parsing (read-only)

### Phase 6: Update Runner Configuration

1. Ensure `RuntimeContextAdapter` is scanned by Spring
2. Verify `RuntimeContextPort` bean is available
3. Test end-to-end flow

---

## PART 7: VERIFICATION CHECKLIST

- [ ] `RuntimeContextPort` moved to `autwit-core/ports`
- [ ] `RuntimeContextAdapter` created in `internal-testkit`
- [ ] `ScenarioContext` (domain) renamed to `PersistedScenarioState`
- [ ] `ScenarioContext` (internal-testkit) renamed to `RuntimeScenarioContext`
- [ ] All references updated
- [ ] SDK dependency on `internal-testkit` removed
- [ ] Payload validation added to `EventExpectation`
- [ ] Spring beans wired correctly
- [ ] Tests pass
- [ ] No compile-time dependencies from SDK to internal-testkit

---

## SUMMARY

### Key Corrections

1. **Move `RuntimeContextPort`** from SDK to core
2. **Create `RuntimeContextAdapter`** in internal-testkit
3. **Rename classes** to avoid collision
4. **Remove SDK dependency** on internal-testkit
5. **Add payload validation** to EventExpectation
6. **Wire Spring beans** correctly

### Final Architecture

- **Runtime Context:** ThreadLocal in `internal-testkit`, accessed via port
- **Persisted State:** Domain model in `core/domain`, accessed via port
- **Client Access:** Facade in `client-sdk`, hides all internals
- **Dependencies:** SDK → Core (ports only), Internal-Testkit → Core (implements ports)

---

**END OF ANALYSIS**

