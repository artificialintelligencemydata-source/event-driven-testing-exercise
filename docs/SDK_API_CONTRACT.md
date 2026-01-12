SDK_API_CONTRACT.md
AUTWIT — CLIENT SDK API CONTRACT

This document defines the OFFICIAL, PUBLIC, and STABLE API
exposed to client tests through autwit-client-sdk.

Anything not listed here is NOT part of the contract.

If code violates this contract, the code is wrong.

------------------------------------------------------------

1. PURPOSE OF THE CLIENT SDK

The client SDK exists to:
- Allow tests to express intent
- Hide engine complexity
- Prevent misuse of internals
- Provide long-term stability

The SDK is the ONLY entry point for client tests.

------------------------------------------------------------

2. STABILITY GUARANTEE

The following guarantees apply:

- Client SDK APIs are stable
- Client tests MUST NOT break due to engine changes
- Breaking SDK changes are forbidden without major version bump

Internal modules may change freely.
The SDK contract does not.

------------------------------------------------------------

3. ALLOWED CLIENT OPERATIONS

Client tests may:
- Declare expected events
- Declare expected states
- Express scenario intent
- Trigger business actions (outside AUTWIT)

Client tests may NOT:
- Control timing
- Control execution order
- Control pause or resume
- Access persistence
- Access messaging systems

------------------------------------------------------------

4. CORE SDK ENTRY POINT

The SDK exposes exactly ONE primary entry point.

Interface name:
Autwit

Responsibilities:
- Accept client intent
- Return expectation handles
- Hide engine behavior

Clients MUST obtain Autwit via dependency injection.

------------------------------------------------------------

5. EXPECT EVENT API

Public API:

Autwit.expectEvent(businessKey, eventType)

Parameters:
- businessKey: unique identifier (orderId, caseId, etc.)
- eventType: logical event name

Returns:
- EventExpectation

Behavior:
- If event already exists, assertion succeeds immediately
- If event does not exist, scenario pauses automatically
- No waiting, polling, or retrying occurs in client code

------------------------------------------------------------

6. EVENT EXPECTATION API

Public API:

EventExpectation.assertSatisfied()

Behavior:
- Confirms the expected event exists
- Delegates timing and resume logic to engine
- Throws assertion failure only if event violates expectations

Client MUST NOT:
- Catch SkipException
- Catch internal engine exceptions
- Wrap assertions with retries

------------------------------------------------------------

7. TIME IS NOT PART OF THE CONTRACT

The SDK MUST NOT expose:
- Timeouts
- Durations
- withinSeconds
- await
- retryUntil

Any time-based behavior is strictly internal.

------------------------------------------------------------

8. PAUSE AND RESUME SEMANTICS

Client-visible behavior:

- Missing data causes scenario pause (Skipped)
- Pause is expected and correct
- Resume happens automatically when data arrives

Client MUST NOT:
- Force resume
- Detect resume
- React to pause explicitly

------------------------------------------------------------

9. ERROR AND FAILURE SEMANTICS

SDK distinguishes:

- Pause (Skipped): data not available yet
- Failure: assertion violation
- Error: infrastructure or configuration issue

Client code MUST NOT blur these cases.

------------------------------------------------------------

10. THREAD SAFETY AND PARALLELISM

SDK guarantees:
- Safe usage across parallel scenarios
- Isolation per business key
- No shared mutable client state

Client MUST assume:
- Concurrent execution
- Non-deterministic ordering

------------------------------------------------------------

11. WHAT SDK DOES NOT DO

SDK does NOT:
- Start Spring
- Configure TestNG or Cucumber
- Manage lifecycle hooks
- Own adapters or ports
- Expose internal state

Those responsibilities belong elsewhere.

------------------------------------------------------------

12. FORBIDDEN SDK CHANGES

SDK MUST NEVER:
- Leak engine classes
- Leak ports
- Leak adapters
- Introduce time-based APIs
- Require client code changes for engine refactors

------------------------------------------------------------

13. SDK EXTENSION RULES

If new capability is needed:

DO:
- Add a new intent-based API
- Keep API declarative
- Preserve backward compatibility

DO NOT:
- Add low-level controls
- Add configuration knobs
- Add tuning parameters

------------------------------------------------------------

14. DOCUMENTATION REQUIREMENT

Every public SDK API MUST:
- Be documented
- Have clear semantics
- Have examples
- Be reflected in this file

Undocumented APIs are not allowed.

------------------------------------------------------------

15. FINAL CONTRACT STATEMENT

The client SDK is a PROMISE.

It promises:
- Simplicity
- Stability
- Correctness

Breaking this promise breaks AUTWIT.

------------------------------------------------------------

END OF FILE
