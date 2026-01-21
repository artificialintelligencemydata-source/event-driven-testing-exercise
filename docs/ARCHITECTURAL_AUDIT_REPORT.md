# AUTWIT ARCHITECTURAL ALIGNMENT AUDIT REPORT

**Date:** 2026-01-05  
**Audit Scope:** Complete codebase alignment against specification documents  
**Mode:** Audit Only (No Code Modifications)

---

## 1. HIGH-LEVEL ALIGNMENT SUMMARY

The AUTWIT codebase demonstrates **PARTIAL ALIGNMENT** with the architectural specifications. While core concepts are implemented (pause/resume, event matching, ResumeEngine), there are **CRITICAL VIOLATIONS** that break architectural boundaries and violate fundamental principles.

**Key Findings:**
- Architecture boundaries are violated (client code imports engine ports)
- Timeout-based logic violates event-driven principles
- Scenario state model is implicit rather than explicit
- Runner lacks RESUME_READY discovery mechanism
- Scenario key generation uses timestamps (violates stability)

---

## 2. VERIFIED ALIGNED AREAS

### 2.1 Resume Engine Core Logic
- ✅ `ResumeEngine` only marks `resumeReady=true` (does not execute tests)
- ✅ ResumeEngine persists state via `EventContextPort`
- ✅ ResumeEngine uses canonical key for matching
- ✅ ResumeEngine does not call client code

### 2.2 Pause Implementation
- ✅ `SkipException` is used correctly for pausing scenarios
- ✅ Pauses are not treated as test failures in framework

### 2.3 Event Persistence
- ✅ Events are persisted to DB before matching
- ✅ DB-first matching is implemented in `EventStepNotifier.match()`

### 2.4 Module Structure
- ✅ Clear separation between `autwit-core`, `autwit-client-sdk`, `autwit-runner`
- ✅ Adapters are properly isolated

---

## 3. VIOLATIONS (CRITICAL)

### 3.1 ARCHITECTURE BOUNDARIES — CRITICAL

**Violation:** Client code directly imports engine ports

**Location:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`

**Evidence:**
```java
import com.acuver.autwit.core.ports.EventContextPort;
import com.acuver.autwit.core.ports.EventMatcherPort;
import com.acuver.autwit.core.ports.ScenarioStatePort;
```

**Specification Violated:** 
- `ARCHITECTURAL_GUARDRAILS.md`: "DO NOT import engine classes"
- `ARCHITECTURAL_GUARDRAILS.md`: "DO NOT import ports or adapters"
- `RUNNER_VS_CLIENT_BOUNDARY.md`: Client code must only depend on client SDK

**Impact:** CRITICAL — Breaks encapsulation, allows client code to bypass SDK facade

---

### 3.2 EVENT MATCHING — TIMEOUT VIOLATIONS — CRITICAL

**Violation:** Client code uses timeouts for event matching

**Location:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`

**Evidence:**
```java
EventContext ctx = eventStepNotifier.match(orderId, eventType)
        .get(10, TimeUnit.SECONDS);  // Lines 119, 157, 193, 229, 265, 301
```

**Specification Violated:**
- `EVENT_MATCHING_RULES.md`: "NO OTHER FLOW IS VALID" (only DB-first, no waiting)
- `FAILURE_VS_PAUSE_SEMANTICS.md`: "Timeouts are forbidden in AUTWIT"
- `EVENT_MATCHING_RULES.md`: "Using sleeps or waits" is FORBIDDEN

**Impact:** CRITICAL — Violates core event-driven principle, introduces timing dependencies

---

### 3.3 CLIENT SDK TIMEOUT USAGE — CRITICAL

**Violation:** Client SDK uses internal timeout

**Location:** `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/EventExpectationImpl.java`

**Evidence:**
```java
matcher.match(orderId, eventType)
    .get(INTERNAL_YIELD_SECONDS, TimeUnit.SECONDS);  // Line 30
```

**Specification Violated:**
- `EVENT_MATCHING_RULES.md`: "No waiting occurs in client code"
- `FAILURE_VS_PAUSE_SEMANTICS.md`: "Timeouts are forbidden in AUTWIT"

**Impact:** CRITICAL — SDK should not wait, should immediately pause if event not found

---

### 3.4 SCENARIO STATE MODEL — MISSING EXPLICIT STATES — CRITICAL

**Violation:** No explicit state enum or state machine implementation

**Location:** Entire codebase

**Evidence:**
- States are represented as boolean flags (`paused`, `resumeReady`) in `EventContext`
- No enum for: CREATED, RUNNING, WAITING_FOR_EVENT, PAUSED, RESUME_READY, RESUMING, COMPLETED, FAILED
- No state transition validation

**Specification Violated:**
- `SCENARIO_STATE_MODEL.md`: "AUTWIT scenarios can exist in the following states: 1) CREATED 2) RUNNING 3) WAITING_FOR_EVENT 4) PAUSED 5) RESUME_READY 6) RESUMING 7) COMPLETED 8) FAILED"
- `SCENARIO_STATE_MODEL.md`: "NO OTHER STATES ARE VALID"

**Impact:** CRITICAL — Cannot enforce legal transitions, cannot prevent illegal states

---

### 3.5 RUNNER RESUME DISCOVERY — MISSING — CRITICAL

**Violation:** Runner does not query for RESUME_READY scenarios

**Location:** `autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberTestRunner.java`

**Evidence:**
- Runner extends `AbstractTestNGCucumberTests` and only uses `super.scenarios()`
- No code queries for `resumeReady=true` scenarios
- No periodic discovery mechanism

**Specification Violated:**
- `RUNNER_RESUME_COORDINATION.md`: "Runner periodically performs: Query for scenarios with state = RESUME_READY"
- `RUNNER_EXECUTION_MODEL.md`: "Runner queries RESUME_READY scenarios"

**Impact:** CRITICAL — Resume functionality is non-functional, paused scenarios never resume

---

### 3.6 RESUME ENGINE AUTHORITY — VIOLATED — CRITICAL

**Violation:** Pollers mark resumeReady, not just ResumeEngine

**Location:** 
- `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/MongoEventPoller.java` (line 52)
- `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/PostgresEventPoller.java` (line 53)
- `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/H2EventPoller.java` (line 53)

**Evidence:**
```java
storage.markResumeReady(pausedCtx.getCanonicalKey());
```

**Specification Violated:**
- `RESUME_ENGINE_INTERNAL_FLOW.md`: "ResumeEngine is the ONLY component allowed to transition scenarios out of PAUSED state"
- `RESUME_ENGINE_INTERNAL_FLOW.md`: "ResumeEngine performs ONLY the following transition: PAUSED → RESUME_READY"

**Impact:** CRITICAL — Violates single responsibility, creates multiple paths to resume readiness

---

### 3.7 SCENARIO KEY STABILITY — TIMESTAMP USAGE — CRITICAL

**Violation:** OrderId generated using `System.currentTimeMillis()`

**Location:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java` (line 76)

**Evidence:**
```java
String orderId = String.valueOf(System.currentTimeMillis());
```

**Specification Violated:**
- `SCENARIO_KEY_MODEL.md`: "The following are FORBIDDEN: Using timestamps in keys"
- `SCENARIO_KEY_MODEL.md`: "Scenario key MUST NEVER change" across pause/resume

**Impact:** CRITICAL — Keys are not stable, resume will fail to correlate events

---

### 3.8 EVENT MATCHING — POLLING EXISTS — CRITICAL

**Violation:** Scheduled pollers query DB periodically

**Location:**
- `MongoEventPoller.java` (line 32: `@Scheduled(fixedDelayString = "${autwit.poller.delay-ms:1000}")`)
- `PostgresEventPoller.java` (line 32)
- `H2EventPoller.java` (line 32)

**Evidence:**
```java
@Scheduled(fixedDelayString = "${autwit.poller.delay-ms:1000}")
public void poll() {
    List<EventContext> paused = storage.findPaused();
    // ... processes paused contexts
}
```

**Specification Violated:**
- `EVENT_MATCHING_RULES.md`: "The following are STRICTLY FORBIDDEN: Polling DB from step definitions"
- `RESUME_ENGINE_INTERNAL_FLOW.md`: "Reconciliation flow MUST NOT: Rely on timers in tests"

**Impact:** CRITICAL — Introduces timing dependency, violates event-driven model

**Note:** While reconciliation is allowed as a safety net, scheduled polling at fixed intervals violates the "no timers" principle.

---

## 4. VIOLATIONS (NON-CRITICAL)

### 4.1 SCENARIO KEY GENERATION — HASH-BASED

**Location:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java` (line 37)

**Evidence:**
```java
int hash = Math.abs((name + id).hashCode() % 9999);
String exampleKey = "TC" + hash;
String scenarioKey = name + "_" + exampleKey;
```

**Issue:** Hash-based key generation may not be collision-resistant for parallel execution

**Specification:** `SCENARIO_KEY_MODEL.md` requires "Collision-resistant" keys

**Impact:** NON-CRITICAL — May cause key collisions in high-parallelism scenarios

---

### 4.2 SCENARIO STATE TRACKER — TIME-BASED METADATA

**Location:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java` (line 34)

**Evidence:**
```java
state.setLastUpdated(System.currentTimeMillis());
```

**Issue:** Uses timestamp for metadata (not state decision)

**Specification:** `SCENARIO_STATE_MODEL.md`: "If a scenario state change depends on: Wall-clock delays, Then the state model is being violated"

**Impact:** NON-CRITICAL — Used for audit only, not state transitions

---

### 4.3 EVENT STEP NOTIFIER — TTL TIMEOUT

**Location:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java` (line 66)

**Evidence:**
```java
ScheduledFuture<?> cleaner = scheduler.schedule(() -> {
    if (!future.isDone()) {
        future.completeExceptionally(new TimeoutException("Await TTL expired for key=" + key));
    }
}, Duration.ofHours(1).toMillis(), TimeUnit.MILLISECONDS);
```

**Issue:** TTL timeout for cleanup (1 hour)

**Specification:** `EVENT_MATCHING_RULES.md`: "Timeouts are forbidden"

**Impact:** NON-CRITICAL — Safety mechanism, but violates "no timeouts" principle

---

### 4.4 CLIENT SDK DEPENDENCY ON PORTS

**Location:** `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/AutwitImpl.java`

**Evidence:**
```java
@Autowired
private EventMatcherPort matcher;
@Autowired
private ScenarioStatePort scenarioState;
```

**Issue:** Client SDK depends on ports (may be acceptable if SDK is part of core boundary)

**Specification:** `ARCHITECTURAL_GUARDRAILS.md`: "DO NOT import ports or adapters"

**Impact:** NON-CRITICAL — Depends on whether SDK is considered "client" or "internal"

---

## 5. RISKY / AMBIGUOUS AREAS

### 5.1 STATE TRANSITION AUTHORITY

**Issue:** No centralized state transition manager

**Location:** State changes are scattered across:
- `ResumeEngine` (marks resumeReady)
- Pollers (mark resumeReady)
- Runner (implicitly via test execution)
- Client code (throws SkipException)

**Risk:** Cannot enforce legal transitions, cannot prevent illegal states

**Specification:** `SCENARIO_STATE_MODEL.md`: "Only AUTWIT Core may: Change scenario state"

---

### 5.2 RESUME EXECUTION PATH

**Issue:** No evidence of resume execution path being identical to fresh execution

**Location:** Runner does not distinguish resume vs fresh execution

**Risk:** Resume may skip steps or use different logic

**Specification:** `RUNNER_EXECUTION_MODEL.md`: "Both modes use the SAME execution pipeline"

**Status:** AMBIGUOUS — Cannot verify without resume discovery mechanism

---

### 5.3 SCENARIO CONTEXT RESTORATION

**Issue:** Scenario key is generated in `Hooks.beforeScenario()` but may not be stable across resume

**Location:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`

**Risk:** If scenario key changes on resume, context restoration fails

**Specification:** `SCENARIO_KEY_MODEL.md`: "Resume MUST reuse the same key"

**Status:** RISKY — Key generation logic may not preserve key across resume

---

### 5.4 EVENT MATCHING — IN-MEMORY WAITERS

**Issue:** `EventStepNotifier` maintains in-memory `ConcurrentMap` of waiters

**Location:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java`

**Risk:** In-memory state is lossy, violates "DB is source of truth"

**Specification:** `EVENT_MATCHING_RULES.md`: "In-memory structures: Are optional, Are lossy, Are NOT authoritative"

**Status:** ACCEPTABLE — But must ensure DB-first matching always occurs

---

## 6. FILES REQUIRING ATTENTION

### 6.1 CRITICAL PRIORITY

1. **`client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`**
   - **Reason:** Direct port imports, timeout usage, timestamp-based orderId
   - **Violations:** Architecture boundaries, event matching rules, scenario key model

2. **`autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/EventExpectationImpl.java`**
   - **Reason:** Timeout usage in SDK
   - **Violations:** Event matching rules, pause semantics

3. **`autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberTestRunner.java`**
   - **Reason:** Missing RESUME_READY discovery
   - **Violations:** Runner execution model, runner-resume coordination

4. **`autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/*EventPoller.java`**
   - **Reason:** Pollers mark resumeReady (should only be ResumeEngine)
   - **Violations:** Resume engine internal flow

5. **State Model Implementation (Missing)**
   - **Reason:** No explicit state enum or state machine
   - **Violations:** Scenario state model

### 6.2 HIGH PRIORITY

6. **`autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java`**
   - **Reason:** TTL timeout, in-memory waiters
   - **Violations:** Event matching rules (timeout)

7. **`autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`**
   - **Reason:** Hash-based key generation may not be collision-resistant
   - **Violations:** Scenario key model (collision resistance)

### 6.3 MEDIUM PRIORITY

8. **`autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`**
   - **Reason:** Time-based metadata (non-critical)
   - **Violations:** Scenario state model (minor)

---

## 7. FINAL VERDICT

### **MISALIGNED**

The codebase is **MISALIGNED** with the architectural specifications due to:

1. **Critical Architecture Boundary Violations** — Client code directly imports engine ports
2. **Fundamental Principle Violations** — Timeout-based logic throughout (violates event-driven model)
3. **Missing Core Functionality** — No explicit state model, no runner resume discovery
4. **Authority Violations** — Multiple components mark resumeReady (should only be ResumeEngine)
5. **Key Stability Violations** — Timestamp-based keys break resume correlation

**Recommendation:** The codebase requires **significant architectural refactoring** to align with specifications. Critical violations must be addressed before the framework can function as designed.

---

## APPENDIX: SPECIFICATION REFERENCES

All violations are referenced against:
- `SCENARIO_STATE_MODEL.md`
- `EVENT_MATCHING_RULES.md`
- `RESUME_ENGINE_INTERNAL_FLOW.md`
- `RUNNER_RESUME_COORDINATION.md`
- `RUNNER_EXECUTION_MODEL.md`
- `SCENARIO_CONTEXT_LIFECYCLE.md`
- `FAILURE_VS_PAUSE_SEMANTICS.md`
- `SCENARIO_KEY_MODEL.md`

---

**END OF AUDIT REPORT**

