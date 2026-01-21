RESUME_ENGINE_RULES.md
AUTWIT — RESUME ENGINE RULES

This document defines the NON-NEGOTIABLE rules governing
the ResumeEngine in AUTWIT.

The ResumeEngine is the heart of AUTWIT.
If behavior violates this document, the implementation is wrong.

------------------------------------------------------------

1. PURPOSE OF THE RESUME ENGINE

The ResumeEngine exists to:
- Resume paused test scenarios
- Based solely on persisted truth
- Without relying on time
- Without relying on in-memory state

The ResumeEngine is NOT a retry mechanism.

------------------------------------------------------------

2. DATABASE IS THE SOURCE OF TRUTH

The ResumeEngine MUST treat the database as:
- The authoritative state store
- The resume decision source
- The historical audit log

The ResumeEngine MUST assume:
- JVM restarts will happen
- In-memory data may be lost
- Resume must still work

------------------------------------------------------------

3. RESUME IS DATA-DRIVEN

A scenario becomes resumable ONLY when:
- Required event/state is persisted
- Resume conditions are satisfied in DB

Resume MUST NOT be triggered by:
- Timers
- Thread sleeps
- Scheduled delays
- Client code

Data arrival is the only trigger.

------------------------------------------------------------

4. PAUSE SEMANTICS

A scenario is paused when:
- Required data is missing
- Preconditions are not yet satisfied

Pause is implemented via:
- SkipException
- Test framework skip semantics

Pause is EXPECTED behavior.

------------------------------------------------------------

5. RESUME SEMANTICS

Resume behavior:
- Re-execute only the paused scenario
- Continue from remaining steps
- Preserve scenario identity
- Preserve business key

Resume MUST NOT:
- Re-run completed steps unnecessarily
- Re-run unrelated scenarios
- Depend on execution order

------------------------------------------------------------

6. NO DIRECT TEST WAKEUP

The ResumeEngine MUST NOT:
- Wake threads directly
- Resume blocked tests
- Signal waiting step definitions

Tests do NOT block.
They exit and are re-executed.

------------------------------------------------------------

7. IDENTITY AND CANONICAL KEYS

Each scenario MUST have:
- A canonical scenario key
- A business key (orderId, caseId, etc.)

Resume decisions MUST be made using:
- Canonical keys
- Persisted mappings

In-memory correlation is an optimization only.

------------------------------------------------------------

8. IDEMPOTENCY REQUIREMENT

The ResumeEngine MUST be idempotent.

This means:
- Multiple resume evaluations cause no harm
- Duplicate events do not cause duplicate resumes
- Resume can be evaluated repeatedly

Idempotency is mandatory.

------------------------------------------------------------

9. PARALLEL SAFETY

The ResumeEngine MUST:
- Support multiple scenarios resuming concurrently
- Avoid global locks
- Avoid shared mutable state

Parallel resume is the default.

------------------------------------------------------------

10. FAILURE HANDLING

ResumeEngine MUST distinguish:

- Missing data → remain paused
- Invalid data → test failure
- Infrastructure error → explicit error

ResumeEngine MUST NOT:
- Convert failures into pauses
- Mask real assertion failures

------------------------------------------------------------

11. NO TIME-BASED LOGIC

The ResumeEngine MUST NOT:
- Sleep
- Wait
- Retry with delays
- Depend on clocks

Polling, if any, is internal and hidden,
and MUST NOT affect client semantics.

------------------------------------------------------------

12. EVENT ARRIVAL HANDLING

When an event arrives:
- It MUST be persisted first
- Resume conditions MUST be evaluated after persistence
- In-memory waiters may be notified as optimization

Persistence ALWAYS comes first.

------------------------------------------------------------

13. TEST FRAMEWORK INTEGRATION

ResumeEngine MAY integrate with:
- TestNG
- Cucumber
- Other runners

But it MUST:
- Remain framework-agnostic internally
- Isolate framework specifics at the edges

------------------------------------------------------------

14. OBSERVABILITY REQUIREMENTS

ResumeEngine SHOULD:
- Log resume decisions
- Log pause reasons
- Log resume triggers

Logs MUST use:
- Scenario keys
- Business keys

------------------------------------------------------------

15. CHANGE POLICY

Changes to the ResumeEngine MUST:
- Preserve client behavior
- Preserve pause/resume semantics
- Be backward compatible

Breaking resume behavior breaks AUTWIT.

------------------------------------------------------------

16. FINAL RULE

The ResumeEngine exists to model reality.

Reality is asynchronous.
Reality is unordered.
Reality is slow.

The ResumeEngine MUST embrace this.

------------------------------------------------------------

END OF FILE
