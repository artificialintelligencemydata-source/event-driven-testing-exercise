RUNNER_RESUME_COORDINATION.md
AUTWIT — RUNNER ↔ RESUME ENGINE COORDINATION

This document defines the EXACT coordination contract
between the AUTWIT Runner and the ResumeEngine.

This is a CONTROL-PLANE contract.
Execution logic NEVER crosses this boundary.

------------------------------------------------------------

1. PURPOSE

Runner ↔ ResumeEngine coordination exists to:

- Resume paused scenarios deterministically
- Keep execution logic out of the engine
- Keep state logic out of the runner
- Avoid tight coupling between components

Runner is an EXECUTION SHELL.
ResumeEngine is a STATE ORCHESTRATOR.

------------------------------------------------------------

2. RESPONSIBILITY SPLIT (NON-NEGOTIABLE)

ResumeEngine:
- Owns scenario state
- Decides resume eligibility
- Persists state transitions
- Never executes tests

Runner:
- Discovers runnable scenarios
- Schedules execution
- Boots test environments
- Never decides readiness

------------------------------------------------------------

3. COMMUNICATION MODEL

Runner and ResumeEngine communicate ONLY via:

- Persistent state (DB)
- Well-defined query APIs

There is:
- No direct callbacks
- No method invocation chain
- No shared memory

------------------------------------------------------------

4. RUNNER STARTUP FLOW

When the runner starts:

1. Initialize execution environment
2. Register listeners (TestNG / Cucumber)
3. Start ResumeEngine (if embedded)
4. Query for RESUME_READY scenarios

Runner MUST NOT assume a clean slate.

------------------------------------------------------------

5. RESUME DISCOVERY FLOW

Runner periodically performs:

- Query for scenarios with state = RESUME_READY

This query:
- Is idempotent
- May return previously attempted scenarios
- Must tolerate duplicates

Runner MUST treat discovery as stateless.

------------------------------------------------------------

5A. TEMPORARY LENIENCY — RETRY-DRIVEN RESUME

⚠️ TECHNICAL DEBT ⚠️

During the current stabilization phase, AUTWIT uses a TEMPORARY
scaffolding mechanism that bypasses the canonical runner-driven
resume discovery flow.

CURRENT BEHAVIOR (TEMPORARY):

The runner does NOT yet actively discover RESUME_READY scenarios
from storage. There is NO explicit resume scheduling loop.

Instead, resume currently depends on:
- TestNG retry semantics
- Same-JVM execution continuity
- In-memory test framework state

ResumeEngine continues to mark scenarios as resumeReady=true
when events match, but the runner does NOT query for these
scenarios. Resume readiness is not checked before retry execution.

This behavior violates the canonical coordination contract where:
- Runner queries for RESUME_READY scenarios
- Runner schedules resumable scenarios explicitly
- Resume is state-driven, not retry-driven

AUTHORITY PRESERVATION:

ResumeEngine remains the SOLE authority to mark resumeReady.
This leniency does NOT change ResumeEngine's responsibility
or authority. The violation is in the runner's discovery mechanism,
not in the state marking authority.

This is acceptable ONLY as scaffolding during framework stabilization.
This behavior MUST NOT be relied upon long-term.

⚠️ WARNING ⚠️

This leniency MUST be removed before AUTWIT is declared stable.

Production correctness depends on explicit resume coordination:
- Runner-driven resume discovery
- State-driven scheduling
- Persisted state as source of truth
- Elimination of retry-based resume dependencies

Do NOT:
- Build new features assuming retry-based resume
- Create dependencies on TestNG retry behavior
- Treat this as a permanent pattern
- Extend this leniency to other coordination areas

FUTURE STATE:

The canonical resume coordination will be implemented as follows:

1. RUNNER-DRIVEN RESUME DISCOVERY:
   - Runner periodically queries EventContextPort for scenarios with resumeReady=true
   - Runner maintains a discovery mechanism (periodic polling or event-driven)
   - Discovery is idempotent and stateless
   - Runner logs discovery events with scenario keys

2. RESUME SCHEDULING BASED ON PERSISTED STATE:
   - Runner schedules RESUME_READY scenarios based on persisted state queries
   - Scheduling is explicit and traceable
   - Runner transitions RESUME_READY → RESUMING → RUNNING
   - Scenario context is restored from persisted state

3. ELIMINATION OF RETRY-BASED RESUME:
   - TestNG retry is no longer used for resume functionality
   - SkipException triggers pause only, not retry
   - Resume is fully controlled by Runner + ResumeEngine coordination
   - No dependency on in-memory test framework state for resume

Once the canonical coordination is implemented, this temporary
leniency will be removed and the retry-based resume path will
be eliminated.

------------------------------------------------------------

6. RESUME SCHEDULING RULES

For each RESUME_READY scenario:

Runner MUST:
- Schedule it exactly like a new scenario
- Reconstruct scenario context
- Preserve original scenario key

Runner MUST NOT:
- Skip steps
- Fast-forward execution
- Inject artificial state

------------------------------------------------------------

7. STATE TRANSITION AUTHORITY

Only the runner may perform:

RESUME_READY → RESUMING → RUNNING

ResumeEngine MUST NOT perform these transitions.

------------------------------------------------------------

8. FAILURE DURING RESUME

If a failure occurs during resume execution:

- Scenario transitions to FAILED
- ResumeEngine is NOT involved
- No automatic retry occurs

Resume is not retry.
Resume is continuation.

------------------------------------------------------------

9. DOUBLE RESUME PROTECTION

Runner MUST protect against:

- Scheduling the same scenario twice concurrently

Mechanisms may include:
- DB compare-and-set
- Execution locks
- Runner-local deduplication

ResumeEngine guarantees idempotent readiness,
not execution exclusivity.

------------------------------------------------------------

10. RUNNER CRASH SCENARIOS

If runner crashes:

- RESUME_READY scenarios remain RESUME_READY
- PAUSED scenarios remain PAUSED
- No state corruption occurs

ResumeEngine does NOT roll back state.

------------------------------------------------------------

11. ENGINE CRASH SCENARIOS

If ResumeEngine crashes:

- Runner continues executing RUNNING scenarios
- Resume readiness is delayed
- Reconciliation restores readiness

No coordination repair is required from runner.

------------------------------------------------------------

12. ILLEGAL COORDINATION PATTERNS

The following are FORBIDDEN:

- Runner calling ResumeEngine.resume()
- ResumeEngine triggering test execution
- Shared static state
- Thread signaling between runner and engine
- Time-based resume checks

------------------------------------------------------------

13. OBSERVABILITY REQUIREMENTS

Runner MUST log:

- Resume discovery
- Resume scheduling
- Resume execution start
- Resume execution end

Logs must include scenario key.

------------------------------------------------------------

14. FINAL GUARANTEE

Runner ↔ ResumeEngine coordination ensures:

- Loose coupling
- Crash resilience
- Deterministic resumption
- Parallel safety

If coordination requires:
- Timing assumptions
- Retry loops
- Synchronization hacks

Then the design is broken.

------------------------------------------------------------

END OF FILE
