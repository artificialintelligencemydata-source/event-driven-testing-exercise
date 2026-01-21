RESUME_ENGINE_INTERNAL_FLOW.md
AUTWIT — RESUME ENGINE INTERNAL FLOW

This document defines the INTERNAL and AUTHORITATIVE
flow of the ResumeEngine in AUTWIT.

This is ENGINE-ONLY behavior.
Client code must NEVER interact with this flow.

------------------------------------------------------------

1. PURPOSE OF RESUME ENGINE

The ResumeEngine exists to:

- Resume paused scenarios safely
- Ensure deterministic continuation
- Eliminate retries and timeouts
- Coordinate with the runner without coupling

ResumeEngine is the ONLY component allowed
to transition scenarios out of PAUSED state.

------------------------------------------------------------

2. OWNERSHIP AND VISIBILITY

Owned by:
- autwit-core (engine)

Visible to:
- Runner (trigger only)
- Internal adapters (event notifications)

Invisible to:
- Client SDK
- Step definitions
- Feature files

------------------------------------------------------------

3. INPUT SOURCES

ResumeEngine reacts to ONLY two inputs:

1) Event arrival notifications
2) Periodic state reconciliation trigger

NO OTHER TRIGGERS ARE VALID.

------------------------------------------------------------

4. EVENT-DRIVEN RESUME FLOW (PRIMARY)

This is the preferred and dominant path.

Flow:

1. External event arrives
2. Adapter persists event to DB
3. Adapter emits internal notification
4. ResumeEngine receives notification
5. ResumeEngine queries paused scenarios
6. ResumeEngine evaluates expectations
7. Matching scenarios are marked RESUME_READY

This flow MUST be synchronous up to DB persistence.

------------------------------------------------------------

5. RECONCILIATION FLOW (SAFETY NET)

This flow exists for recovery only.

Trigger examples:
- Engine restart
- Adapter restart
- Missed in-memory signal

Flow:

1. ResumeEngine scans DB for PAUSED scenarios
2. Evaluates stored expectations
3. Marks eligible scenarios RESUME_READY

This flow MUST NOT:
- Rely on timers in tests
- Be exposed to clients

------------------------------------------------------------

6. RESUME READINESS CRITERIA

A scenario is RESUME_READY ONLY when:

- Scenario state is PAUSED
- All expected events exist in DB
- No unresolved expectations remain

Partial readiness is NOT allowed.

------------------------------------------------------------

7. STATE TRANSITIONS

ResumeEngine performs ONLY the following transition:

PAUSED → RESUME_READY

It MUST NOT:
- Resume execution directly
- Invoke test code
- Modify runner state

------------------------------------------------------------

7A. TEMPORARY LENIENCY — POLLER-ASSISTED RESUME MARKING

During the current stabilization phase, AUTWIT temporarily allows
database pollers (MongoEventPoller, PostgresEventPoller, H2EventPoller)
to mark scenarios as resumeReady directly.

CURRENT BEHAVIOR:

Pollers perform the following operations:
1. Query database for PAUSED scenarios
2. Check if matching events exist in DB
3. Call storage.markResumeReady() directly when matches are found

This behavior violates the ideal AUTWIT single-authority model where
ResumeEngine is the ONLY component authorized to transition scenarios
from PAUSED → RESUME_READY.

WHY THIS EXISTS:

This temporary leniency exists as scaffolding to:
- Provide resume functionality during framework stabilization
- Enable testing of resume behavior before full event-driven orchestration
- Allow gradual migration to fully event-driven resume flow
- Support reconciliation when event notifications are missed

CANONICAL AUTHORITY:

ResumeEngine remains the long-term canonical authority for resume decisions.
The poller-assisted marking is a temporary mechanism that will be removed
once fully event-driven resume orchestration is stable.

FUTURE REMOVAL:

This behavior will be removed when:
- Event-driven resume flow is fully operational and stable
- ResumeEngine handles all resume readiness decisions
- Pollers are restricted to reconciliation-only roles (if retained)
- All resume marking flows through ResumeEngine exclusively

⚠️ WARNING ⚠️

Pollers MUST NOT be considered authoritative for resume decisions.

- No new logic should depend on pollers marking resumeReady
- Pollers are temporary scaffolding, not architectural components
- Future refactors must route all resume decisions through ResumeEngine
- Do not extend poller responsibilities beyond current temporary scope
- Do not create dependencies on poller behavior

The canonical resume authority is ResumeEngine. Poller behavior is
temporary and subject to removal.

------------------------------------------------------------

8. RUNNER INTEGRATION

Runner responsibilities:

- Periodically ask for RESUME_READY scenarios
- Schedule them for execution
- Transition RESUME_READY → RESUMING

ResumeEngine responsibilities:

- Mark readiness
- Persist readiness state
- Provide resumable scenario metadata

Direct invocation between runner and engine is forbidden.

------------------------------------------------------------

9. RESUME EXECUTION GUARANTEE

ResumeEngine guarantees:

- At-least-once resume eligibility
- Never duplicate resume transitions
- Idempotent state marking

If runner crashes after RESUME_READY,
scenario remains resumable.

------------------------------------------------------------

10. FAILURE HANDLING

If ResumeEngine encounters an error:

- Scenario remains PAUSED
- Error is logged
- Resume is retried via reconciliation

ResumeEngine MUST NEVER mark FAILED.

------------------------------------------------------------

11. CONCURRENCY RULES

ResumeEngine must:

- Support parallel event arrivals
- Prevent double resume marking
- Use DB-level idempotency

In-memory locks alone are insufficient.

------------------------------------------------------------

12. ILLEGAL RESPONSIBILITIES

ResumeEngine MUST NOT:

- Execute test steps
- Call client code
- Perform assertions
- Interpret payloads
- Apply timeouts
- Sleep or wait

------------------------------------------------------------

13. OBSERVABILITY REQUIREMENTS

Every resume decision MUST:

- Log scenario key
- Log triggering event(s)
- Log previous and new state
- Be traceable via DB

Silent resumes are forbidden.

------------------------------------------------------------

14. IDEMPOTENCY GUARANTEE

Calling ResumeEngine multiple times with:

- Same scenario
- Same events

MUST result in the same outcome.

------------------------------------------------------------

15. FINAL RULE

If resumption depends on:

- Thread timing
- Test execution order
- JVM memory state

Then the ResumeEngine design is broken.

Resumption must be purely state-driven.

------------------------------------------------------------

END OF FILE
