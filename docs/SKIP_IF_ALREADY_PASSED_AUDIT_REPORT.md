# SKIP_IF_ALREADY_PASSED LOGIC AUDIT REPORT

**Date:** 2026-01-05  
**Audit Focus:** Replacement of `skipIfAlreadyPassed()` client-side logic  
**Mode:** Architectural Compliance Audit Only

---

## 1️⃣ WHERE THIS LOGIC MOVED TO

### Replacement Summary

| Component | Status | Location | Notes |
|-----------|--------|----------|-------|
| **ScenarioStateTracker** | ✅ EXISTS | `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java` | Provides `isStepAlreadySuccessful()` and `getStepData()` |
| **ReusableStepDefsImpl** | ✅ EXISTS | `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/stepDef/ReusableStepDefsImpl.java` | Contains `skipIfAlreadyPassed()` method (lines 33-51) |
| **Client Step Definitions** | ❌ COMMENTED OUT | `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java` | All `skipIfAlreadyPassed()` calls are commented out (lines 37-63) |
| **Facade-Based StepDefs** | ✅ ACTIVE | `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefsFacedBased.java` | Uses `skipIfAlreadyPassed()` (lines 36-62) |
| **ResumeEngine** | ❌ NO | N/A | Does NOT track step success/failure |
| **Runner** | ❌ NO | N/A | Does NOT skip steps on resume |

### Explicit Answers

**Q: Is there any equivalent of "isStepAlreadySuccessful"?**

**A: YES**

- **Location:** `ScenarioStateTracker.isStepAlreadySuccessful(String scenario, String step)`
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`
- **Lines:** 40-44
- **Implementation:** Queries `ScenarioContextPort` for persisted step status, checks if status equals "success"

**Q: If yes, where?**

**A:** The infrastructure exists in:
1. **ScenarioStatePort interface** (`autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/ScenarioStatePort.java`)
   - Defines `isStepAlreadySuccessful()` and `getStepData()` methods
2. **ScenarioStateTracker** (engine component)
   - Implements the port interface
   - Persists step status via `ScenarioContextPort`
3. **ReusableStepDefsImpl** (internal testkit)
   - Uses `skipIfAlreadyPassed()` internally (lines 33-51, 85, 118)
   - Restores orderId from stepData when skipping

**Q: If no, why not?**

**A: N/A** (Infrastructure exists but is inconsistently used)

---

## 2️⃣ RESUME EXECUTION FLOW (ACTUAL, NOT THEORETICAL)

### Actual Execution Path

```
Feature File
  ↓
Step Definition (Cucumber)
  ↓
Autwit.expectEvent().assertSatisfied()
  ↓
EventExpectationImpl.assertSatisfied()
  ↓ (if event not found)
SkipException thrown
  ↓
TestNG marks scenario as SKIPPED
  ↓
RetrySkippedTestsExecutor.onExecutionFinish()
  ↓
RetrySkippedTestsExecutor.runRetrySuite()
  ↓
TestNG re-executes ENTIRE scenario from beginning
  ↓
Hooks.beforeScenario() regenerates scenarioKey
  ↓
Step Definition executes again
  ↓
Step checks isStepAlreadySuccessful() (if implemented)
  OR
Step finds event in DB immediately (if event arrived)
```

### Key Findings

**Q: Does the runner re-run the entire scenario or only remaining steps?**

**A: ENTIRE SCENARIO**

- **Evidence:** `RetrySkippedTestsExecutor.runRetrySuite()` creates a new TestNG suite with the same test method
- **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/listeners/RetrySkippedTestsExecutor.java`
- **Lines:** 88-125
- **Behavior:** TestNG re-executes the entire scenario method from the first step
- **No step-level skipping:** Runner does NOT skip steps; it re-runs everything

**Q: Is step-level idempotency relied upon instead of skipping?**

**A: PARTIALLY**

- **Internal testkit:** `ReusableStepDefsImpl` uses `skipIfAlreadyPassed()` to skip steps
- **Client step definitions:** Most client step definitions have `skipIfAlreadyPassed()` **commented out**
- **Event-driven steps:** Rely on "facts now exist → step succeeds immediately" pattern
- **Mixed approach:** Some steps check DB, others assume idempotency

**Q: Who decides that a step should NOT run again?**

**A: THE STEP ITSELF (if it implements the check)**

- **No runner-level decision:** Runner does not skip steps
- **No engine-level decision:** ResumeEngine does not track step status
- **Step-level decision:** Each step must check `isStepAlreadySuccessful()` if it wants to skip
- **Default behavior:** If step doesn't check, it re-executes

---

## 3️⃣ STEP-LEVEL DECISION MECHANISM

### How Resumed Run Avoids Re-waiting/Re-blocking

**Mechanism 1: Event Already Exists in DB**

- **Location:** `EventStepNotifier.match()` → `storage.findByCanonicalKey()`
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java`
- **Lines:** 48-52
- **Behavior:** If event exists in DB, `CompletableFuture` completes immediately
- **No waiting:** Step does not block if event is already persisted

**Mechanism 2: Step-Level Skip Check (Optional)**

- **Location:** `ReusableStepDefsImpl.skipIfAlreadyPassed()`
- **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/stepDef/ReusableStepDefsImpl.java`
- **Lines:** 33-51, 85, 118
- **Behavior:** Checks `scenarioState.isStepAlreadySuccessful()` before executing step logic
- **Restoration:** Restores orderId from `getStepData()` if step already passed

**Mechanism 3: Idempotent Step Logic**

- **Assumption:** Steps are expected to be idempotent
- **Pattern:** "Facts now exist → step succeeds immediately"
- **Example:** If orderId already exists in ScenarioContext, step may not recreate it

### Step Assumptions

**Q: Are steps assumed to be idempotent?**

**A: YES (Implicitly)**

- Steps are expected to handle re-execution gracefully
- Event-driven steps succeed immediately if events exist
- No explicit idempotency enforcement

**Q: Are steps assumed to be expectation-driven?**

**A: YES (Explicitly)**

- Steps declare expectations (e.g., `autwit.expectEvent()`)
- If expectation is satisfied (event in DB), step succeeds
- If expectation is not satisfied, step pauses (SkipException)

**Q: Are steps assumed to be stateless?**

**A: NO**

- Steps rely on `ScenarioContext` (ThreadLocal) for state
- Steps store orderId, scenarioKey, etc. in context
- Context is NOT automatically restored from DB on resume

### DB Check Confirmation

**Q: Is there ANY DB check like "if step already passed → return"?**

**A: YES (But Inconsistently Used)**

- **Infrastructure exists:** `ScenarioStateTracker.isStepAlreadySuccessful()`
- **Used in:** `ReusableStepDefsImpl.skipIfAlreadyPassed()` (internal testkit)
- **NOT used in:** Most client step definitions (commented out)
- **Pattern:** Steps can check, but are not required to

**Q: Or is the system relying entirely on "facts now exist → step succeeds immediately"?**

**A: MIXED APPROACH**

- **Event-driven steps:** Rely on "facts now exist" pattern
- **Step tracking:** Infrastructure exists but is optional
- **Current state:** System works with either approach, but step-level skipping is not consistently applied

---

## 4️⃣ SCENARIO CONTEXT RESTORATION

### Context Restoration Responsibilities

| Responsibility | Current Location | Status |
|----------------|------------------|--------|
| **Restoring orderId** | `ReusableStepDefsImpl.skipIfAlreadyPassed()` (lines 43-48) | ✅ Implemented (if skipIfAlreadyPassed is called) |
| **Restoring MDC** | `ReusableStepDefsImpl.skipIfAlreadyPassed()` (line 47) | ✅ Implemented (if skipIfAlreadyPassed is called) |
| **Restoring paused scenario metadata** | `Hooks.beforeScenario()` (lines 31-56) | ⚠️ Regenerates (not restored) |

### Detailed Analysis

**Q: Is ScenarioContext restored from DB?**

**A: PARTIALLY**

- **Step data restoration:** `skipIfAlreadyPassed()` restores orderId from `getStepData()` if step already passed
- **Scenario key:** NOT restored from DB; regenerated in `Hooks.beforeScenario()` using hash
- **Full context:** NOT automatically restored; only step-specific data is restored when skipping

**Q: Or recomputed deterministically?**

**A: SCENARIO KEY IS RECOMPUTED**

- **Location:** `Hooks.beforeScenario()`
- **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`
- **Lines:** 37-40
- **Method:** `int hash = Math.abs((name + id).hashCode() % 9999); String scenarioKey = name + "_" + exampleKey;`
- **Issue:** Hash-based key may not be stable across resume if scenario name/id changes

**Q: Or fully engine-owned?**

**A: NO**

- Context is ThreadLocal-based (`ScenarioContext`)
- Engine does NOT own context lifecycle
- Runner/Hooks manage context creation
- Step data is engine-tracked but context is runner-scoped

### Restoration Flow

**When skipIfAlreadyPassed() is called:**
1. Checks `isStepAlreadySuccessful()` via `ScenarioStatePort`
2. If true, retrieves `getStepData()` from DB
3. Restores orderId: `ScenarioContext.set("orderId", restored)`
4. Restores MDC: `ScenarioMDC.setOrderId(restored)`
5. Returns true (step skipped)

**When skipIfAlreadyPassed() is NOT called:**
1. Step executes normally
2. Context is NOT restored from DB
3. Step must be idempotent or re-create state

---

## 5️⃣ FINAL VERDICT (MANDATORY)

### Verdict: **B) Split Across Engine + Internal Testkit (Inconsistently Applied)**

### Justification

**The `skipIfAlreadyPassed()` logic was:**

1. **Partially moved to internal testkit:**
   - `ReusableStepDefsImpl.skipIfAlreadyPassed()` implements the logic
   - Uses `ScenarioStatePort.isStepAlreadySuccessful()` and `getStepData()`
   - Restores orderId and MDC when skipping

2. **Infrastructure exists in engine:**
   - `ScenarioStateTracker` provides step tracking
   - `ScenarioStatePort` interface defines the contract
   - Step status and data are persisted to DB

3. **NOT consistently used in client code:**
   - Main client step definitions have `skipIfAlreadyPassed()` **commented out**
   - Only facade-based step definitions use it
   - Most steps rely on idempotency instead

4. **Runner does NOT skip steps:**
   - `RetrySkippedTestsExecutor` re-executes entire scenarios
   - No step-level skipping at runner level
   - Steps must handle their own idempotency or check DB themselves

### Concrete Class/Method References

**Infrastructure:**
- `ScenarioStatePort.isStepAlreadySuccessful()` - Interface definition
- `ScenarioStateTracker.isStepAlreadySuccessful()` - Implementation (lines 40-44)
- `ScenarioStateTracker.getStepData()` - Data retrieval (lines 46-50)
- `ScenarioStateTracker.markStep()` - Step tracking (lines 20-38)

**Usage:**
- `ReusableStepDefsImpl.skipIfAlreadyPassed()` - Internal implementation (lines 33-51)
- `ReusableStepDefsImpl.verifyEvent()` - Uses skipIfAlreadyPassed (line 85)
- `ReusableStepDefsImpl.validateEvent()` - Uses skipIfAlreadyPassed (line 118)

**Client Code:**
- `EventDrivenOrderLifecycleStepDefs.skipIfAlreadyPassed()` - **COMMENTED OUT** (lines 37-63)
- `EventDrivenOrderLifecycleStepDefsFacedBased.skipIfAlreadyPassed()` - **ACTIVE** (lines 36-62)

**Runner:**
- `RetrySkippedTestsExecutor.runRetrySuite()` - Re-executes entire scenario (lines 88-125)
- `CucumberTestRunner.scenarios()` - No step skipping logic (lines 32-35)

---

## EXPLICIT ANSWER TO CORE QUESTION

**"How does AUTWIT know whether to execute a step again on resume?"**

### Answer:

**AUTWIT does NOT automatically know. The decision is made at the STEP LEVEL, not at the framework level.**

**Current Mechanisms:**

1. **Step-Level Check (Optional):**
   - Step calls `scenarioState.isStepAlreadySuccessful(scenarioKey, stepName)`
   - If true, step returns early (skips execution)
   - Step restores context data from `getStepData()`

2. **Event-Driven Immediate Success:**
   - Step calls `autwit.expectEvent(orderId, eventType).assertSatisfied()`
   - If event exists in DB, `EventStepNotifier.match()` returns immediately
   - Step succeeds without waiting

3. **Idempotent Step Logic:**
   - Step assumes it can be safely re-executed
   - Step handles re-execution gracefully
   - No explicit skip check

**The runner re-executes the ENTIRE scenario from the first step. Steps that want to skip must check `isStepAlreadySuccessful()` themselves. Most client step definitions do NOT implement this check (it's commented out).**

---

## SUMMARY TABLE

| Aspect | Status | Location |
|--------|--------|----------|
| **Step tracking infrastructure** | ✅ EXISTS | `ScenarioStateTracker` |
| **Skip logic implementation** | ✅ EXISTS | `ReusableStepDefsImpl.skipIfAlreadyPassed()` |
| **Client step usage** | ❌ COMMENTED OUT | Most client step definitions |
| **Runner step skipping** | ❌ NO | Runner re-executes entire scenario |
| **Context restoration** | ⚠️ PARTIAL | Only when skipIfAlreadyPassed is called |
| **Scenario key stability** | ⚠️ RISKY | Hash-based, may not be stable |

---

**END OF AUDIT REPORT**

