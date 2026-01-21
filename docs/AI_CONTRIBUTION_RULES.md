AI_CONTRIBUTION_RULES.md
AUTWIT — RULES FOR AI ASSISTED DEVELOPMENT

This document defines the NON-NEGOTIABLE rules
for any AI system contributing to AUTWIT.

If an AI suggestion violates this document,
the suggestion MUST be rejected.

------------------------------------------------------------

1. AUTWIT IS NOT A TRADITIONAL AUTOMATION FRAMEWORK

AI MUST understand:
- AUTWIT is event-driven
- AUTWIT is resumable
- AUTWIT is parallel-safe
- AUTWIT does NOT rely on time

AI MUST NOT propose:
- Sleeps
- Waits
- Polling loops
- Client-side retries
- Timeout-based assertions

------------------------------------------------------------

2. TIME IS FORBIDDEN IN CLIENT APIS

AI MUST NEVER:
- Introduce time-based APIs to client SDK
- Suggest withinSeconds(...)
- Suggest await(timeout)
- Suggest retry-until patterns

Time may exist ONLY:
- Internally
- As an engine optimization
- Hidden from client tests

------------------------------------------------------------

3. STRICT LAYER BOUNDARIES MUST BE PRESERVED

AI MUST respect the following boundaries:

Client Tests
  -> autwit-client-sdk
      -> autwit-engine
          -> Ports
              -> Adapters

AI MUST NOT:
- Expose engine internals to clients
- Inject ports into step definitions
- Suggest direct DB or Kafka access in tests

------------------------------------------------------------

4. CLIENT TESTS EXPRESS INTENT ONLY

AI MUST generate client code that:
- Declares expectations
- Triggers actions
- Avoids mechanics

AI MUST NOT generate:
- Execution control logic
- Resume logic
- Pause logic
- Infrastructure logic

------------------------------------------------------------

5. PAUSE IS NOT FAILURE

AI MUST understand:
- Skipped tests represent paused scenarios
- Paused scenarios are expected
- Resume happens via data, not code

AI MUST NOT suggest:
- Converting skips to failures
- Preventing SkipException
- Re-running tests via loops

------------------------------------------------------------

6. DATABASE IS THE SOURCE OF TRUTH

AI MUST assume:
- All resume decisions are DB-driven
- In-memory state is optional and lossy
- JVM restarts are expected

AI MUST NOT suggest:
- In-memory-only coordination
- Memory-based resume logic
- Static maps as authoritative state

------------------------------------------------------------

7. NO POLLING IN TESTS

AI MUST NEVER generate:
- while(...) loops waiting for state
- Scheduled polling in step definitions
- Repeated DB or Kafka queries from tests

Polling belongs ONLY:
- Inside the engine
- Inside adapters
- Hidden from client code

------------------------------------------------------------

8. ENGINE EVOLUTION > TEST HACKS

If AI encounters a limitation:

AI MUST suggest:
- SDK evolution
- Engine improvement
- Adapter enhancement

AI MUST NOT suggest:
- Test workarounds
- Client-side hacks
- Feature file tricks

------------------------------------------------------------

9. PARALLELISM IS THE DEFAULT

AI MUST design assuming:
- Multiple scenarios run concurrently
- No shared mutable state
- No ordering guarantees

AI MUST NOT:
- Assume serial execution
- Introduce global locks
- Rely on static variables in tests

------------------------------------------------------------

10. FEATURE FILES MUST BE TIMELESS

AI MUST generate feature files without:
- Seconds
- Minutes
- Delays
- Time windows

Feature files describe BUSINESS TRUTH,
not execution behavior.

------------------------------------------------------------

11. FAILURE SEMANTICS MUST BE PRESERVED

AI MUST preserve:
- Assertion failure = real failure
- Missing data = pause
- Infrastructure failure = explicit error

AI MUST NOT blur these distinctions.

------------------------------------------------------------

12. STABILITY OF CLIENT SDK IS SACRED

AI MUST assume:
- Client SDK is stable
- Breaking SDK changes are unacceptable
- Compatibility is a priority

AI MAY evolve:
- Engine
- Adapters
- Internal testkit

------------------------------------------------------------

13. DOCUMENTATION IS PART OF THE SYSTEM

AI MUST:
- Update architecture documents when design changes
- Respect existing documents as authoritative
- Reject changes that contradict documentation

------------------------------------------------------------

14. IF IN DOUBT, ASK

If AI is uncertain:
- It MUST ask for clarification
- It MUST NOT guess
- It MUST NOT invent behavior

Silence is better than wrong code.

------------------------------------------------------------

15. FINAL AI OATH

AI MUST remember:

AUTWIT models reality,
not convenience.

Correctness beats speed.
Design beats hacks.
Intent beats mechanics.

------------------------------------------------------------

END OF FILE
