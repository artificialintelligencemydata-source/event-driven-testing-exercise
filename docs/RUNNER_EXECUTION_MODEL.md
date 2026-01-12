RUNNER_EXECUTION_MODEL.md
AUTWIT — RUNNER EXECUTION MODEL

This document defines the EXACT execution model of the AUTWIT Runner.

The runner is a BOOTSTRAP + EXECUTION SHELL.
It is NOT part of the orchestration engine.

------------------------------------------------------------

1. PURPOSE OF THE RUNNER

The runner exists to:

- Boot the test runtime
- Integrate with TestNG / Cucumber
- Execute scenarios when scheduled
- Provide lifecycle hooks for internal testkit

The runner does NOT:
- Own state
- Decide readiness
- Match events
- Resume scenarios by itself

------------------------------------------------------------

2. RUNNER POSITION IN ARCHITECTURE

Runner sits between:

- Test execution frameworks (TestNG / Cucumber)
- AUTWIT Core (engine + ResumeEngine)

Runner is REPLACEABLE.
AUTWIT Core is NOT.

------------------------------------------------------------

3. WHAT THE RUNNER KNOWS

Runner knows about:

- Scenario identifiers
- Execution lifecycle callbacks
- Test framework semantics
- Resume readiness (via queries)

Runner does NOT know about:

- Event storage
- Matching rules
- Canonical keys
- Pause logic

------------------------------------------------------------

4. EXECUTION MODES

Runner supports two execution modes:

1) Fresh execution
2) Resume execution

Both modes use the SAME execution pipeline.

------------------------------------------------------------

5. FRESH EXECUTION FLOW

For a new scenario:

1. Test framework discovers scenario
2. Runner initializes scenario context
3. Scenario enters RUNNING state
4. Steps execute sequentially
5. Scenario either:
   - Completes
   - Pauses
   - Fails

Runner does NOT distinguish fresh vs resume at step level.

------------------------------------------------------------

6. RESUME EXECUTION FLOW

For a resumed scenario:

1. Runner queries RESUME_READY scenarios
2. Scenario scheduled like a normal test
3. Scenario context is restored
4. Execution resumes from paused step
5. Scenario proceeds normally

No special resume-only code paths are allowed.

------------------------------------------------------------

6A. TEMPORARY SCAFFOLDING: TESTNG-BASED RESUME

During the current stabilization phase, AUTWIT uses a TEMPORARY
scaffolding mechanism that bypasses the canonical resume flow.

CURRENT BEHAVIOR (TEMPORARY):

- ResumeEngine marks resumeReady=true when events match
- Runner DOES NOT query resumeReady scenarios
- TestNG retry executor independently re-executes skipped tests
- TestNG retry operates independently of resumeReady state
- Resume readiness is not checked before retry execution

This behavior BYPASSES the canonical AUTWIT resume flow:
ResumeEngine → resumeReady → Runner discovery → Resuming → Running

Instead, the current flow is:
ResumeEngine → resumeReady (unused) → TestNG retry → SkipException → Retry

This is ACCEPTED TEMPORARILY for framework stabilization and will be
REMOVED once Runner-driven resume discovery is implemented.

CANONICAL INTENDED FLOW (PRESERVED):

The intended final architecture remains:
1. ResumeEngine evaluates paused scenarios
2. ResumeEngine marks eligible scenarios as RESUME_READY
3. Runner periodically queries for RESUME_READY scenarios
4. Runner schedules RESUME_READY scenarios for execution
5. Runner transitions RESUME_READY → RESUMING → RUNNING
6. Scenario context is restored and execution continues

This canonical flow ensures:
- State-driven resumption (not time-driven)
- Explicit runner control over resume scheduling
- Proper state transitions through RESUME_READY
- Separation of concerns (Engine decides, Runner executes)

FUTURE REMOVAL CRITERIA:

The TestNG-based resume scaffolding will be removed when:

1. Runner implements resumeReady discovery:
   - Runner queries EventContextPort for scenarios with resumeReady=true
   - Runner maintains a discovery mechanism (periodic or event-driven)

2. Runner controls resumption explicitly:
   - Runner transitions RESUME_READY → RESUMING → RUNNING
   - Runner restores scenario context before execution
   - Runner logs resume events with scenario key

3. TestNG retry executor is deprecated:
   - TestNG retry is no longer used for resume
   - SkipException triggers pause only, not retry
   - Resume is fully controlled by Runner + ResumeEngine coordination

Once these criteria are met, the canonical resume flow will be
fully operational and the temporary scaffolding will be removed.

------------------------------------------------------------

7. SCENARIO CONTEXT HANDLING

Runner responsibilities:

- Create scenario context on start
- Restore context on resume
- Bind context to execution thread
- Clean up context on completion

Context is opaque to runner.
Runner does not interpret context content.

------------------------------------------------------------

8. PARALLEL EXECUTION MODEL

Runner must:

- Support parallel scenario execution
- Isolate scenario contexts
- Avoid shared mutable state

Parallelism is controlled by:
- TestNG configuration
- Runner thread pool
- NOT by engine logic

------------------------------------------------------------

9. PAUSE HANDLING

When a scenario pauses:

- Step throws SkipException
- Runner marks scenario as PAUSED
- Execution thread is released
- No failure is recorded

Pause is a SUCCESSFUL control outcome.

------------------------------------------------------------

10. FAILURE HANDLING

When a scenario fails:

- Assertion error or explicit failure
- Runner marks scenario FAILED
- No resume eligibility exists
- Execution stops permanently

Failures are FINAL.

------------------------------------------------------------

11. COMPLETION HANDLING

When a scenario completes:

- All steps executed successfully
- Runner marks scenario COMPLETED
- Context is finalized
- No further execution occurs

------------------------------------------------------------

12. WHAT RUNNER MUST NEVER DO

Runner MUST NOT:

- Poll for events
- Inspect databases
- Retry scenarios
- Decide resume eligibility
- Sleep or wait
- Interpret payloads

------------------------------------------------------------

13. INTEGRATION WITH INTERNAL TESTKIT

Runner integrates with:

- Hooks (before/after)
- ScenarioContext
- MDC / logging
- Test listeners

These are INTERNAL components.
They are NOT client APIs.

------------------------------------------------------------

14. ERROR ISOLATION GUARANTEE

Runner must ensure:

- One scenario failure does not affect others
- One pause does not block others
- Resume does not starve fresh execution

------------------------------------------------------------

15. OBSERVABILITY REQUIREMENTS

Runner MUST log:

- Scenario start
- Scenario pause
- Scenario resume
- Scenario completion
- Scenario failure

Logs must include scenario key and execution mode.

------------------------------------------------------------

16. FINAL RULE

If the runner ever needs to ask:

“Is this scenario allowed to run?”

Then the architecture is broken.

Runner executes.
Engine decides.

------------------------------------------------------------

END OF FILE
