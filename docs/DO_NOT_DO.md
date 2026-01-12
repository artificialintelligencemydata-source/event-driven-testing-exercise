# DO_NOT_DO.md  
## AUTWIT — Non-Negotiable Violations

This document defines **hard prohibitions** in AUTWIT.  
Any violation here is considered an **architectural breach**, not a coding mistake.

---

## ❌ TIME & WAITING (ABSOLUTE BAN)

1. DO NOT use `Thread.sleep()` anywhere.
2. DO NOT use explicit waits, implicit waits, or polling loops.
3. DO NOT use time-based retries.
4. DO NOT use `.withinSeconds()`, `.withinMillis()`, or any time-bound APIs.
5. DO NOT add timeouts to step definitions.
6. DO NOT assume events arrive within any duration.
7. DO NOT design tests that depend on clock behavior.
8. DO NOT block threads waiting for events.
9. DO NOT delay execution to "let the system catch up".
10. DO NOT add sleeps to fix flaky tests.

---

## 🚫 NEVER ALLOWED (NO EXCEPTIONS)

The following are ABSOLUTELY FORBIDDEN with NO exceptions,
regardless of development phase, temporary lenience, or
stabilization needs:

- `Thread.sleep()` — NEVER allowed, no exceptions
- `CompletableFuture.get(timeout, TimeUnit)` — NEVER allowed, no exceptions
- `Awaitility` or any await/timeout libraries — NEVER allowed, no exceptions
- `withinSeconds()`, `withinMillis()`, or any time-bound APIs — NEVER allowed, no exceptions
- Polling (while loops, scheduled checks, DB queries in loops) — NEVER allowed, no exceptions
- Retry-for-time logic (retry until timeout, retry N times with delay) — NEVER allowed, no exceptions

These violate the fundamental event-driven model of AUTWIT.
Time-based logic is NEVER acceptable, even during temporary development lenience.

---

## ❌ CLIENT TEST VIOLATIONS

11. DO NOT access Kafka directly from step definitions.
12. DO NOT poll Kafka from tests.
13. DO NOT consume Kafka topics in client code.
14. DO NOT access MongoDB, PostgreSQL, H2, or any DB directly from steps.
15. DO NOT query repositories from step definitions.
16. DO NOT call engine ports from client code.
17. DO NOT import `autwit-core` in client tests.
18. DO NOT import `autwit-engine` in client tests.
19. DO NOT import `autwit-internal-testkit` in client tests.
20. DO NOT bypass `autwit-client-sdk`.
21. DO NOT instantiate engine components manually.
22. DO NOT create Spring beans in client test modules.

---

## ❌ ARCHITECTURAL LEAKS

23. DO NOT expose engine internals to clients.
24. DO NOT let client code know how resume works.
25. DO NOT let client code know when resume happens.
26. DO NOT leak database schema knowledge to tests.
27. DO NOT let tests reason about event buffering.
28. DO NOT let tests coordinate resume logic.
29. DO NOT allow tests to manage execution state.
30. DO NOT add “helper shortcuts” that bypass the engine.

---

## ❌ RUNNER MISUSE

31. DO NOT place feature files in the runner.
32. DO NOT place step definitions in the runner.
33. DO NOT write test logic in the runner.
34. DO NOT add business logic to the runner.
35. DO NOT treat the runner as a test framework.
36. DO NOT modify runner behavior for client convenience.

---

## ❌ TEST EXECUTION ANTI-PATTERNS

37. DO NOT fail tests when data is not yet available.
38. DO NOT retry failed scenarios immediately.
39. DO NOT re-run the entire suite to recover one scenario.
40. DO NOT mark paused scenarios as failures.
41. DO NOT depend on test execution order.
42. DO NOT assume scenarios complete in one run.

---

## ❌ EVENT MISUSE

43. DO NOT validate business payload meaning in the engine.
44. DO NOT parse event payloads inside core logic.
45. DO NOT assume event ordering without DB confirmation.
46. DO NOT trigger resume directly from Kafka listeners.
47. DO NOT resume tests from in-memory signals alone.

---

## ❌ DESIGN & DISCIPLINE VIOLATIONS

48. DO NOT weaken boundaries for convenience.
49. DO NOT introduce flexibility at the cost of correctness.
50. DO NOT add APIs “just for ease of use”.
51. DO NOT change public APIs casually.
52. DO NOT break client compatibility due to engine refactors.
53. DO NOT introduce DSL complexity.
54. DO NOT over-abstract client APIs.
55. DO NOT allow inconsistent patterns across modules.

---

## ❌ AI & AUTOMATION RULES

56. DO NOT allow AI to refactor architecture without rules.
57. DO NOT accept AI-generated code that violates this file.
58. DO NOT let AI introduce waits, retries, or polling.
59. DO NOT let AI collapse module boundaries.
60. DO NOT merge AI changes without rule validation.

---

## 🚨 ENFORCEMENT

- Any violation here is a **design error**, not a test bug.
- PRs violating this document **must be rejected**.
- This document overrides convenience, speed, and legacy habits.

AUTWIT correctness > everything else.
