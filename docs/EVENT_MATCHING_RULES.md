EVENT_MATCHING_RULES.md
AUTWIT — EVENT MATCHING RULES

This document defines the ONLY valid rules
by which AUTWIT matches system events to test expectations.

These rules are NON-NEGOTIABLE.

------------------------------------------------------------

1. PURPOSE

Event matching exists to:

- Determine when a scenario can proceed
- Enable pause/resume without timing
- Eliminate polling and sleeps
- Ensure determinism in asynchronous systems

Event matching is ENGINE RESPONSIBILITY ONLY.

------------------------------------------------------------

2. EVENT MATCHING PRINCIPLES

AUTWIT event matching follows these principles:

- DB-first, always
- Deterministic, not time-based
- Idempotent
- Payload-agnostic
- Scenario-isolated
- Parallel-safe

If any rule below is violated, the design is incorrect.

------------------------------------------------------------

3. EVENT DEFINITION

An event is defined by:

- Canonical Key (mandatory)
- Event Type (mandatory)
- Correlation identifiers (orderId, etc.)
- Raw payload (opaque)

AUTWIT treats payload as:
- Stored
- Indexed if needed
- NEVER interpreted for matching

------------------------------------------------------------

4. CANONICAL KEY RULE

Every event MUST have a canonical key.

Canonical key properties:
- Deterministic
- Stable across retries
- Unique per expectation

Example (conceptual):
orderId + eventType

AUTWIT never matches events without a canonical key.

------------------------------------------------------------

5. EVENT EXPECTATION DECLARATION

An expectation is declared when client code calls:

autwit.expectEvent(orderId, eventType)

At this point:
- No DB access occurs in client code
- No waiting occurs in client code
- No timing logic is involved

------------------------------------------------------------

6. MATCHING FLOW (HIGH LEVEL)

When an expectation is declared:

1. Engine checks DB for matching event
2. If found → expectation satisfied immediately
3. If not found → scenario is paused

NO OTHER FLOW IS VALID.

------------------------------------------------------------

7. DB-FIRST MATCHING (MANDATORY)

Matching ALWAYS happens in this order:

1) Persistent store (DB)
2) In-memory waiters (optimization only)

In-memory structures:
- Are optional
- Are lossy
- Are NOT authoritative

DB is the single source of truth.

------------------------------------------------------------

8. EVENT ARRIVAL HANDLING

When an event arrives (Kafka, webhook, etc.):

1. Adapter persists event to DB
2. Adapter publishes internal event notification
3. Engine evaluates paused expectations
4. Matching scenarios are marked RESUME_READY

Order MUST NOT be reversed.

------------------------------------------------------------

9. IDENTITY MATCHING ONLY

AUTWIT matches events based on:

- Canonical key
- Event type

AUTWIT does NOT match based on:
- Payload fields
- Partial content
- JSON paths
- Regex
- Time windows

Payload validation is a SEPARATE concern.

------------------------------------------------------------

10. MULTIPLE EXPECTATIONS

A scenario may declare multiple expectations.

Rules:
- Each expectation is independent
- Each has its own canonical key
- Expectations are matched one at a time

Parallel expectations in a single step are forbidden.

------------------------------------------------------------

11. DUPLICATE EVENTS

If the same event arrives multiple times:

- First arrival satisfies expectation
- Subsequent arrivals are ignored
- No duplicate resume is triggered

Event matching must be idempotent.

------------------------------------------------------------

12. OUT-OF-ORDER EVENTS

If an event arrives BEFORE expectation is declared:

- Event is persisted
- Future expectation matches immediately

This is REQUIRED behavior.

------------------------------------------------------------

13. MISSING EVENTS

If an event never arrives:

- Scenario remains PAUSED
- No timeout is applied
- No failure is triggered

AUTWIT never fails due to missing events.

------------------------------------------------------------

14. ILLEGAL MATCHING BEHAVIORS

The following are STRICTLY FORBIDDEN:

- Polling DB from step definitions
- Using sleeps or waits
- Client-controlled timeouts
- Matching on payload content
- Matching on timestamps
- Retrying expectations in client code

------------------------------------------------------------

15. ERROR HANDLING DURING MATCHING

If an error occurs while evaluating matching:

- Scenario is PAUSED
- Error is logged
- Resume is deferred

Errors must never crash the engine.

------------------------------------------------------------

16. RESUME TRIGGER CONDITIONS

A scenario becomes RESUME_READY ONLY when:

- All declared expectations are satisfied
- Matching is confirmed via DB

Partial satisfaction does not trigger resume.

------------------------------------------------------------

17. OBSERVABILITY REQUIREMENTS

Every matching decision MUST be:

- Logged with scenario key
- Traceable to an event record
- Reproducible via DB inspection

------------------------------------------------------------

18. FINAL RULE

If event matching logic depends on:
- Time
- Thread scheduling
- JVM memory state
- Retry loops

Then the design is broken.

Event matching must be purely state-driven.

------------------------------------------------------------

END OF FILE
