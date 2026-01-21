FAILURE_VS_PAUSE_SEMANTICS.md
AUTWIT — FAILURE VS PAUSE SEMANTICS

This document defines the STRICT semantic difference
between FAILURE and PAUSE in AUTWIT.

Confusing these two concepts breaks the framework.

------------------------------------------------------------

1. PURPOSE

Failure vs Pause semantics exist to:

- Eliminate flaky tests
- Distinguish business failure from execution unavailability
- Enable deterministic resume
- Prevent false negatives in async systems

------------------------------------------------------------

2. DEFINITIONS (CANONICAL)

FAILURE:
- A definitive, terminal test outcome
- Indicates incorrect system behavior

PAUSE:
- A controlled, non-terminal execution state
- Indicates system state not yet ready

These are NOT interchangeable.

------------------------------------------------------------

3. FAILURE — WHAT IT MEANS

A FAILURE means:

- The system violated a contract
- An assertion failed
- An invariant was broken

Failure indicates:
THE SYSTEM IS WRONG.

------------------------------------------------------------

4. PAUSE — WHAT IT MEANS

A PAUSE means:

- The system has not yet reached the expected state
- Required information is not available
- External dependency is temporarily unavailable

Pause indicates:
THE SYSTEM IS NOT READY YET.

------------------------------------------------------------

5. VALID FAILURE CAUSES

A scenario may FAIL only due to:

- Assertion failure
- Explicit failure in test logic
- Invalid business state
- Corrupt or contradictory data

------------------------------------------------------------

6. INVALID FAILURE CAUSES

The following MUST NEVER cause FAILURE:

- Missing events
- Event delay
- Kafka lag
- Database replication delay
- Network outage
- Service downtime
- Timeout expiration

These MUST cause PAUSE instead.

------------------------------------------------------------

7. VALID PAUSE CAUSES

A scenario MUST PAUSE when:

- Expected event not present
- Event matching incomplete
- External API unreachable
- Order creation failed transiently
- System dependency unavailable

Pause is a SUCCESSFUL control decision.

------------------------------------------------------------

8. PAUSE IMPLEMENTATION

Pause is implemented by:

- Throwing SkipException
- Persisting scenario state
- Releasing execution thread

Pause MUST NOT:
- Mark test failed
- Record assertion failure
- Trigger retries

------------------------------------------------------------

9. FAILURE IMPLEMENTATION

Failure is implemented by:

- Assertion errors
- Explicit failure exceptions

Failure MUST:
- Mark scenario FAILED
- Stop execution permanently
- Prevent resume

------------------------------------------------------------

10. RESUME ELIGIBILITY

Only PAUSED scenarios are eligible for resume.

FAILED scenarios:
- Are terminal
- Are never resumed
- Are never retried

------------------------------------------------------------

11. TIME IS NOT A DECISION FACTOR

Time must NEVER decide:

- Failure
- Pause
- Resume

Timeouts are forbidden in AUTWIT.

------------------------------------------------------------

12. CLIENT CODE RULES

Client step definitions:

- MUST throw SkipException to pause
- MUST throw assertion errors to fail
- MUST NOT decide resume eligibility
- MUST NOT apply timeouts

------------------------------------------------------------

13. ENGINE RULES

Engine:

- NEVER fails scenarios due to missing events
- NEVER applies timeouts
- ONLY resumes based on state

------------------------------------------------------------

14. COMMON ANTI-PATTERNS

The following indicate broken design:

- Failing after waiting X seconds
- Retrying assertions
- Using Awaitility
- Using Thread.sleep
- Catching SkipException and continuing

------------------------------------------------------------

15. OBSERVABILITY REQUIREMENTS

Logs MUST clearly indicate:

- Pause reason
- Failure reason
- Scenario state transition

Ambiguous outcomes are forbidden.

------------------------------------------------------------

16. FINAL RULE

If a scenario fails because:
“the system was slow”

Then AUTWIT has been misused.

AUTWIT waits for correctness, not time.

------------------------------------------------------------

END OF FILE
