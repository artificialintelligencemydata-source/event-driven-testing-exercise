SCENARIO_STATE_MODEL.md
AUTWIT — SCENARIO STATE MODEL (DERIVED SEMANTICS)

-------------------------------------------------------------------------------

PURPOSE

This document defines the COMPLETE and EXCLUSIVE execution semantics for a
scenario in AUTWIT.

AUTWIT does NOT implement a finite state machine.
AUTWIT does NOT maintain an explicit scenario state enum.
AUTWIT does NOT perform state transitions.

Scenario meaning is DERIVED from persisted execution facts and runner outcomes.

-------------------------------------------------------------------------------

CORE PRINCIPLE

AUTWIT scenarios do not "move through states".

Instead, scenario meaning is derived from:

- Persisted execution flags (paused, resumeReady)
- Step execution outcomes (success / failure)
- Runner-level execution result (completed / failed / skipped)

There is no lifecycle object.
There is no state enum.
There is no transition graph.

-------------------------------------------------------------------------------

AUTHORITATIVE FACTS

AUTWIT relies on the following authoritative facts only:

1. paused (boolean)
   - Indicates that execution was intentionally halted
   - Set when a step throws SkipException
   - Persisted by AUTWIT Core

2. resumeReady (boolean)
   - Indicates that all resume conditions are satisfied
   - Set ONLY by ResumeEngine
   - Never set by client code, steps, or pollers

3. step execution outcome
   - Which steps have already succeeded
   - Owned by the runner
   - Persisted for resume correlation

4. runner execution result
   - PASSED / FAILED / SKIPPED
   - Runner-only responsibility

No other facts are authoritative.

-------------------------------------------------------------------------------

DERIVED SCENARIO SEMANTICS

Scenario meaning is derived as follows:

| paused | resumeReady | step execution      | Derived meaning                     |
|--------|-------------|---------------------|-------------------------------------|
| false  | false       | none                | Fresh execution                     |
| true   | false       | partial             | Paused (waiting for external event) |
| true   | true        | partial             | Resume-eligible                     |
| false  | false       | all succeeded       | Completed successfully              |
| false  | false       | any failed          | Failed (non-resumable)              |

IMPORTANT:
- "Skipped" is a runner artifact, not a scenario meaning.
- AUTWIT interprets "skipped" as PAUSED when paused=true.

-------------------------------------------------------------------------------

PAUSE SEMANTICS (CRITICAL)

A scenario is considered PAUSED when ALL of the following are true:

- A required event is not available
- paused=true is persisted
- Execution is halted via SkipException

Persisting paused=true WITHOUT throwing SkipException is INVALID.

Pause is a CONTROL-FLOW decision, not a database state.

-------------------------------------------------------------------------------

RESUME SEMANTICS

A scenario becomes RESUME-ELIGIBLE when:

- paused=true
- resumeReady=true
- At least one step has already succeeded

resumeReady may ONLY be set by ResumeEngine.

ResumeEngine MUST NOT execute tests.
ResumeEngine ONLY marks eligibility.

-------------------------------------------------------------------------------

RUNNER RESPONSIBILITIES

The runner is responsible for:

- Executing steps
- Throwing SkipException to pause
- Determining PASSED / FAILED outcomes
- Re-running resume-eligible scenarios

The runner does NOT:
- Manage scenario state
- Decide resume eligibility
- Mutate paused or resumeReady flags

-------------------------------------------------------------------------------

FAILURE SEMANTICS

A scenario is FAILED when:

- A definitive assertion failure occurs
- Business validation fails
- Test logic explicitly fails execution

The following MUST NEVER cause failure:

- Missing events
- External system unavailability
- Eventual consistency delays

These conditions MUST cause PAUSE, not FAILURE.

-------------------------------------------------------------------------------

MULTI-PAUSE SUPPORT

A scenario may be paused and resumed multiple times.

Each pause corresponds to:
- A new event expectation
- A new SkipException
- A new resume eligibility evaluation

AUTWIT supports unlimited pause/resume cycles.

-------------------------------------------------------------------------------

ILLEGAL CONDITIONS (FRAMEWORK BUGS)

The following conditions indicate a framework bug:

- resumeReady=true while paused=false
- Execution continuing after paused=true
- resumeReady set by any component other than ResumeEngine
- Scenario marked completed while paused=true
- Time-based logic influencing pause or resume

-------------------------------------------------------------------------------

NON-GOALS (EXPLICIT)

AUTWIT explicitly DOES NOT:

- Maintain a scenario state enum
- Enforce explicit state transitions
- Use timeouts, sleeps, or waits
- Depend on wall-clock timing
- Implement a lifecycle state machine

-------------------------------------------------------------------------------

FINAL RULE

If scenario execution behavior depends on:

- Thread timing
- Sleep
- Retry loops
- Timeouts
- Wall-clock delays

Then the design is INVALID.

Fix the execution model, not the timing.

-------------------------------------------------------------------------------

END OF FILE
