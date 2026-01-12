TEST_LIFECYCLE_FLOW.md
AUTWIT — TEST LIFECYCLE FLOW

This document defines the COMPLETE lifecycle of a test
in AUTWIT from execution start to final completion.

This is the canonical flow.
No alternative flows are allowed.

------------------------------------------------------------

1. CORE PHILOSOPHY

AUTWIT tests are NOT synchronous tests.

AUTWIT tests are:
- Event-driven
- Resume-capable
- Non-blocking
- Parallel-safe
- Deterministic

A test NEVER waits.
A test either:
- Proceeds
- Pauses
- Resumes
- Completes

------------------------------------------------------------

2. HIGH-LEVEL LIFECYCLE OVERVIEW

The AUTWIT test lifecycle has the following stages:

1) Scenario bootstrap
2) Intent execution
3) Event expectation registration
4) Pause decision
5) External system processing
6) Event arrival
7) Resume decision
8) Scenario continuation
9) Scenario completion

Each stage is explicit and observable.

------------------------------------------------------------

3. SCENARIO BOOTSTRAP

Triggered by:
- TestNG runner
- Cucumber runner
- AUTWIT runner module

Actions:
- Scenario ID generated
- ScenarioContext created
- Correlation keys computed
- Scenario registered with ResumeEngine

Rules:
- No business logic here
- No assertions here
- No external calls here

------------------------------------------------------------

4. INTENT EXECUTION (GIVEN STEPS)

Purpose:
- Express business intent
- Trigger external behavior

Examples:
- Place order
- Send API request
- Publish message

Rules:
- Intent MAY fail due to external unavailability
- If intent fails → scenario is PAUSED
- Intent failure NEVER fails the test immediately

Outcome:
- Either intent accepted
- Or scenario paused

------------------------------------------------------------

5. EVENT EXPECTATION REGISTRATION

Triggered by:
- Then / And steps declaring expectations

Examples:
- Expect ORDER_CREATED
- Expect PAYMENT_CONFIRMED

Actions:
- Expected events registered with engine
- Canonical event keys computed
- Scenario marked as WAITING_FOR_EVENT

Rules:
- No timeouts
- No polling
- No sleeps

------------------------------------------------------------

6. PAUSE DECISION

Decision point:
- Is the expected event already present?

If YES:
- Scenario continues immediately

If NO:
- Scenario is PAUSED
- Execution thread is released
- Scenario state persisted

Key Rule:
PAUSE IS NOT FAILURE.

------------------------------------------------------------

7. EXTERNAL SYSTEM PROCESSING

Occurs OUTSIDE the test lifecycle.

Examples:
- Kafka consumers process messages
- Databases updated
- Downstream services respond

AUTWIT does NOT control this phase.
AUTWIT only observes outcomes.

------------------------------------------------------------

8. EVENT ARRIVAL

Triggered by:
- Kafka listener
- DB poller
- Adapter callback

Actions:
- Event persisted
- Canonical key matched
- ResumeEngine notified

Rules:
- Multiple events may arrive
- Order does not matter
- Matching is deterministic

------------------------------------------------------------

9. RESUME DECISION

ResumeEngine evaluates:
- Which paused scenarios are eligible
- Which expectations are now satisfied

If conditions met:
- Scenario marked RESUME_READY

If not:
- Scenario remains paused

No test threads are involved here.

------------------------------------------------------------

10. SCENARIO RESUMPTION

Triggered by:
- ResumeEngine + runner integration

Actions:
- Only paused scenarios are re-executed
- Execution resumes from the paused step
- No steps are re-run unnecessarily

Rules:
- Resume is idempotent
- Resume is deterministic
- Resume is parallel-safe

------------------------------------------------------------

11. CONTINUATION STEPS

After resume:
- Next step executes
- Further expectations may be registered
- Scenario may pause again

A scenario can pause and resume MULTIPLE times.

------------------------------------------------------------

12. SCENARIO COMPLETION

Completion conditions:
- All steps executed
- All expectations satisfied

Final states:
- PASSED
- FAILED (logic/assertion failure only)
- SKIPPED (paused and pending resume)

Rules:
- Infrastructure issues do not cause failure
- Missing events do not cause failure
- Only violated assertions cause failure

------------------------------------------------------------

13. FAILURE SEMANTICS

A scenario FAILS only when:
- Business assertion fails
- Invalid state is detected
- Explicit failure is raised

A scenario does NOT fail due to:
- Kafka delay
- DB delay
- Service downtime
- Timing issues

------------------------------------------------------------

14. PARALLEL EXECUTION GUARANTEES

AUTWIT guarantees:
- No shared mutable state
- Scenario-level isolation
- Deterministic resume behavior

Parallelism is a first-class feature.

------------------------------------------------------------

15. OBSERVABILITY

Each lifecycle stage is observable via:
- Logs
- Scenario state
- Resume metadata

Nothing is hidden.
Nothing is implicit.

------------------------------------------------------------

16. FINAL RULE

If a test needs:
- sleep
- timeout
- retry loop
- polling

Then it is NOT an AUTWIT test.

Fix the model, not the timing.

------------------------------------------------------------

END OF FILE
