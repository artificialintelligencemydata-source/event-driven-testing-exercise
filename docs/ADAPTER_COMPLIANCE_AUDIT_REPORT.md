# AUTWIT ADAPTER COMPLIANCE AUDIT REPORT

**Date:** 2026-01-05  
**Audit Scope:** Multi-Persistence Reconciliation Rule Compliance  
**Target Adapters:** MongoDB, PostgreSQL, H2  
**Mode:** Architectural Compliance Audit Only

---

## AUDIT RULE

**MULTI-PERSISTENCE RECONCILIATION RULE**

Each adapter MAY:
- Detect newly persisted events
- Reconcile persisted facts periodically
- Notify the AUTWIT event bus

Each adapter MUST NOT:
- Decide scenario success or failure
- Change scenario state
- Mark resumeReady
- Resume execution
- Introduce waits or timeouts into test execution

Resume eligibility is evaluated ONLY by ResumeEngine, regardless of persistence backend.

---

## SECTION 1: MONGO ADAPTER AUDIT

**Compliant:** NO

**Violations:**

1. **markResumeReady() method implementation**
   - **File:** `autwit-core/autwit-adapter-mongo/src/main/java/com/acuver/autwit/adapter/mongo/MongoEventContextAdapter.java`
   - **Lines:** 87-94
   - **Violation:** Adapter implements `markResumeReady()` method that mutates `resumeReady` flag in database
   - **Impact:** While the adapter only implements the interface contract, it provides the mechanism for marking resumeReady, which violates the rule that adapters MUST NOT mark resumeReady

2. **Poller calls adapter's markResumeReady()**
   - **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/MongoEventPoller.java`
   - **Lines:** 52
   - **Violation:** Database-specific poller (MongoEventPoller) calls `storage.markResumeReady()` directly
   - **Impact:** Poller makes decision to mark resumeReady and executes it via adapter, bypassing ResumeEngine authority

**Compliant Behaviors:**

- ✅ Does NOT call ScenarioStatePort
- ✅ Does NOT call ResumeEngine directly
- ✅ Does NOT decide PASS/FAIL/SKIP
- ✅ Does NOT introduce Thread.sleep, timeouts, or waits
- ✅ Only reads persisted facts (findLatest, findByCanonicalKey, findPaused, findByOrderId)
- ✅ Only persists data (save, markPaused)
- ✅ Poller uses @Scheduled for reconciliation (ALLOWED per rule)
- ✅ Poller notifies event bus via `awaiter.eventArrived()` (ALLOWED per rule)

---

## SECTION 2: POSTGRES ADAPTER AUDIT

**Compliant:** NO

**Violations:**

1. **markResumeReady() method implementation**
   - **File:** `autwit-core/autwit-adapter-postgres/src/main/java/com/acuver/autwit/adapter/postgres/PostgresEventContextAdapter.java`
   - **Lines:** 71-75
   - **Violation:** Adapter implements `markResumeReady()` method that mutates `resumeReady` flag in database
   - **Impact:** While the adapter only implements the interface contract, it provides the mechanism for marking resumeReady, which violates the rule that adapters MUST NOT mark resumeReady

2. **Poller calls adapter's markResumeReady()**
   - **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/PostgresEventPoller.java`
   - **Lines:** 53
   - **Violation:** Database-specific poller (PostgresEventPoller) calls `storage.markResumeReady()` directly
   - **Impact:** Poller makes decision to mark resumeReady and executes it via adapter, bypassing ResumeEngine authority

**Compliant Behaviors:**

- ✅ Does NOT call ScenarioStatePort
- ✅ Does NOT call ResumeEngine directly
- ✅ Does NOT decide PASS/FAIL/SKIP
- ✅ Does NOT introduce Thread.sleep, timeouts, or waits
- ✅ Only reads persisted facts (findLatest, findByCanonicalKey, findPaused, findByOrderId)
- ✅ Only persists data (save, markPaused)
- ✅ Poller uses @Scheduled for reconciliation (ALLOWED per rule)
- ✅ Poller notifies event bus via `awaiter.eventArrived()` (ALLOWED per rule)

---

## SECTION 3: H2 ADAPTER AUDIT

**Compliant:** NO

**Violations:**

1. **markResumeReady() method implementation**
   - **File:** `autwit-core/autwit-adapter-h2/src/main/java/com/acuver/autwit/adapter/h2/H2EventContextAdapter.java`
   - **Lines:** 88-95
   - **Violation:** Adapter implements `markResumeReady()` method that mutates `resumeReady` flag in database
   - **Impact:** While the adapter only implements the interface contract, it provides the mechanism for marking resumeReady, which violates the rule that adapters MUST NOT mark resumeReady

2. **Poller calls adapter's markResumeReady()**
   - **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/H2EventPoller.java`
   - **Lines:** 53
   - **Violation:** Database-specific poller (H2EventPoller) calls `storage.markResumeReady()` directly
   - **Impact:** Poller makes decision to mark resumeReady and executes it via adapter, bypassing ResumeEngine authority

**Compliant Behaviors:**

- ✅ Does NOT call ScenarioStatePort
- ✅ Does NOT call ResumeEngine directly
- ✅ Does NOT decide PASS/FAIL/SKIP
- ✅ Does NOT introduce Thread.sleep, timeouts, or waits
- ✅ Only reads persisted facts (findLatest, findByCanonicalKey, findPaused, findByOrderId)
- ✅ Only persists data (save, markPaused)
- ✅ Poller uses @Scheduled for reconciliation (ALLOWED per rule)
- ✅ Poller notifies event bus via `awaiter.eventArrived()` (ALLOWED per rule)

---

## SECTION 4: CROSS-ADAPTER CONSISTENCY CHECK

**Behavioral Divergence:** NO

**Explanation:**

All three adapters (Mongo, Postgres, H2) exhibit identical behavior:

1. **Identical Interface Implementation:**
   - All implement `EventContextPort` interface identically
   - All provide same methods: save, findLatest, findByCanonicalKey, markPaused, markResumeReady, isResumeReady, findByOrderId, findPaused
   - All use same mapping patterns (toEntity/toDomain)

2. **Identical Poller Pattern:**
   - All have corresponding pollers (MongoEventPoller, PostgresEventPoller, H2EventPoller)
   - All pollers use identical logic:
     - @Scheduled with same delay configuration
     - Query paused contexts via `storage.findPaused()`
     - Check for matching events via `storage.findLatest()`
     - Call `storage.markResumeReady()` when match found
     - Notify via `awaiter.eventArrived()`

3. **No Backend-Specific Semantics:**
   - No shortcuts or special handling for any database type
   - No conditional logic based on database type
   - All follow same reconciliation pattern

**Consistency Assessment:** All adapters violate the rule in the same way (providing markResumeReady mechanism), and all have identical poller behavior that calls this mechanism.

---

## SECTION 5: RISK ASSESSMENT

**Risk Level:** HIGH

**Justification:**

1. **Authority Leakage Risk:**
   - **HIGH:** Pollers (database-specific components) are making resume eligibility decisions and executing them via adapter methods
   - This creates a parallel authority path alongside ResumeEngine
   - Violates single-source-of-truth principle for resume decisions
   - Creates potential for inconsistent resume behavior

2. **Timing Risk:**
   - **LOW:** Pollers use @Scheduled which is allowed for reconciliation
   - No blocking waits or timeouts introduced into test execution threads
   - Scheduled polling does not block test execution

3. **Architectural Integrity Risk:**
   - **HIGH:** The rule explicitly states "Resume eligibility is evaluated ONLY by ResumeEngine, regardless of persistence backend"
   - Current implementation violates this by allowing pollers to mark resumeReady
   - This creates a maintenance burden and potential for bugs
   - Future changes to resume logic must be coordinated across multiple components (ResumeEngine + 3 pollers)

4. **State Mutation Risk:**
   - **MEDIUM:** Adapters provide the mechanism to mutate resumeReady state
   - While adapters don't make the decision, they enable the violation
   - Multiple code paths can mutate the same state (ResumeEngine + pollers)

**Overall Risk:** HIGH

The primary risk is authority leakage where database-specific pollers are making resume eligibility decisions, creating a parallel authority path that bypasses ResumeEngine. This violates the architectural principle that ResumeEngine is the sole authority for resume decisions.

---

## SUMMARY

**All three adapters (Mongo, Postgres, H2) are NON-COMPLIANT** with the Multi-Persistence Reconciliation Rule.

**Primary Violation:**
- Adapters implement `markResumeReady()` method that enables resumeReady flag mutation
- Database-specific pollers call this method directly, bypassing ResumeEngine authority
- This creates a parallel authority path for resume decisions

**Secondary Observations:**
- Adapters are otherwise compliant (no state decisions, no timeouts, only read/write operations)
- All adapters exhibit identical behavior (consistent violation pattern)
- Pollers use allowed reconciliation mechanisms (@Scheduled, event bus notification)

**Recommendation:**
The `markResumeReady()` method should be removed from the `EventContextPort` interface or restricted such that only ResumeEngine can invoke it. Pollers should notify ResumeEngine of matching events rather than marking resumeReady directly.

---

**END OF AUDIT REPORT**

