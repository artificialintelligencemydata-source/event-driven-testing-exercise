RUNNER_VS_CLIENT_BOUNDARY.md
AUTWIT — RUNNER VS CLIENT BOUNDARY

This document defines the STRICT boundary between
the AUTWIT runner and client test code.

This boundary is NON-NEGOTIABLE.
If code crosses it, the architecture is broken.

------------------------------------------------------------

1. PURPOSE OF THIS BOUNDARY

This boundary exists to:
- Separate execution mechanics from test intent
- Protect client tests from framework complexity
- Allow engine and runner evolution without client impact

Runner and Client serve different purposes.
They must NEVER blend.

------------------------------------------------------------

2. WHAT THE RUNNER IS

The runner is an EXECUTION SHELL.

The runner is responsible for:
- Bootstrapping Spring
- Wiring profiles and properties
- Configuring TestNG / Cucumber
- Registering listeners and hooks
- Starting and stopping execution

The runner owns HOW tests run.

------------------------------------------------------------

3. WHAT THE RUNNER IS NOT

The runner MUST NOT:
- Contain feature files
- Contain step definitions
- Contain business assertions
- Contain test intent
- Expose SDK-like APIs

The runner MUST NOT know WHAT is being tested.

------------------------------------------------------------

4. WHAT CLIENT TESTS ARE

Client tests express INTENT.

Client tests are responsible for:
- Writing feature files
- Writing step definitions
- Declaring expectations
- Triggering business actions

Client tests own WHAT is being tested.

------------------------------------------------------------

5. WHAT CLIENT TESTS MUST NOT DO

Client tests MUST NOT:
- Bootstrap Spring
- Configure TestNG or Cucumber
- Register listeners
- Manage lifecycle hooks
- Know about ResumeEngine

Execution mechanics are not client concerns.

------------------------------------------------------------

6. ALLOWED DEPENDENCY DIRECTION

The ONLY allowed dependency flow:

Client Tests
  -> autwit-client-sdk
      -> autwit-engine
          -> Ports
              -> Adapters

Runner sits BESIDE client tests,
not above or inside them.

------------------------------------------------------------

7. SDK IS THE ONLY BRIDGE

The client SDK is the ONLY allowed bridge
between client tests and AUTWIT internals.

Client tests MUST:
- Depend only on the SDK
- Never import runner classes
- Never import internal testkit classes

------------------------------------------------------------

7A. TEMPORARY LENIENCY — CLIENT DIRECT CORE PORT ACCESS

⚠️ TECHNICAL DEBT ⚠️

During the current stabilization phase, AUTWIT temporarily allows
client step definitions to import AUTWIT core ports directly.

CURRENT BEHAVIOR (TEMPORARY):

Client step definitions currently import AUTWIT core ports directly:
- EventContextPort
- EventMatcherPort
- ScenarioStatePort

This is a TEMPORARY and EXPLICITLY APPROVED leniency.

WHY THIS EXISTS:

This leniency exists only to:
- Accelerate early framework development
- Validate engine behavior before final SDK shape
- Enable rapid iteration during core stabilization

This leniency MUST NOT:
- Spread to new client modules
- Be copied into reference examples
- Be considered stable architecture
- Be used as justification for additional boundary violations

AUTHORITY PRESERVATION:

The canonical boundary rule remains unchanged:
Client tests MUST depend ONLY on autwit-client-sdk.

This leniency is a temporary scaffolding measure that bypasses
the intended architecture. It does NOT change the final requirement.

FUTURE REQUIREMENT:

All client code MUST depend ONLY on autwit-client-sdk.

Client code MUST NOT import:
- Core ports (EventContextPort, EventMatcherPort, ScenarioStatePort, etc.)
- Engine classes (ResumeEngine, EventStepNotifier, etc.)
- Adapters (MongoEventContextAdapter, PostgresEventContextAdapter, etc.)
- Internal testkit classes
- Any autwit-core or autwit-engine packages

All interactions will move behind SDK APIs (Autwit facade).

The SDK will provide all necessary functionality through:
- Autwit.expectEvent()
- Autwit.scenario()
- Other SDK facade methods

FINAL RULE:

AUTWIT is NOT considered stable until this leniency is removed.

Any new client code MUST follow the SDK-only rule:
- Import only from autwit-client-sdk
- Use only SDK facade APIs
- Do NOT import core ports or engine classes

This leniency applies ONLY to existing code during stabilization.
New client code MUST NOT follow this pattern.

------------------------------------------------------------

8. INTERNAL TESTKIT POSITION

The internal testkit:
- Is owned by AUTWIT
- Is used by the runner
- Is invisible to clients

Client tests MUST NOT:
- Import internal testkit
- Rely on hooks
- Reference listeners

------------------------------------------------------------

9. FEATURE FILE OWNERSHIP

Feature files MUST:
- Live in client test repositories
- Describe business behavior
- Remain framework-agnostic

Feature files MUST NOT:
- Live in the runner
- Contain execution hints
- Contain timing assumptions

------------------------------------------------------------

10. STEP DEFINITION OWNERSHIP

Step definitions MUST:
- Live in client test code
- Import only the SDK
- Remain declarative

Step definitions MUST NOT:
- Control execution flow
- Handle resume logic
- Catch SkipException
- Use waits or retries

------------------------------------------------------------

11. EXECUTION FLOW (HIGH LEVEL)

Runner:
- Starts the environment
- Triggers test execution
- Hands control to test framework

Client tests:
- Execute steps
- Declare expectations
- Exit when paused

Runner:
- Handles resume and re-execution
- Without client involvement

------------------------------------------------------------

12. WHY THIS BOUNDARY MATTERS

Without this boundary:
- Clients become framework experts
- Tests become brittle
- Engine changes break tests
- Resume logic leaks everywhere

This boundary keeps AUTWIT sustainable.

------------------------------------------------------------

13. COMMON VIOLATIONS (REJECT IMMEDIATELY)

Reject any change that:
- Moves hooks into client code
- Moves step definitions into runner
- Injects ports into steps
- Adds execution logic to features

These are architectural failures.

------------------------------------------------------------

14. CHANGE POLICY

Any change affecting this boundary MUST:
- Be explicitly documented
- Be reviewed architecturally
- Preserve client simplicity

Silent boundary erosion is forbidden.

------------------------------------------------------------

15. FINAL RULE

Runner owns EXECUTION.
Client owns INTENT.

If you cannot clearly say which side code belongs to,
it probably does not belong anywhere.

------------------------------------------------------------

END OF FILE
