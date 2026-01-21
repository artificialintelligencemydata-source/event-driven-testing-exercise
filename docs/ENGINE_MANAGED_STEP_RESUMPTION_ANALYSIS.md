# ENGINE-MANAGED STEP RESUMPTION — ARCHITECTURAL ANALYSIS

**Date:** 2026-01-05  
**Purpose:** Analyze current state and design requirements for engine-managed step resumption  
**Mode:** Analysis Only (No Code Changes)

---

## 1️⃣ CURRENT STATE ANALYSIS

### Where Scenario Execution Restarts Today

**Location:** `RetrySkippedTestsExecutor.runRetrySuite()`

**File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/listeners/RetrySkippedTestsExecutor.java`

**Lines:** 88-125

**Behavior:**
- Creates new TestNG `XmlSuite` with same test method
- TestNG re-executes ENTIRE scenario from first step
- No step-level skipping or fast-forwarding
- Scenario method runs from beginning

**Evidence:**
```java
XmlSuite suite = new XmlSuite();
// ... creates suite with same test method
TestNG ng = new TestNG();
ng.setXmlSuites(Collections.singletonList(suite));
ng.run(); // Re-executes entire scenario
```

---

### Who Decides Which Steps Run Again

**Answer: NO ONE (Framework-Level)**

**Current State:**
- **Runner:** Does NOT decide step execution order
- **Engine:** Does NOT control step execution
- **TestNG/Cucumber:** Executes all steps in feature file order
- **Step Definitions:** OPTIONALLY check `isStepAlreadySuccessful()` themselves

**Concrete Evidence:**

1. **Runner has no step skipping logic:**
   - `CucumberTestRunner.scenarios()` - Lines 32-35
   - Only provides data provider, no execution control

2. **Engine does not intercept step execution:**
   - `ResumeEngine` - Only marks `resumeReady`, does not control execution
   - `ScenarioStateTracker` - Tracks step status but does not prevent execution

3. **Optional step-level checks exist:**
   - `ReusableStepDefsImpl.skipIfAlreadyPassed()` - Lines 33-51
   - Used internally but NOT enforced
   - Client step definitions have this **commented out**

---

### How Step Success Is Currently Stored

**Storage Mechanism:**

1. **Interface:** `ScenarioStatePort.markStep()`
   - **File:** `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/ScenarioStatePort.java`
   - **Line:** 5

2. **Implementation:** `ScenarioStateTracker.markStep()`
   - **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`
   - **Lines:** 20-38

3. **Persistence:** Via `ScenarioContextPort.save()`
   - Stores in `ScenarioContext` domain object
   - Persisted to DB via adapters (Mongo/Postgres/H2)
   - **File:** `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/domain/ScenarioContext.java`
   - **Structure:** `Map<String, String> stepStatus` and `Map<String, Map<String, String>> stepData`

4. **Retrieval:** `ScenarioStateTracker.isStepAlreadySuccessful()`
   - **Lines:** 40-44
   - Queries `ScenarioContextPort.findByScenarioName()`
   - Checks if step status equals "success"

**Storage Format:**
- Key: `scenarioName` (not scenarioKey)
- Value: `ScenarioContext` object containing:
  - `stepStatus`: Map of step name → "success"/"failed"
  - `stepData`: Map of step name → Map of key-value pairs (e.g., orderId)

---

### How (and If) Context Is Restored on Resume

**Current Restoration Behavior:**

**PARTIAL RESTORATION (Only When skipIfAlreadyPassed is Called):**

1. **Scenario Key:** NOT restored
   - **Location:** `Hooks.beforeScenario()`
   - **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`
   - **Lines:** 37-40
   - **Behavior:** Regenerates scenarioKey using hash: `Math.abs((name + id).hashCode() % 9999)`
   - **Issue:** Key may not be stable across resume

2. **Step Data (orderId, etc.):** Restored ONLY if step calls `skipIfAlreadyPassed()`
   - **Location:** `ReusableStepDefsImpl.skipIfAlreadyPassed()`
   - **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/stepDef/ReusableStepDefsImpl.java`
   - **Lines:** 43-48
   - **Behavior:** Retrieves `getStepData()` and restores orderId to `ScenarioContext` and `ScenarioMDC`

3. **Full Context:** NOT automatically restored
   - No hook in `Hooks.beforeScenario()` to restore from DB
   - Context is ThreadLocal-based and cleared on scenario end
   - No persistence/restoration of full `ScenarioContext` (ThreadLocal map)

**Evidence of Missing Restoration:**

- `Hooks.beforeScenario()` - Lines 31-56: Creates NEW context, does NOT restore
- `Hooks.afterScenario()` - Lines 77-94: Clears context, does NOT persist
- No `@Before` hook queries DB for existing scenario context

---

### Where Step Skipping CAN Happen vs Where It DOES NOT

**Where Step Skipping CAN Happen:**

1. **Inside Step Definition (Optional):**
   - **Location:** Step definition method body
   - **Example:** `ReusableStepDefsImpl.verifyEvent()` - Line 85
   - **Mechanism:** Calls `skipIfAlreadyPassed()`, returns early if true
   - **Status:** ✅ Works but is OPTIONAL

2. **Internal Testkit Steps:**
   - **Location:** `ReusableStepDefsImpl`
   - **Status:** ✅ Implemented and used

**Where Step Skipping DOES NOT Happen:**

1. **Runner Level:**
   - `CucumberTestRunner` - No step skipping logic
   - `RetrySkippedTestsExecutor` - Re-executes entire scenario

2. **Engine Level:**
   - `ResumeEngine` - Only marks resumeReady, does not control execution
   - `ScenarioStateTracker` - Tracks but does not prevent execution

3. **Cucumber Framework:**
   - No `@BeforeStep` hooks that skip steps
   - No step execution interception

4. **Client Step Definitions:**
   - Most have `skipIfAlreadyPassed()` **commented out**
   - Steps execute unconditionally

---

## 2️⃣ GAP ANALYSIS

### Why AUTWIT Cannot Currently Resume from a Specific Step

**Root Causes:**

1. **No Step Execution Interception:**
   - Cucumber executes steps sequentially without framework-level interception
   - No `@BeforeStep` hook that can prevent step execution
   - No step execution wrapper that checks step status

2. **No Step Index Tracking:**
   - Framework does not track "which step number are we on"
   - No concept of "resume from step N"
   - Steps are identified by name/text, not by position

3. **No Context Restoration Hook:**
   - `Hooks.beforeScenario()` creates NEW context
   - Does NOT query DB for existing scenario context
   - Does NOT restore scenarioKey, orderId, or other persisted data

4. **Scenario Key Instability:**
   - Key is regenerated on each execution (hash-based)
   - Cannot reliably correlate resume execution with original execution
   - Key may change if scenario name/id changes

5. **TestNG Retry Model:**
   - `RetrySkippedTestsExecutor` treats scenario as atomic unit
   - Re-executes entire test method
   - No step-level granularity

---

### Missing Responsibilities at Runner Level

**Current Runner Responsibilities:**
- ✅ Bootstrap TestNG/Cucumber
- ✅ Provide data provider for scenarios
- ❌ **MISSING:** Query DB for existing scenario context on resume
- ❌ **MISSING:** Restore scenarioKey from persisted state
- ❌ **MISSING:** Restore full ScenarioContext from DB
- ❌ **MISSING:** Detect resume execution vs fresh execution
- ❌ **MISSING:** Coordinate with engine to determine resume point
- ❌ **MISSING:** Skip steps that already succeeded

**Files Involved:**
- `autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberTestRunner.java`
- `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`

---

### Missing Responsibilities at Engine Level

**Current Engine Responsibilities:**
- ✅ Track step success/failure via `ScenarioStateTracker`
- ✅ Mark resumeReady via `ResumeEngine`
- ❌ **MISSING:** Determine which step to resume from
- ❌ **MISSING:** Provide "first incomplete step" query
- ❌ **MISSING:** Coordinate step execution order with runner
- ❌ **MISSING:** Enforce step skipping (currently optional)

**Files Involved:**
- `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`
- `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/resume/ResumeEngine.java`

---

### Missing Responsibilities at Scenario Context Level

**Current Context Responsibilities:**
- ✅ Store step status and step data
- ✅ Persist to DB via adapters
- ❌ **MISSING:** Persist full ThreadLocal ScenarioContext (orderId, etc.)
- ❌ **MISSING:** Restore full context on resume
- ❌ **MISSING:** Stable scenarioKey generation/persistence
- ❌ **MISSING:** Context versioning for resume compatibility

**Files Involved:**
- `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/context/ScenarioContext.java`
- `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/domain/ScenarioContext.java`

---

### Cucumber/TestNG Limitations

**Cucumber Limitations:**

1. **No Built-in Step Skipping:**
   - Cucumber executes all steps in feature file order
   - No native "skip step N" mechanism
   - Steps are matched by text, not by index

2. **Step Execution Model:**
   - Steps are executed via reflection/invocation
   - No pre-execution hook that can prevent step execution
   - `@BeforeStep` can run code but cannot skip the step

3. **Scenario as Atomic Unit:**
   - Cucumber treats scenario as single test method
   - TestNG sees one test method per scenario
   - No framework-level step granularity

**TestNG Limitations:**

1. **Method-Level Retry:**
   - `RetrySkippedTestsExecutor` retries at method level
   - Cannot retry individual steps within a method
   - No step-level execution control

2. **No Step Index:**
   - TestNG does not track step execution order
   - Cannot resume from "step 3 of 5"

**Workarounds Available:**

1. **Step-Level Checks:**
   - Steps can check `isStepAlreadySuccessful()` and return early
   - Requires manual implementation in each step

2. **Cucumber Hooks:**
   - `@BeforeStep` can execute code before each step
   - Could potentially throw SkipException to skip step
   - But this would mark step as skipped, not as "already passed"

---

## 3️⃣ DESIGN INSERTION POINTS

### Where Step Execution Interception Could Occur

**Option 1: Cucumber @BeforeStep Hook**

**Location:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`

**Extension Point:**
- Add `@BeforeStep` method
- Check `ScenarioStatePort.isStepAlreadySuccessful()` before step executes
- If true, restore step data and throw SkipException (or use Cucumber's skip mechanism)

**Feasibility:** ⚠️ MEDIUM
- Cucumber supports `@BeforeStep`
- Can execute code before step
- Cannot easily "skip" without marking as skipped
- Would need to use reflection or framework-specific mechanism

**Option 2: Step Definition Wrapper/Proxy**

**Location:** New component in `autwit-core/autwit-internal-testkit`

**Extension Point:**
- Create step definition wrapper/interceptor
- Wrap all step definitions with execution check
- Check step status before invoking actual step method

**Feasibility:** ❌ LOW
- Requires AOP or bytecode manipulation
- Complex integration with Cucumber's step matching
- High risk of breaking existing step definitions

**Option 3: Internal Testkit Step Base Class**

**Location:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/stepDef/`

**Extension Point:**
- Extend `ReusableStepDefsImpl` pattern
- All steps inherit from base class that checks step status
- Client steps use base class methods

**Feasibility:** ✅ HIGH
- Similar to existing `ReusableStepDefsImpl` pattern
- Can be adopted incrementally
- Requires client steps to use base class

---

### Where Scenario State Can Be Consulted Before Step Execution

**Existing Infrastructure:**

1. **ScenarioStatePort Interface:**
   - **File:** `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/ScenarioStatePort.java`
   - **Methods:** `isStepAlreadySuccessful()`, `getStepData()`

2. **ScenarioStateTracker Implementation:**
   - **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`
   - **Status:** ✅ Fully implemented

**Insertion Points:**

1. **@BeforeStep Hook:**
   - **File:** `Hooks.java`
   - **Method:** Add `@BeforeStep` method
   - **Logic:** Query `ScenarioStatePort.isStepAlreadySuccessful(scenarioKey, stepName)`
   - **Action:** If true, restore step data and skip step execution

2. **Step Definition Base Class:**
   - **File:** Extend `ReusableStepDefsImpl`
   - **Logic:** Check step status in base method before calling step implementation
   - **Action:** Return early if step already passed

---

### Where ScenarioContext Restoration SHOULD Occur

**Current Location:** `Hooks.beforeScenario()`

**File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`

**Lines:** 31-56

**Required Changes:**

1. **Detect Resume Execution:**
   - Query `EventContextPort` for scenarios with `resumeReady=true` and matching scenario name
   - OR query `ScenarioContextPort` for existing scenario context

2. **Restore Scenario Key:**
   - Load persisted scenarioKey from DB (if exists)
   - Do NOT regenerate if resume execution

3. **Restore Full Context:**
   - Load `ScenarioContext` from `ScenarioContextPort`
   - Restore all step data (orderId, etc.) to ThreadLocal `ScenarioContext`
   - Restore MDC values

4. **Restore Step Status:**
   - Load step status map
   - Make available for step execution checks

**New Method Required:**
- `restoreScenarioContext(String scenarioName)` in `Hooks` or new component
- Called from `@Before(order = -1)` hook (before scenario key generation)

---

### Which Existing Components Could Be Extended

**1. Hooks.java**

**Current:** Creates new context on scenario start

**Extension:**
- Add resume detection logic
- Add context restoration logic
- Preserve existing behavior for fresh executions

**File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`

**2. ScenarioStateTracker**

**Current:** Tracks step status, provides query methods

**Extension:**
- Add `getFirstIncompleteStep(String scenarioName)` method
- Add `getCompletedSteps(String scenarioName)` method
- Add step execution order tracking

**File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`

**3. ScenarioContextPort**

**Current:** Stores scenario context by scenarioName

**Extension:**
- Add `findByScenarioKey(String scenarioKey)` method (if scenarioKey is different from scenarioName)
- Add context versioning for compatibility

**File:** `autwit-core/autwit-domain/src/main/java/com/acuver/autwit/core/ports/ScenarioContextPort.java`

**4. ReusableStepDefsImpl**

**Current:** Implements `skipIfAlreadyPassed()` for internal steps

**Extension:**
- Make skipIfAlreadyPassed() public or provide base class
- Add automatic step name extraction from Cucumber context
- Add automatic context restoration

**File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/stepDef/ReusableStepDefsImpl.java`

---

## 4️⃣ FEASIBILITY OPTIONS (STRICT)

### A. Runner-Level Step Skipping

**Description:** Runner intercepts Cucumber step execution and skips steps that already passed.

**Feasibility:** ⚠️ MEDIUM

**Required Changes:**
1. Implement `@BeforeStep` hook in `Hooks.java`
2. Extract step name from Cucumber `Step` object
3. Query `ScenarioStatePort.isStepAlreadySuccessful()`
4. If true, restore step data and skip step (mechanism TBD)
5. Runner must detect resume execution vs fresh execution

**Risks:**
- Cucumber may not support "skip without marking as skipped"
- Step name extraction may be unreliable
- Requires deep Cucumber integration knowledge
- May break Cucumber's internal state tracking

**Stack Compatibility:**
- ✅ Cucumber supports `@BeforeStep`
- ⚠️ Skipping mechanism unclear
- ❌ No native "skip already passed" feature

---

### B. Engine-Level Step Execution Controller

**Description:** Engine provides a step execution controller that wraps step definitions and enforces skipping.

**Feasibility:** ❌ LOW

**Required Changes:**
1. Create `StepExecutionController` component
2. Implement AOP or proxy pattern to wrap step definitions
3. Engine intercepts all step invocations
4. Engine checks step status before allowing execution
5. Engine restores context data

**Risks:**
- Requires AOP framework (Spring AOP or AspectJ)
- Complex integration with Cucumber's step matching
- High risk of breaking existing step definitions
- May interfere with Cucumber's internal mechanisms

**Stack Compatibility:**
- ⚠️ Requires AOP framework addition
- ❌ Cucumber step matching may break
- ❌ High complexity

---

### C. Internal Testkit Wrapper Around Steps

**Description:** Extend `ReusableStepDefsImpl` pattern to provide base step methods that all client steps use.

**Feasibility:** ✅ HIGH

**Required Changes:**
1. Create `ResumableStepDefs` base class/interface
2. Provide wrapper methods for common step patterns
3. Client steps extend base class or use wrapper methods
4. Base class checks step status before execution
5. Base class restores context automatically

**Risks:**
- Requires client step definitions to adopt new pattern
- Migration effort for existing steps
- Some steps may not fit wrapper pattern

**Stack Compatibility:**
- ✅ No framework changes required
- ✅ Works with existing Cucumber/TestNG
- ✅ Can be adopted incrementally

**Example Pattern:**
```java
public class ResumableStepDefs {
    protected void executeStep(String stepName, Runnable stepLogic) {
        if (skipIfAlreadyPassed(stepName)) return;
        stepLogic.run();
        markStepSuccess(stepName);
    }
}
```

---

### D. Explicit "Resume from Step Index" Model

**Description:** Track step execution order, persist step index, resume from specific step number.

**Feasibility:** ❌ LOW

**Required Changes:**
1. Track step execution order (step 1, 2, 3, etc.)
2. Persist "last executed step index" to DB
3. Runner queries for resume point
4. Cucumber/TestNG must support "start from step N" (NOT SUPPORTED)

**Risks:**
- Cucumber does NOT support starting from step N
- TestNG does NOT support partial method execution
- Would require custom Cucumber execution engine
- Extremely high complexity

**Stack Compatibility:**
- ❌ Cucumber limitation: Cannot start from step N
- ❌ TestNG limitation: Method-level execution only
- ❌ Would require framework modification

---

### E. Hybrid Model

**Description:** Combine context restoration (runner) + step-level checks (testkit) + engine coordination.

**Feasibility:** ✅ HIGH

**Components:**

1. **Runner (Hooks):**
   - Detect resume execution
   - Restore scenarioKey from DB
   - Restore full ScenarioContext from DB
   - Restore MDC values

2. **Engine (ScenarioStateTracker):**
   - Track step execution order
   - Provide `getFirstIncompleteStep()` query
   - Enforce step status checks

3. **Internal Testkit (@BeforeStep Hook):**
   - Intercept step execution
   - Check `isStepAlreadySuccessful()`
   - Restore step data if skipping
   - Skip step execution (via SkipException or framework mechanism)

4. **Client Steps:**
   - Use base class or wrapper methods (optional)
   - Steps remain declarative
   - Framework handles skipping automatically

**Required Changes:**

1. **Hooks.java:**
   - Add resume detection in `@Before(order = -1)`
   - Restore scenarioKey from persisted state
   - Restore full context from `ScenarioContextPort`

2. **New @BeforeStep Hook:**
   - Extract step name from Cucumber `Step` object
   - Query `ScenarioStatePort.isStepAlreadySuccessful()`
   - If true, restore step data and skip step

3. **ScenarioStateTracker:**
   - Add step execution order tracking
   - Add `getFirstIncompleteStep()` method

4. **ScenarioContextPort:**
   - Add `findByScenarioKey()` if needed
   - Ensure context includes scenarioKey (not just scenarioName)

**Risks:**
- Step name extraction from Cucumber may be unreliable
- Skip mechanism needs investigation (SkipException vs other)
- Requires coordination across multiple components

**Stack Compatibility:**
- ✅ Uses existing Cucumber hooks
- ✅ No framework modifications
- ✅ Can be implemented incrementally

---

## 5️⃣ MIGRATION STRATEGY

### Phase 1: Documentation + Guardrails

**Duration:** Immediate

**Actions:**

1. **Document Current Behavior:**
   - Update `RUNNER_EXECUTION_MODEL.md` with actual resume behavior
   - Document that runner re-executes entire scenario
   - Document that step skipping is optional

2. **Establish Guardrails:**
   - Require scenarioKey stability across resume
   - Require context restoration on resume
   - Prohibit step-level data regeneration on resume

3. **Audit Existing Steps:**
   - Identify steps that regenerate data (orderId, etc.)
   - Document which steps use `skipIfAlreadyPassed()`
   - Create migration checklist

**Deliverables:**
- Updated documentation
- Migration guide
- Audit report

---

### Phase 2: Partial Enforcement

**Duration:** Short-term (1-2 sprints)

**Actions:**

1. **Implement Context Restoration:**
   - **File:** `Hooks.java`
   - **Change:** Add `@Before(order = -1)` hook to detect resume
   - **Change:** Restore scenarioKey from persisted state
   - **Change:** Restore full ScenarioContext from DB
   - **Change:** Restore MDC values

2. **Implement Step Execution Interception:**
   - **File:** `Hooks.java` (new method)
   - **Change:** Add `@BeforeStep` hook
   - **Change:** Extract step name from Cucumber `Step`
   - **Change:** Query `ScenarioStatePort.isStepAlreadySuccessful()`
   - **Change:** Restore step data if skipping
   - **Change:** Skip step execution (mechanism TBD)

3. **Enhance ScenarioStateTracker:**
   - **File:** `ScenarioStateTracker.java`
   - **Change:** Add step execution order tracking
   - **Change:** Add `getFirstIncompleteStep()` method

4. **Fix Scenario Key Stability:**
   - **File:** `Hooks.java`
   - **Change:** Generate stable scenarioKey (not hash-based)
   - **Change:** Persist scenarioKey to DB
   - **Change:** Restore scenarioKey on resume

**Deliverables:**
- Context restoration working
- Step skipping working for steps that check
- Stable scenarioKey generation

**Testing:**
- Verify context restored on resume
- Verify steps skip when already passed
- Verify scenarioKey stable across resume

---

### Phase 3: Full Engine-Managed Resumption

**Duration:** Medium-term (2-4 sprints)

**Actions:**

1. **Enforce Step Skipping:**
   - Make step skipping mandatory (not optional)
   - Remove commented-out `skipIfAlreadyPassed()` calls
   - Ensure all steps use framework skipping

2. **Runner-Driven Resume Discovery:**
   - **File:** `CucumberTestRunner.java` or new component
   - **Change:** Query `EventContextPort` for `resumeReady=true` scenarios
   - **Change:** Schedule resume scenarios explicitly
   - **Change:** Transition `RESUME_READY → RESUMING → RUNNING`

3. **Eliminate TestNG Retry:**
   - **File:** `RetrySkippedTestsExecutor.java`
   - **Change:** Deprecate retry-based resume
   - **Change:** Remove retry executor from resume flow

4. **Full Context Persistence:**
   - **File:** `Hooks.java` and new component
   - **Change:** Persist full ThreadLocal ScenarioContext on pause
   - **Change:** Restore full context on resume
   - **Change:** Ensure all context data survives resume

**Deliverables:**
- Engine-managed step resumption fully operational
- Runner-driven resume discovery
- No dependency on TestNG retry
- Full context persistence/restoration

**Testing:**
- End-to-end resume flow
- Multi-pause/resume scenarios
- Parallel execution with resume
- Crash recovery scenarios

---

## OPTION COMPARISON TABLE

| Option | Feasibility | Complexity | Risk | Stack Compatibility | Migration Effort |
|--------|-------------|------------|------|---------------------|------------------|
| **A. Runner-Level Step Skipping** | MEDIUM | MEDIUM | MEDIUM | ⚠️ Partial | MEDIUM |
| **B. Engine-Level Controller** | LOW | HIGH | HIGH | ❌ Low | HIGH |
| **C. Internal Testkit Wrapper** | HIGH | LOW | LOW | ✅ High | MEDIUM |
| **D. Explicit Step Index** | LOW | VERY HIGH | VERY HIGH | ❌ None | VERY HIGH |
| **E. Hybrid Model** | HIGH | MEDIUM | MEDIUM | ✅ High | MEDIUM |

---

## RECOMMENDED MINIMAL PATH FORWARD

### Recommended Approach: **E. Hybrid Model (Phased)**

**Rationale:**
- Uses existing infrastructure (ScenarioStateTracker, Hooks)
- No framework modifications required
- Can be implemented incrementally
- Balances feasibility with architectural correctness

**Phase 1 (Immediate):**
1. Fix scenarioKey stability in `Hooks.beforeScenario()`
2. Add context restoration in `Hooks.beforeScenario()`
3. Document current gaps

**Phase 2 (Short-term):**
1. Add `@BeforeStep` hook for step skipping
2. Enhance `ScenarioStateTracker` with step order tracking
3. Test with existing steps

**Phase 3 (Medium-term):**
1. Implement runner-driven resume discovery
2. Eliminate TestNG retry dependency
3. Full enforcement of engine-managed resumption

**Key Files to Modify:**

1. `Hooks.java` - Context restoration, step skipping hook
2. `ScenarioStateTracker.java` - Step order tracking
3. `ScenarioContextPort.java` - Add findByScenarioKey if needed
4. `CucumberTestRunner.java` - Resume discovery (Phase 3)

---

## RISKS IF THIS IS NOT IMPLEMENTED

### Current Risks (Without Engine-Managed Resumption)

1. **Data Regeneration Risk:**
   - **Impact:** HIGH
   - **Description:** Steps regenerate orderId, correlation IDs on resume
   - **Consequence:** Broken event correlation, test failures, non-deterministic behavior

2. **Context Loss Risk:**
   - **Impact:** HIGH
   - **Description:** ScenarioContext not restored, data lost between pause/resume
   - **Consequence:** Steps fail due to missing data, non-deterministic execution

3. **Step Re-execution Risk:**
   - **Impact:** MEDIUM
   - **Description:** Steps that should not run again execute anyway
   - **Consequence:** Unnecessary API calls, potential side effects, test pollution

4. **Scenario Key Instability:**
   - **Impact:** HIGH
   - **Description:** ScenarioKey regenerated on resume, cannot correlate
   - **Consequence:** Cannot find persisted state, resume fails silently

5. **Non-Deterministic Behavior:**
   - **Impact:** CRITICAL
   - **Description:** Same scenario may behave differently on resume vs fresh execution
   - **Consequence:** Flaky tests, unreliable test results, loss of confidence

6. **Client Code Complexity:**
   - **Impact:** MEDIUM
   - **Description:** Client steps must handle resume logic themselves
   - **Consequence:** Violates AUTWIT principle of keeping client code simple

---

## SUMMARY

**Current State:**
- Runner re-executes entire scenario on resume
- Step skipping is optional and inconsistently applied
- Context restoration is partial (only when skipIfAlreadyPassed is called)
- ScenarioKey is regenerated (not stable)

**Required State:**
- Engine-managed step resumption
- Automatic step skipping for already-passed steps
- Full context restoration on resume
- Stable scenarioKey across pause/resume

**Recommended Path:**
- Hybrid model with phased implementation
- Start with context restoration and step skipping hooks
- Progress to full runner-driven resume discovery
- Eliminate TestNG retry dependency

**Critical Files:**
- `Hooks.java` - Primary modification point
- `ScenarioStateTracker.java` - Enhancement point
- `CucumberTestRunner.java` - Future modification (Phase 3)

---

**END OF ANALYSIS**

