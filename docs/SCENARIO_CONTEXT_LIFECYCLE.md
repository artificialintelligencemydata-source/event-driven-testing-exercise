SCENARIO_CONTEXT_LIFECYCLE.md
AUTWIT — SCENARIO CONTEXT LIFECYCLE

This document defines the COMPLETE lifecycle
of ScenarioContext in AUTWIT.

ScenarioContext is EXECUTION-SCOPED state.
It is NOT orchestration state.

------------------------------------------------------------

1. PURPOSE OF SCENARIO CONTEXT

ScenarioContext exists to:

- Carry scenario-specific data across steps
- Survive pause and resume
- Isolate parallel executions
- Enable deterministic continuation

ScenarioContext does NOT:
- Decide execution flow
- Own pause/resume logic
- Interact with DB directly

------------------------------------------------------------

2. CONTEXT OWNERSHIP

Owned by:
- autwit-internal-testkit

Managed by:
- Runner lifecycle

Invisible to:
- AUTWIT Core engine
- ResumeEngine
- Client SDK

------------------------------------------------------------

3. CONTEXT SCOPE

ScenarioContext scope is:

- Exactly ONE scenario
- One logical execution chain
- Potentially multiple JVM executions (resume)

Context must never leak across scenarios.

------------------------------------------------------------

4. CONTEXT CREATION

ScenarioContext is created when:

- Runner starts executing a scenario

Creation steps:

1. Generate scenario execution key
2. Initialize empty context
3. Bind context to execution thread
4. Register MDC / logging correlation

------------------------------------------------------------

5. CONTEXT POPULATION

ScenarioContext may store:

- Order IDs
- Correlation IDs
- Generated test data
- Execution metadata

Context MUST NOT store:

- Engine ports
- DB connections
- Kafka clients
- Resume state flags
- Thread references

------------------------------------------------------------

6. CONTEXT ACCESS

Context is accessed via:

- Internal testkit APIs
- Thread-local binding

Client step definitions:
- May read/write context data
- Must not depend on context lifecycle

------------------------------------------------------------

7. CONTEXT DURING RUNNING

While scenario is RUNNING:

- Context is mutable
- Bound to execution thread
- Accessible to steps

Context updates are in-memory only.

------------------------------------------------------------

8. CONTEXT DURING PAUSE

When scenario pauses:

- Context is serialized
- Persisted via internal testkit
- Detached from execution thread

Runner releases execution resources.

------------------------------------------------------------

9. CONTEXT PERSISTENCE RULES

Persisted context must be:

- Serializable
- Deterministic
- Version-tolerant

Context persistence is:

- NOT a DB transaction
- NOT authoritative
- Used ONLY for resume

------------------------------------------------------------

10. CONTEXT RESTORATION

On resume:

1. Runner loads persisted context
2. Context is deserialized
3. Bound to new execution thread
4. MDC is restored
5. Scenario continues

Context restoration MUST be lossless.

------------------------------------------------------------

11. CONTEXT DURING RESUMING

During RESUMING:

- Context is read-only
- Validation may occur
- No mutations allowed until RUNNING

------------------------------------------------------------

12. CONTEXT CLEANUP

When scenario ends (COMPLETED or FAILED):

- Context is cleared
- Persisted state is removed
- Thread-local references cleaned

No residual state must remain.

------------------------------------------------------------

13. PARALLEL SAFETY

ScenarioContext must:

- Be isolated per scenario
- Avoid static mutable fields
- Avoid cross-thread access

Violation causes non-determinism.

------------------------------------------------------------

14. ILLEGAL CONTEXT USAGE

The following are FORBIDDEN:

- Using ScenarioContext as cache
- Sharing context between scenarios
- Using context to signal resume
- Using context to store engine state
- Using context to store timing info

------------------------------------------------------------

15. OBSERVABILITY REQUIREMENTS

Context lifecycle MUST log:

- Creation
- Serialization
- Restoration
- Cleanup

Logs must include scenario key.

------------------------------------------------------------

16. FINAL RULE

If ScenarioContext is required for:

- Event matching
- Resume eligibility
- State transitions

Then the architecture is broken.

Context is execution support only.

------------------------------------------------------------

END OF FILE
