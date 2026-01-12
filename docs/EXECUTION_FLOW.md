# AUTWIT Execution Flow

This document describes how AUTWIT executes tests, from scenario start to completion or pause/resume.

---

## Overview

AUTWIT execution follows a deterministic, event-driven model:

1. **Scenario starts** → Fresh execution or resume
2. **Steps execute** → Sequential, with pause capability
3. **Event expectations** → Check database, pause if missing
4. **Resume** → Triggered by ResumeEngine when events arrive
5. **Completion** → All steps pass, scenario marked complete

---

## Execution Modes

### Fresh Execution

For a new scenario:

1. Test framework (TestNG/Cucumber) discovers scenario
2. Runner initializes scenario context
3. Scenario enters `RUNNING` state
4. Steps execute sequentially
5. Scenario either:
   - Completes (all steps pass)
   - Pauses (event missing)
   - Fails (assertion error)

### Resume Execution

For a resumed scenario:

1. ResumeEngine marks scenario as `RESUME_READY`
2. Runner queries for `RESUME_READY` scenarios
3. Scenario scheduled like a normal test
4. Scenario context is restored
5. Execution resumes from paused step
6. Scenario proceeds normally

**Key Point:** Fresh and resume execution use the **SAME execution pipeline**. No special resume-only code paths.

---

## Step Execution Model

### Normal Step Execution

```
Step Definition Called
    ↓
Set Current Step Name (via autwit.context().setCurrentStep())
    ↓
Execute Step Logic
    ↓
Mark Step Success (via autwit.step().markStepSuccess())
    ↓
Next Step
```

### Step with Event Expectation

```
Step Definition Called
    ↓
Declare Event Expectation (autwit.expectEvent(orderId, eventType))
    ↓
EventMatcherPort.match() checks database
    ↓
    ├─ Event Found → Continue
    └─ Event Missing → Throw SkipException
                            ↓
                    Scenario Paused
                    State Persisted
                    Thread Released
```

---

## Pause Mechanism

### When Pause Occurs

A scenario pauses when:
- Expected event not found in database
- External API call fails (transient error)
- Required data is unavailable

### How Pause Works

1. Step throws `SkipException`
2. Runner marks scenario as `PAUSED`
3. Scenario state persisted to database
4. Execution thread is released
5. No failure is recorded

**Pause is a SUCCESSFUL control outcome.**

### Pause State Persistence

When paused:
- Scenario key stored
- Step name stored
- Context data stored
- Expected events stored
- State: `PAUSED`

---

## Resume Mechanism

### Resume Trigger

Resume is triggered by **ResumeEngine**, not by time or retries.

**Flow:**
```
External Event Arrives
    ↓
Adapter Persists Event to DB
    ↓
Adapter Notifies ResumeEngine
    ↓
ResumeEngine Queries Paused Scenarios
    ↓
ResumeEngine Evaluates Matching
    ↓
ResumeEngine Marks Scenario RESUME_READY
    ↓
Runner Discovers RESUME_READY Scenarios
    ↓
Runner Schedules Scenario for Execution
    ↓
Scenario Context Restored
    ↓
Execution Resumes from Paused Step
```

### Resume Conditions

A scenario becomes `RESUME_READY` ONLY when:
- Scenario state is `PAUSED`
- All expected events exist in database
- No unresolved expectations remain

**Partial readiness does NOT trigger resume.**

### Resume State Transitions

```
PAUSED → RESUME_READY → RESUMING → RUNNING
```

Only ResumeEngine can transition `PAUSED → RESUME_READY`.

---

## Context Restoration

### Scenario Context Lifecycle

**Creation:**
- Created when scenario starts
- Bound to execution thread (ThreadLocal)
- Initialized with scenario key

**During Execution:**
- Stores order IDs, correlation IDs, test data
- Survives pause/resume
- Isolated per scenario

**Restoration on Resume:**
- Context data loaded from database
- ThreadLocal re-bound
- MDC logging correlation restored

**Cleanup:**
- Context cleared when scenario completes
- ThreadLocal removed
- MDC cleared

---

## Event Matching Flow

### Declaration

```java
autwit.expectEvent(orderId, "ORDER_CREATED").assertSatisfied();
```

### Matching Process

1. **Canonical key constructed:** `orderId + eventType`
2. **Database query:** `EventMatcherPort.match(canonicalKey)`
3. **Result:**
   - **Found:** Event expectation satisfied, continue
   - **Not Found:** Throw `SkipException`, pause scenario

### Matching Rules

- **DB-first:** Always check database first
- **Deterministic:** Same key always matches same event
- **Idempotent:** Duplicate events don't cause issues
- **Payload-agnostic:** Matching based on key, not payload

---

## Step Success Tracking

### Marking Step Success

```java
autwit.step().markStepSuccess();
```

**What happens:**
1. Current step name extracted from context
2. Scenario key extracted from context
3. `ScenarioStatePort.markStep()` called
4. Step marked as `SUCCESS` in database

### Why Track Step Success

- Prevents re-execution of completed steps on resume
- Enables step-level resume (future enhancement)
- Provides execution history

---

## Failure Handling

### When Failure Occurs

A scenario fails when:
- Assertion error thrown
- Explicit failure in test logic
- Invalid business state detected

### Failure Behavior

1. Scenario marked `FAILED`
2. Execution stops permanently
3. No resume eligibility
4. Context finalized

**Failures are FINAL.**

---

## Parallel Execution

### Isolation

- Each scenario has isolated context
- Canonical keys prevent collisions
- No shared mutable state
- Parallel execution is safe by design

### Thread Safety

- ThreadLocal context per scenario
- Database-level isolation via canonical keys
- ResumeEngine supports concurrent resumes
- No global locks required

---

## Temporary Scaffolding

### TestNG-Based Resume (Temporary)

**Current Behavior:**
- ResumeEngine marks `resumeReady=true`
- Runner does NOT query `resumeReady` scenarios
- TestNG retry executor independently re-executes skipped tests
- TestNG retry operates independently of `resumeReady` state

**This bypasses canonical flow and will be removed.**

**Canonical Intended Flow:**
1. ResumeEngine evaluates paused scenarios
2. ResumeEngine marks eligible scenarios as `RESUME_READY`
3. Runner periodically queries for `RESUME_READY` scenarios
4. Runner schedules `RESUME_READY` scenarios for execution
5. Runner transitions `RESUME_READY → RESUMING → RUNNING`
6. Scenario context restored and execution continues

---

## Complete Execution Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    SCENARIO START                           │
│  (Fresh or Resume)                                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              CONTEXT INITIALIZATION                          │
│  Create/Restore ScenarioContext                            │
│  Bind ThreadLocal                                           │
│  Set MDC Logging                                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    STEP EXECUTION                           │
│  Execute Step Definition                                    │
│  Set Current Step Name                                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
            ┌──────────┴──────────┐
            │                     │
            ▼                     ▼
    ┌──────────────┐    ┌──────────────────┐
    │ Event Check  │    │ Business Logic   │
    │ (if needed)  │    │ (API calls, etc) │
    └──────┬───────┘    └────────┬─────────┘
           │                     │
           │                     │
           ▼                     ▼
    ┌─────────────────────────────────────┐
    │         Event Found?                │
    └──────┬──────────────────┬───────────┘
           │                  │
      YES  │                  │ NO
           │                  │
           ▼                  ▼
    ┌──────────────┐   ┌──────────────┐
    │ Mark Success │   │ PAUSE        │
    │ Next Step    │   │ SkipException│
    └──────┬───────┘   └──────┬───────┘
           │                  │
           │                  │
           ▼                  ▼
    ┌─────────────────────────────────────┐
    │      More Steps?                    │
    └──────┬──────────────────┬───────────┘
           │                  │
      YES  │                  │ NO
           │                  │
           ▼                  ▼
    ┌──────────────┐   ┌──────────────┐
    │ Next Step    │   │ COMPLETE     │
    └──────────────┘   └──────────────┘
```

---

## Key Takeaways

1. **Execution is deterministic** — No time-based decisions
2. **Pause is intentional** — Not a failure, but a control mechanism
3. **Resume is state-driven** — Triggered by data, not time
4. **Context survives pause/resume** — Isolated and persistent
5. **Parallel execution is safe** — Canonical keys prevent collisions

---

## Final Rule

If execution flow depends on:
- Time
- Thread scheduling
- JVM memory state
- Retry loops

Then the design is broken.

Execution must be purely state-driven.

