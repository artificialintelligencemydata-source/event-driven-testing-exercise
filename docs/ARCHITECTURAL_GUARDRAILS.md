ARCHITECTURAL_GUARDRAILS.md
AUTWIT — ARCHITECTURAL GUARDRAILS

This document defines the HARD GUARDRAILS of AUTWIT.
These rules are non-negotiable.

If implementation, refactoring, or AI-generated code
violates these guardrails, it MUST be rejected.

------------------------------------------------------------

1. AUTWIT IS EVENT-DRIVEN BY DEFINITION

AUTWIT MUST:
- React to events
- Persist events
- Resume based on data

AUTWIT MUST NOT:
- Wait for events in tests
- Poll for state in client code
- Assume synchronous behavior

------------------------------------------------------------

2. TIME MUST NEVER LEAK TO CLIENTS

AUTWIT MUST NOT expose:
- Timeouts
- Delays
- withinSeconds APIs
- await or retry semantics

Time may exist ONLY:
- Internally
- As an optimization
- Hidden from client tests

------------------------------------------------------------

3. CLIENT TESTS ARE SACRED

Client tests MUST:
- Express intent only
- Remain simple
- Depend only on the SDK

Client tests MUST NOT:
- Import engine classes
- Import ports or adapters
- Access DB or Kafka
- Reason about resume

------------------------------------------------------------

4. DATABASE IS THE SOURCE OF TRUTH

AUTWIT MUST:
- Persist all events
- Persist resume state
- Make resume decisions from DB

AUTWIT MUST NOT:
- Rely on in-memory state
- Lose correctness on restart
- Require JVM affinity

------------------------------------------------------------

5. PAUSE IS EXPECTED BEHAVIOR

A paused (skipped) test means:
- Required data is not yet available

AUTWIT MUST:
- Treat pause as success-in-progress

AUTWIT MUST NOT:
- Treat pause as failure
- Force execution to continue
- Retry tests in a loop

------------------------------------------------------------

6. RESUME BEATS RETRY

AUTWIT MUST:
- Resume tests when data appears

AUTWIT MUST NOT:
- Retry tests blindly
- Re-run tests on a timer
- Hide data delays with retries

------------------------------------------------------------

7. STRICT LAYER BOUNDARIES

The only allowed dependency flow:

Client Tests
  -> autwit-client-sdk
      -> autwit-engine
          -> Ports
              -> Adapters

Any deviation breaks the architecture.

------------------------------------------------------------

8. NO BLOCKING ANYWHERE IN TESTS

AUTWIT MUST NOT:
- Block threads in step definitions
- Use futures or latches in tests
- Wait on conditions in client code

Tests either run or exit.

------------------------------------------------------------

9. PARALLELISM IS NOT OPTIONAL

AUTWIT MUST:
- Support parallel execution
- Isolate scenario state
- Avoid shared mutable state

Assuming serial execution is a bug.

------------------------------------------------------------

10. ENGINE OWNS COMPLEXITY

Complexity belongs in:
- Engine
- Adapters
- Infrastructure

Complexity MUST NOT leak into:
- Client tests
- Feature files
- Step definitions

------------------------------------------------------------

11. SDK IS A FACADE, NOT A TOOLBOX

SDK MUST:
- Be minimal
- Be declarative
- Hide internals

SDK MUST NOT:
- Expose tuning knobs
- Expose internals
- Require configuration knowledge

------------------------------------------------------------

12. IN-MEMORY STATE IS OPTIONAL

AUTWIT MAY use in-memory structures:
- As optimizations
- As caches
- As accelerators

AUTWIT MUST NOT:
- Depend on memory for correctness
- Lose resume capability on restart

------------------------------------------------------------

13. FAILURES MUST BE HONEST

AUTWIT MUST:
- Fail when expectations are violated

AUTWIT MUST NOT:
- Mask failures
- Convert failures into pauses
- Hide assertion errors

------------------------------------------------------------

14. DOCUMENTATION IS LAW

Architectural documents:
- Define truth
- Override implementation
- Guide evolution

If code contradicts documentation,
the code is wrong.

------------------------------------------------------------

15. CHANGE REQUIRES JUSTIFICATION

Any change that touches:
- Pause semantics
- Resume semantics
- Client SDK APIs
- Layer boundaries

MUST be explicitly justified and documented.

------------------------------------------------------------

16. FINAL GUARDRAIL

AUTWIT models reality.

Reality is asynchronous.
Reality is messy.
Reality does not care about your test timing.

AUTWIT MUST NOT pretend otherwise.

------------------------------------------------------------

17. TEMPORARY DEVELOPMENT LENIENCE

During the current stabilization phase, AUTWIT allows TEMPORARY
exceptions to architectural boundaries for internal development
and core stabilization purposes ONLY.

TEMPORARY EXCEPTION: Client Step Definitions Port Access

Client step definitions MAY temporarily import internal ports:
- EventContextPort
- EventMatcherPort
- ScenarioStatePort

This exception is ONLY permitted for:
- Core framework stabilization
- Internal development and testing
- Framework maintainers working on SDK completion

This exception MUST be removed before:
- SDK hardening phase
- External adoption
- Repository split
- v1.0 release

ABSOLUTE PROHIBITION: Time-Based Logic

Even during temporary lenience, the following remain STRICTLY FORBIDDEN:
- Timeouts (CompletableFuture.get(timeout), withinSeconds, etc.)
- Sleeps (Thread.sleep, delays, etc.)
- Polling (while loops, scheduled checks, etc.)
- Retry loops (for-time logic, retry-until, etc.)
- Waiting for Kafka/DB (direct blocking, await, etc.)

Time-based logic violates the fundamental event-driven model
and is NEVER acceptable, regardless of development phase.

This lenience does NOT weaken final AUTWIT principles.
It is a temporary scaffolding measure only.

------------------------------------------------------------

18. RESUME AUTHORITY – TEMPORARY LENIENCY

⚠️ TECHNICAL DEBT ⚠️

During the current stabilization phase, AUTWIT maintains a TEMPORARY
architectural leniency regarding resume authority.

CANONICAL AUTHORITY:

ResumeEngine is the ONLY component allowed to mark resumeReady.
ResumeEngine is the SOLE authority for deciding resume eligibility.

TEMPORARY LENIENCY:

The EventContextPort interface currently exposes markResumeReady().
This method is TEMPORARILY retained for backward compatibility
during incremental stabilization of AUTWIT core.

This is an intentional, documented leniency.

RESTRICTIONS:

Only ResumeEngine is allowed to invoke markResumeReady().

The following components MUST NOT call markResumeReady():
- Pollers (MongoEventPoller, PostgresEventPoller, H2EventPoller)
- Adapters (MongoEventContextAdapter, PostgresEventContextAdapter, H2EventContextAdapter)
- Runners (CucumberTestRunner, any test execution framework integration)
- SDK (autwit-client-sdk components)
- Client code (step definitions, feature files)

This restriction is currently enforced by:
- Convention
- Architectural audits
- Documentation requirements

This restriction is NOT yet enforced by the compiler or type system.

FUTURE ENFORCEMENT:

This leniency will be removed by:
- Removing markResumeReady() from EventContextPort interface
- Restricting resumeReady mutations to ResumeEngine internal implementation
- Enforcing authority through type system and compiler checks

The reason for this temporary leniency is incremental stabilization
of AUTWIT core, allowing gradual migration to fully event-driven
resume orchestration.

This leniency does NOT change the architectural principle that
ResumeEngine is the sole authority for resume decisions.

------------------------------------------------------------

END OF FILE
