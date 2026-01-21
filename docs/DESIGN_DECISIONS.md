# AUTWIT Design Decisions

This document explains the key design decisions in AUTWIT and why they were made.

---

## Why Single Autwit Facade

### Decision

AUTWIT exposes **only one public interface**: `Autwit`. All client functionality is accessed through this single facade.

### Rationale

1. **Simplicity:** Clients have one entry point, not many
2. **Stability:** Internal changes don't affect client code
3. **Boundary Enforcement:** Clear separation between client and internal code
4. **Discoverability:** All functionality is discoverable through one interface

### Implementation

```java
// Client code uses ONLY this:
@Autowired
private Autwit autwit;

// All functionality accessed via autwit:
autwit.expectEvent(orderId, eventType).assertSatisfied();
autwit.step().markStepSuccess();
autwit.context().set("key", value);
autwit.context().api().createOrder(payload);
```

### Alternative Considered

Multiple public interfaces (`EventMatcher`, `StepTracker`, `ContextManager`) were considered but rejected because:
- Clients would need to know multiple interfaces
- Internal refactoring would break client code
- Boundary enforcement would be harder

---

## Why Reflection Is Used

### Decision

SDK implementations use **reflection** to access internal testkit classes instead of direct compile-time dependencies.

### Rationale

1. **Boundary Enforcement:** SDK cannot have compile-time dependency on `autwit-internal-testkit`
2. **Client Isolation:** Clients cannot accidentally import internal classes
3. **Flexibility:** Internal testkit can be refactored without breaking SDK
4. **Runtime Safety:** Reflection failures are caught and wrapped in clear exceptions

### Implementation

```java
// In ContextAccessorImpl.setOrderId()
Class<?> mdcClass = Class.forName("com.acuver.autwit.internal.context.ScenarioMDC");
java.lang.reflect.Method method = mdcClass.getMethod("setOrderId", String.class);
method.invoke(null, orderId);
```

### Trade-offs

**Pros:**
- Strict boundary enforcement
- No compile-time coupling
- Internal refactoring doesn't break SDK

**Cons:**
- Runtime failures instead of compile-time errors
- Slightly more complex code
- Reflection overhead (minimal)

### Alternative Considered

Direct dependencies were considered but rejected because:
- Would allow clients to import internal classes
- Would create tight coupling between SDK and testkit
- Would make internal refactoring harder

---

## Why No Multiple AutwitImpl Variants

### Decision

There is **only one** `AutwitImpl` implementation. No variants, no factory methods, no conditional implementations.

### Rationale

1. **Simplicity:** One implementation is easier to understand and maintain
2. **Consistency:** All clients use the same implementation
3. **Testability:** Easier to test and verify behavior
4. **No Configuration Complexity:** No need to choose between implementations

### Implementation

```java
@Component
class AutwitImpl implements Autwit {
    // Single implementation, wired by Spring
}
```

### Alternative Considered

Multiple implementations (e.g., `AutwitImpl`, `MockAutwitImpl`, `TestAutwitImpl`) were considered but rejected because:
- Would add unnecessary complexity
- Testing can use Spring test context
- Mocking can be done at the interface level

---

## Why Runner Is Thin

### Decision

The `autwit-runner` module is **thin** — it only bootstraps Spring, configures TestNG/Cucumber, and wires components. It contains no business logic, no feature files, no step definitions.

### Rationale

1. **Separation of Concerns:** Runner is infrastructure, not test logic
2. **Replaceability:** Runner can be replaced without affecting tests
3. **Clarity:** Clear boundary between infrastructure and test code
4. **Maintainability:** Less code in runner means less to maintain

### Implementation

```java
@SpringBootApplication(scanBasePackages = "com.acuver.autwit")
public class RunnerApp {
    public static void main(String[] args) {
        SpringApplication.run(RunnerApp.class, args);
        // TestNG execution
    }
}
```

### What Runner Does

- Bootstraps Spring Boot
- Configures TestNG
- Configures Cucumber
- Wires all AUTWIT components
- Starts test execution

### What Runner Does NOT Do

- Own feature files
- Own step definitions
- Contain business logic
- Decide resume eligibility
- Match events

### Alternative Considered

A "fat" runner with feature files and step definitions was considered but rejected because:
- Would mix infrastructure and test code
- Would make runner harder to replace
- Would violate separation of concerns

---

## Why SDK Hides Internals

### Decision

The SDK **hides all internal mechanics** from clients. Clients see only the `Autwit` facade and nested interfaces.

### Rationale

1. **Stability:** Internal changes don't break client code
2. **Simplicity:** Clients don't need to understand internals
3. **Boundary Enforcement:** Clients cannot access forbidden APIs
4. **Future-Proofing:** Internal refactoring is safe

### Implementation

```java
// Public (visible to clients):
public interface Autwit { ... }

// Package-private (hidden from clients):
class AutwitImpl implements Autwit { ... }
class EventExpectationImpl implements Autwit.EventExpectation { ... }
class ContextAccessorImpl implements Autwit.ContextAccessor { ... }
```

### What Is Hidden

- All `*Impl` classes
- Internal ports (`ScenarioContextAccessPort`)
- Internal testkit classes
- Engine internals
- Adapter internals

### Alternative Considered

Exposing implementation classes was considered but rejected because:
- Would allow clients to depend on implementation details
- Would make refactoring harder
- Would violate encapsulation

---

## Why SkipException for Pause

### Decision

Scenarios pause by throwing `SkipException` (TestNG's skip mechanism), not by returning a special value or setting a flag.

### Rationale

1. **Framework Integration:** TestNG understands `SkipException` natively
2. **Thread Release:** Exception causes thread to be released immediately
3. **Clear Semantics:** Skip is clearly different from failure
4. **No Blocking:** Exception-based pause doesn't block threads

### Implementation

```java
if (eventNotFound) {
    throw new SkipException("Event not available → pausing scenario.");
}
```

### Alternative Considered

Returning a pause status or setting a flag was considered but rejected because:
- Would require checking return values everywhere
- Would not release threads automatically
- Would be less clear than exception-based pause

---

## Why No Time-Based APIs

### Decision

AUTWIT has **no time-based APIs** in the client SDK. No timeouts, no waits, no sleeps.

### Rationale

1. **Event-Driven:** Tests should be driven by events, not time
2. **Determinism:** Time-based logic is non-deterministic
3. **Correctness:** Tests should wait for correctness, not time
4. **Simplicity:** No time parameters to configure or tune

### What Is Forbidden

```java
// ALL FORBIDDEN:
Thread.sleep(...)
CompletableFuture.get(timeout, TimeUnit)
Awaitility.await(...)
withinSeconds(...)
```

### Alternative Considered

Time-based APIs were considered but rejected because:
- Would encourage time-based thinking
- Would make tests flaky
- Would violate event-driven principle

---

## Why Database Is Source of Truth

### Decision

The **database is the single source of truth** for event matching. In-memory structures are optimization only.

### Rationale

1. **Persistence:** Events must survive JVM restarts
2. **Determinism:** Database queries are deterministic
3. **Correctness:** Database state is authoritative
4. **Resume:** Resume requires persisted state

### Implementation

```java
// Event matching ALWAYS checks database first:
EventContext event = storagePort.findByCanonicalKey(canonicalKey);
if (event == null) {
    throw new SkipException("Event not found → pausing");
}
```

### Alternative Considered

In-memory-only matching was considered but rejected because:
- Would not survive JVM restarts
- Would not support resume
- Would be non-deterministic

---

## Why Canonical Keys

### Decision

Events are matched using **canonical keys** (e.g., `orderId + eventType`), not payload content or timestamps.

### Rationale

1. **Determinism:** Same key always matches same event
2. **Simplicity:** Key construction is straightforward
3. **Performance:** Key-based lookup is fast
4. **Stability:** Keys don't change over time

### Implementation

```java
String canonicalKey = orderId + ":" + eventType;
EventContext event = storagePort.findByCanonicalKey(canonicalKey);
```

### Alternative Considered

Payload-based matching was considered but rejected because:
- Would require parsing payloads
- Would be slower
- Would be less deterministic
- Would couple engine to payload structure

---

## Why ResumeEngine Is Sole Authority

### Decision

**ResumeEngine is the ONLY component** allowed to mark scenarios as `RESUME_READY`. No other component can make this decision.

### Rationale

1. **Single Source of Truth:** One component makes resume decisions
2. **Consistency:** All resume logic in one place
3. **Testability:** Easier to test resume logic
4. **Correctness:** Prevents conflicting resume decisions

### Implementation

```java
// ONLY ResumeEngine can do this:
storagePort.markResumeReady(canonicalKey);

// Pollers, adapters, runners CANNOT do this
```

### Alternative Considered

Distributed resume decision-making was considered but rejected because:
- Would create race conditions
- Would be harder to test
- Would violate single responsibility principle

---

## Why Nested Interfaces

### Decision

Supporting interfaces (`EventExpectation`, `ScenarioStepStatus`, `ContextAccessor`) are **nested inside** the `Autwit` interface.

### Rationale

1. **Namespace:** All client APIs under one namespace
2. **Discoverability:** Related interfaces are grouped together
3. **Stability:** Nested interfaces are part of the facade contract
4. **Clarity:** Clear that these are part of the Autwit API

### Implementation

```java
public interface Autwit {
    interface EventExpectation { ... }
    interface ScenarioStepStatus { ... }
    interface ContextAccessor { ... }
}
```

### Alternative Considered

Separate top-level interfaces were considered but rejected because:
- Would create multiple public APIs
- Would be less discoverable
- Would violate single facade principle

---

## Summary

All design decisions in AUTWIT prioritize:
1. **Correctness** over convenience
2. **Simplicity** over flexibility
3. **Stability** over features
4. **Boundary enforcement** over ease of use

These decisions make AUTWIT harder to misuse but easier to maintain and evolve.

