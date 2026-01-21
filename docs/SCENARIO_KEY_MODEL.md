SCENARIO_KEY_MODEL.md
AUTWIT — SCENARIO KEY MODEL

This document defines the CANONICAL model
for scenario identification in AUTWIT.

Scenario keys are CRITICAL to determinism.
Incorrect keys break resume, matching, and observability.

------------------------------------------------------------

1. PURPOSE OF SCENARIO KEYS

Scenario keys exist to:

- Uniquely identify a scenario execution
- Correlate events, state, and logs
- Enable pause and resume
- Support parallel execution
- Survive restarts and reruns

Scenario keys are NOT display names.

------------------------------------------------------------

2. KEY OWNERSHIP

Owned by:
- AUTWIT Core (generation rules)
- Runner (execution binding)

Invisible to:
- Client SDK users (structure hidden)
- Feature authors (implicit)

------------------------------------------------------------

3. KEY PROPERTIES (MANDATORY)

Every scenario key MUST be:

- Globally unique per execution
- Deterministic
- Stable across pause/resume
- Collision-resistant
- Serializable
- Human-traceable

------------------------------------------------------------

4. KEY COMPOSITION (LOGICAL MODEL)

A scenario key is composed of:

- Scenario identity
- Execution identity

Logical representation:

SCENARIO_KEY = SCENARIO_ID + EXECUTION_ID

------------------------------------------------------------

5. SCENARIO ID

Scenario ID identifies the logical test.

Derived from:
- Feature file name
- Scenario name
- Example index (for Scenario Outline)

Scenario ID is:
- Stable across runs
- Same for all resumes

------------------------------------------------------------

6. EXECUTION ID

Execution ID identifies a single execution instance.

Properties:
- Generated once per initial run
- Preserved across pause/resume
- NOT regenerated on resume

Execution ID ensures isolation between parallel runs.

------------------------------------------------------------

7. KEY STABILITY RULE

Once generated:

- Scenario key MUST NEVER change
- Resume MUST reuse the same key
- Logs MUST reference the same key

Changing the key breaks resume.

------------------------------------------------------------

8. KEY GENERATION TIMING

Scenario key is generated:

- BEFORE first step execution
- AFTER scenario discovery
- BEFORE any pause can occur

Runner is responsible for binding the key.

------------------------------------------------------------

9. KEY USAGE

Scenario key is used for:

- Scenario state persistence
- Context persistence
- Event expectation correlation
- Logging and MDC
- Resume scheduling

Scenario key is NOT used for:
- Assertions
- Matching payloads
- Business logic

------------------------------------------------------------

10. KEY VISIBILITY

Scenario key is:

- Logged in every state transition
- Exposed in internal logs
- Not part of client API

Client code MUST NOT depend on key structure.

------------------------------------------------------------

11. PARALLEL EXECUTION GUARANTEE

Two scenarios running in parallel MUST:

- Have distinct scenario keys
- Never share execution IDs
- Never overwrite each other’s state

------------------------------------------------------------

12. ILLEGAL KEY PRACTICES

The following are FORBIDDEN:

- Using timestamps in keys
- Using random UUIDs without structure
- Recomputing keys on resume
- Allowing client code to define keys
- Using orderId alone as key

------------------------------------------------------------

13. FAILURE AND KEY LIFETIME

Scenario key lifetime:

- Starts at scenario creation
- Ends at COMPLETED or FAILED

Keys are never reused.

------------------------------------------------------------

14. OBSERVABILITY REQUIREMENTS

Every log entry related to a scenario MUST include:

- Scenario key
- Current state

Missing keys indicate instrumentation failure.

------------------------------------------------------------

15. FINAL RULE

If a scenario cannot be resumed because:
“we lost the key”

Then the framework is broken.

Scenario keys are the backbone of AUTWIT.

------------------------------------------------------------

END OF FILE
