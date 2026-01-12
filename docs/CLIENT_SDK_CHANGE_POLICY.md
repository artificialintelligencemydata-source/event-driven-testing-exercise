CLIENT_SDK_CHANGE_POLICY.md
AUTWIT — CLIENT SDK CHANGE POLICY

This document defines the STRICT policy governing changes
to autwit-client-sdk.

The client SDK is a CONTRACT.
Breaking it breaks AUTWIT.

------------------------------------------------------------

1. ROLE OF THE CLIENT SDK

The client SDK is:
- The ONLY entry point for client tests
- A facade hiding all engine complexity
- A stability boundary

The client SDK is NOT:
- A convenience toolkit
- A configuration surface
- A reflection of engine internals

------------------------------------------------------------

2. STABILITY IS THE TOP PRIORITY

The client SDK MUST prioritize:
- Backward compatibility
- Predictable behavior
- Minimal surface area

Breaking client tests is the WORST possible failure.

------------------------------------------------------------

3. WHAT COUNTS AS A BREAKING CHANGE

The following are BREAKING changes:

- Removing a public API
- Changing method signatures
- Changing semantic behavior
- Introducing required parameters
- Changing pause vs failure behavior
- Exposing time where none existed

Breaking changes REQUIRE a major version bump.

------------------------------------------------------------

4. WHAT IS NOT A BREAKING CHANGE

The following are NOT breaking changes:

- Internal engine refactors
- Adapter changes
- Performance improvements
- Resume logic optimizations
- Internal timeout tuning
- Logging changes

If client code does not change, it is not breaking.

------------------------------------------------------------

5. ADDING NEW SDK APIS

New SDK APIs MAY be added only if they are:
- Declarative
- Intent-based
- Time-agnostic
- Backward compatible

Examples of acceptable additions:
- New expectation types
- New intent expressions
- New scenario abstractions

------------------------------------------------------------

6. FORBIDDEN SDK EVOLUTIONS

The SDK MUST NEVER:
- Add time-based APIs
- Add configuration knobs
- Add tuning parameters
- Add engine or adapter exposure
- Require clients to understand internals

If an API needs explanation, it is probably wrong.

------------------------------------------------------------

7. DEFAULTS OVER CONFIGURATION

The SDK MUST:
- Work with zero configuration
- Choose safe defaults internally

The SDK MUST NOT:
- Push configuration burden to clients
- Require performance tuning by users

Simplicity is part of the contract.

------------------------------------------------------------

8. CLIENT CODE MUST NOT CHANGE FOR ENGINE EVOLUTION

Engine evolution MUST:
- Be invisible to client tests
- Preserve SDK semantics
- Preserve pause/resume behavior

If engine changes require client updates,
the design is wrong.

------------------------------------------------------------

9. DEPRECATION POLICY

If an SDK API must be deprecated:

- It MUST remain functional
- It MUST be documented
- It MUST have a clear replacement
- It MUST remain until next major version

Silent removal is forbidden.

------------------------------------------------------------

10. VERSIONING RULES

The SDK MUST follow semantic versioning:

- MAJOR: Breaking changes
- MINOR: Backward-compatible additions
- PATCH: Bug fixes only

Version bumps must reflect reality.

------------------------------------------------------------

11. DOCUMENTATION REQUIREMENT

Any SDK change MUST:
- Update SDK_API_CONTRACT.md
- Update CLIENT_AUTHORING_GUIDE.md if relevant
- Be clearly documented

Undocumented changes are not allowed.

------------------------------------------------------------

12. AI AND AUTOMATED CHANGES

AI-generated changes to the SDK MUST:
- Obey this policy
- Be reviewed against the contract
- Be rejected if ambiguous

AI convenience must NEVER override stability.

------------------------------------------------------------

13. CHANGE REVIEW CHECKLIST

Before accepting an SDK change, ask:

- Does any client test need to change?
- Does this expose time?
- Does this expose internals?
- Does this increase cognitive load?

If any answer is YES, reject the change.

------------------------------------------------------------

14. PHILOSOPHICAL RULE

The client SDK exists to protect users
from the complexity of reality.

Leaking that complexity defeats its purpose.

------------------------------------------------------------

15. FINAL POLICY STATEMENT

The client SDK is a promise:
- A promise of stability
- A promise of simplicity
- A promise of correctness

Breaking this promise breaks trust.

------------------------------------------------------------

END OF FILE
