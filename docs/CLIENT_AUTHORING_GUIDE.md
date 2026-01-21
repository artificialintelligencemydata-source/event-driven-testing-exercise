CLIENT_AUTHORING_GUIDE.md
Writing Tests with AUTWIT (Client Perspective)

This guide is AUTHORITATIVE for anyone writing feature files
or step definitions using AUTWIT.

If something here contradicts code, the code is wrong.

------------------------------------------------------------

1. WHO THIS GUIDE IS FOR

This guide is for:
- QA engineers
- Test authors
- Automation developers
- Anyone writing feature files or step definitions

This guide is NOT for:
- Engine developers
- Framework maintainers
- Adapter authors

If you are touching:
- Kafka
- Databases
- Ports
- Resume logic
then you are in the wrong place.

------------------------------------------------------------

⚠️ WARNING: TEMPORARY DEVELOPMENT LENIENCE ⚠️

Some existing tests in the AUTWIT codebase may access internal ports
(EventContextPort, EventMatcherPort, ScenarioStatePort) due to
temporary development lenience during core stabilization.

NEW TESTS MUST NOT follow this pattern.

Even during lenience:
- Tests MUST still be event-driven
- Tests MUST pause via SkipException when data is unavailable
- Time-based logic (timeouts, sleeps, polling, retries) is INVALID
- Waiting for Kafka/DB is FORBIDDEN

This lenience is temporary and will be removed before:
- SDK hardening
- External adoption
- v1.0 release

If you are writing new tests, use ONLY the client SDK APIs.

------------------------------------------------------------

2. CORE MINDSET (READ FIRST)

AUTWIT tests are NOT scripts.

They are declarations of intent.

You do NOT:
- Wait for things
- Poll for things
- Retry things
- Reason about time

You ONLY:
- Trigger actions
- Declare expectations

AUTWIT decides WHEN expectations are satisfied.

------------------------------------------------------------

3. WHAT CLIENT TESTS ARE ALLOWED TO DO

Client tests MAY:
- Call business APIs
- Place orders
- Trigger workflows
- Declare expected events
- Validate final outcomes

Client tests MUST NOT:
- Access databases
- Access Kafka
- Use timeouts
- Use sleeps
- Use retries
- Reason about pause or resume

------------------------------------------------------------

4. WHAT YOU CAN IMPORT (STRICT)

Allowed imports:

import com.acuver.autwit.client.Autwit
import com.acuver.autwit.client.EventExpectation

That is ALL.

Forbidden imports:
- Any ports package
- Any engine package
- Any adapter package
- Any internal package
- Any DB client
- Any Kafka client

If you need any forbidden import,
the SDK is incomplete — NOT your test.

------------------------------------------------------------

5. BASIC STEP DEFINITION PATTERN

Example pattern:

@Autowired
Autwit autwit;

@Then("order created event should arrive")
public void verifyOrderCreated() {
    autwit.expectEvent(orderId, "ORDER_CREATED")
          .assertSatisfied();
}

Rules:
- No time
- No waits
- No retries
- No exception handling

If the event does not exist:
- AUTWIT pauses the scenario automatically

------------------------------------------------------------

6. FEATURE FILE GUIDELINES

GOOD example:

Scenario: Order creation lifecycle
  Given I place an order
  Then ORDER_CREATED event should arrive

BAD examples:

Then ORDER_CREATED event should arrive within 10 seconds
Then wait until ORDER_CREATED appears
Then retry until event arrives

Time must NEVER appear in feature files.

------------------------------------------------------------

7. PAUSE & RESUME (CLIENT MENTAL MODEL)

As a client author:
- You do NOT pause tests
- You do NOT resume tests

You simply:
- Declare expectations

Internally:
- Missing data → scenario pauses
- Data arrives later → scenario resumes
- Resume happens automatically

You do NOTHING special.

------------------------------------------------------------

8. FAILURE BEHAVIOR

Event missing:
- Scenario pauses (Skipped)

Event arrives later:
- Scenario resumes automatically

Assertion incorrect:
- Test fails

Code or infrastructure error:
- Test fails

Skipped is NOT a failure.

Skipped means:
Reality has not happened yet.

------------------------------------------------------------

9. WHAT NOT TO FIX IN CLIENT TESTS

If you observe:
- Frequent skips
- Delayed resumes
- Out-of-order events

Do NOT:
- Add waits
- Add retries
- Add sleeps

Instead:
- Fix the system
- Fix adapters
- Fix event production

------------------------------------------------------------

10. FORBIDDEN ANTI-PATTERNS

The following are NEVER allowed:

Thread.sleep(...)
Awaitility.await(...)
withinSeconds(...)
retry(...)
poll(...)

Using these means you are fighting AUTWIT.

------------------------------------------------------------

11. SCENARIO ISOLATION RULE

Each scenario:
- Must have a unique business key (orderId, caseId, etc.)
- Must not share mutable state
- Must not depend on execution order

Parallel execution is the default.

------------------------------------------------------------

12. DEBUGGING GUIDELINE

When a scenario pauses:

1. Check external system logs
2. Check adapter logs
3. Check persisted events
4. DO NOT modify test code

Client tests are the LAST thing to change.

------------------------------------------------------------

13. VERSIONING GUARANTEE

- autwit-client-sdk is stable
- Engine changes must NOT break client tests
- SDK evolves so client tests do not have to

------------------------------------------------------------

14. GOLDEN RULE

If your test needs to understand HOW AUTWIT works,
then AUTWIT has failed — not your test.

------------------------------------------------------------

15. FINAL CHECKLIST BEFORE COMMIT

- No waits
- No time
- No DB access
- No Kafka access
- Only SDK imports
- Clear intent
- Scenario independence

If all are true, the test is correct.

------------------------------------------------------------

END OF FILE
