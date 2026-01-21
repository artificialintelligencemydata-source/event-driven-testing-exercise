# AUTWIT VIOLATION CLASSIFICATION

**Classification Date:** 2026-01-05  
**Mode:** Audit Only — No Code Changes

---

## BUCKET A: MUST FIX (Foundational AUTWIT Violations)

### A1. Client Code Direct Port Imports
- **Bucket:** A (MUST FIX)
- **File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`
- **Lines:** 4-6
- **Justification:** Breaks architectural encapsulation. Client code must only depend on client SDK, not engine internals.
- **Spec Violated:** `ARCHITECTURAL_GUARDRAILS.md`, `RUNNER_VS_CLIENT_BOUNDARY.md`

### A2. Client Code Timeout Usage (Multiple Instances)
- **Bucket:** A (MUST FIX)
- **File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`
- **Lines:** 119, 157, 193, 229, 265, 301
- **Justification:** Violates core event-driven principle. Tests must pause immediately if event not found, not wait with timeouts.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`, `FAILURE_VS_PAUSE_SEMANTICS.md`

### A3. Client SDK Internal Timeout
- **Bucket:** A (MUST FIX)
- **File:** `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/EventExpectationImpl.java`
- **Lines:** 29-30
- **Justification:** SDK should not wait. Must immediately pause if event not found in DB.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`, `FAILURE_VS_PAUSE_SEMANTICS.md`

### A4. Missing Explicit State Model
- **Bucket:** A (MUST FIX)
- **File:** Entire codebase (no state enum found)
- **Lines:** N/A
- **Justification:** States are implicit boolean flags. Must have explicit enum: CREATED, RUNNING, WAITING_FOR_EVENT, PAUSED, RESUME_READY, RESUMING, COMPLETED, FAILED.
- **Spec Violated:** `SCENARIO_STATE_MODEL.md`

### A5. Runner Missing RESUME_READY Discovery
- **Bucket:** A (MUST FIX)
- **File:** `autwit-runner/src/test/java/com/acuver/autwit/runner/CucumberTestRunner.java`
- **Lines:** 27-65 (entire class)
- **Justification:** Runner must periodically query for RESUME_READY scenarios and schedule them. Without this, resume functionality is non-functional.
- **Spec Violated:** `RUNNER_RESUME_COORDINATION.md`, `RUNNER_EXECUTION_MODEL.md`

### A6. Pollers Mark ResumeReady (Mongo)
- **Bucket:** A (MUST FIX)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/MongoEventPoller.java`
- **Lines:** 52
- **Justification:** Only ResumeEngine may mark resumeReady. Pollers violate single responsibility.
- **Spec Violated:** `RESUME_ENGINE_INTERNAL_FLOW.md`

### A7. Pollers Mark ResumeReady (Postgres)
- **Bucket:** A (MUST FIX)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/PostgresEventPoller.java`
- **Lines:** 53
- **Justification:** Only ResumeEngine may mark resumeReady. Pollers violate single responsibility.
- **Spec Violated:** `RESUME_ENGINE_INTERNAL_FLOW.md`

### A8. Pollers Mark ResumeReady (H2)
- **Bucket:** A (MUST FIX)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/H2EventPoller.java`
- **Lines:** 53
- **Justification:** Only ResumeEngine may mark resumeReady. Pollers violate single responsibility.
- **Spec Violated:** `RESUME_ENGINE_INTERNAL_FLOW.md`

### A9. Timestamp-Based OrderId Generation
- **Bucket:** A (MUST FIX)
- **File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`
- **Lines:** 76
- **Justification:** Keys must be stable across pause/resume. Timestamps break correlation and resume functionality.
- **Spec Violated:** `SCENARIO_KEY_MODEL.md`

---

## BUCKET B: TEMPORARY / TOLERABLE (Scaffolding or Safety Mechanisms)

### B1. Scheduled Polling (Mongo)
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/MongoEventPoller.java`
- **Lines:** 32
- **Justification:** Scheduled polling is a safety net for reconciliation but violates "no timers" principle. Acceptable as temporary scaffolding until event-driven path is fully functional.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`, `RESUME_ENGINE_INTERNAL_FLOW.md`

### B2. Scheduled Polling (Postgres)
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/PostgresEventPoller.java`
- **Lines:** 32
- **Justification:** Scheduled polling is a safety net for reconciliation but violates "no timers" principle. Acceptable as temporary scaffolding until event-driven path is fully functional.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`, `RESUME_ENGINE_INTERNAL_FLOW.md`

### B3. Scheduled Polling (H2)
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/scheduler/H2EventPoller.java`
- **Lines:** 32
- **Justification:** Scheduled polling is a safety net for reconciliation but violates "no timers" principle. Acceptable as temporary scaffolding until event-driven path is fully functional.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`, `RESUME_ENGINE_INTERNAL_FLOW.md`

### B4. TTL Timeout in EventStepNotifier
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java`
- **Lines:** 66-71
- **Justification:** 1-hour TTL is a safety mechanism to prevent memory leaks in in-memory waiters. Violates "no timeouts" but serves as cleanup safeguard.
- **Spec Violated:** `EVENT_MATCHING_RULES.md`

### B5. Time-Based Metadata in State Tracker
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/state/ScenarioStateTracker.java`
- **Lines:** 34
- **Justification:** `lastUpdated` timestamp is audit metadata only, not used for state decisions. Acceptable for observability but should not influence state transitions.
- **Spec Violated:** `SCENARIO_STATE_MODEL.md` (minor)

### B6. Hash-Based Key Generation
- **Bucket:** B (TEMPORARY / TOLERABLE)
- **File:** `autwit-core/autwit-internal-testkit/src/main/java/com/acuver/autwit/internal/Hooks.java`
- **Lines:** 37-38
- **Justification:** Hash-based key may not be collision-resistant for high parallelism. Acceptable as temporary implementation but should be replaced with proper execution ID generation.
- **Spec Violated:** `SCENARIO_KEY_MODEL.md`

---

## BUCKET C: ACCEPTABLE / ALIGNED (No Action Required)

### C1. Client SDK Dependency on Ports
- **Bucket:** C (ACCEPTABLE / ALIGNED)
- **File:** `autwit-client-sdk/src/main/java/com/acuver/autwit/client/sdk/AutwitImpl.java`
- **Lines:** 10-13
- **Justification:** Client SDK is part of the framework boundary, not external client code. Dependency on ports is acceptable as SDK is the facade layer.
- **Spec Violated:** None (ambiguous in spec, but acceptable given SDK's role)

### C2. In-Memory Waiters in EventStepNotifier
- **Bucket:** C (ACCEPTABLE / ALIGNED)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/notifier/EventStepNotifier.java`
- **Lines:** 25-26, 59-63
- **Justification:** In-memory waiters are explicitly allowed as optimization. Spec states "In-memory structures: Are optional, Are lossy, Are NOT authoritative" — DB-first matching is implemented.
- **Spec Violated:** None (explicitly allowed by `EVENT_MATCHING_RULES.md`)

### C3. ResumeEngine Event Persistence
- **Bucket:** C (ACCEPTABLE / ALIGNED)
- **File:** `autwit-core/autwit-engine/src/main/java/com/acuver/autwit/engine/resume/ResumeEngine.java`
- **Lines:** 49
- **Justification:** ResumeEngine persists events before matching, which aligns with DB-first matching principle.
- **Spec Violated:** None

### C4. SkipException Usage for Pauses
- **Bucket:** C (ACCEPTABLE / ALIGNED)
- **File:** `client-tests/src/test/java/com/bjs/tests/stepDefinitions/EventDrivenOrderLifecycleStepDefs.java`
- **Lines:** 103, 138, 176, 212, 248, 284, 318
- **Justification:** Correctly uses SkipException to pause scenarios. Aligns with pause semantics specification.
- **Spec Violated:** None

---

## SUMMARY BY BUCKET

### Bucket A (MUST FIX): 9 violations
- Architecture boundary violations: 1
- Timeout violations: 2
- State model violations: 1
- Resume coordination violations: 4
- Key stability violations: 1

### Bucket B (TEMPORARY / TOLERABLE): 6 violations
- Polling mechanisms: 3
- Safety timeouts: 1
- Metadata timestamps: 1
- Key generation: 1

### Bucket C (ACCEPTABLE / ALIGNED): 4 items
- SDK architecture: 1
- Optimization mechanisms: 1
- Correct implementations: 2

---

**END OF CLASSIFICATION**

