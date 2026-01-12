# DO.md  
## AUTWIT — Mandatory Practices & Correct Usage

This document defines what **MUST be done** when building, extending, or using AUTWIT.  
All rules here are **non-optional** and enforce the event-driven, resumable test model.

---

## ✅ CORE AUTWIT PRINCIPLES (NON-NEGOTIABLE)

1. Tests MUST be **event-driven**, not time-driven.
2. Tests MUST be **resumable**, not retried.
3. Tests MUST be **non-blocking**.
4. AUTWIT MUST remain **business-agnostic**.
5. AUTWIT MUST own all orchestration logic.
6. Client code MUST express **intent only**, never mechanics.
7. Resume MUST be **data-driven**, not signal-driven.

---

## ✅ MODULE RESPONSIBILITIES (STRICT)

### Core (`autwit-core`)
8. Own domain models, ports, and engine contracts.
9. Define canonical event identity.
10. Define pause / resume semantics.
11. Remain free of test frameworks.
12. Remain free of client logic.
13. Never expose internal implementations.

### Engine (`autwit-engine`)
14. Own resume orchestration.
15. Decide when a scenario is paused.
16. Decide when a scenario is resume-ready.
17. Subscribe to event sources indirectly (via adapters).
18. Never block threads.
19. Never depend on wall-clock time.

### Adapters (`autwit-adapter-*`)
20. Translate external systems into AUTWIT events.
21. Persist events to durable storage.
22. Never resume scenarios directly.
23. Never contain business logic.

### Client SDK (`autwit-client-sdk`)
24. Be the **ONLY** client entry point.
25. Expose a minimal, stable facade.
26. Hide all engine and adapter details.
27. Express test intent declaratively.
28. Never expose timing APIs.

### Runner (`autwit-runner`)
29. Bootstrap execution only.
30. Configure Spring, TestNG, Cucumber.
31. Host execution lifecycle listeners.
32. Never contain feature files.
33. Never contain step definitions.
34. Never contain assertions.

### Client Tests (`client-tests`)
35. Contain feature files.
36. Contain step definitions.
37. Depend ONLY on `autwit-client-sdk`.
38. Express behavior, not implementation.

---

## ✅ EVENT FLOW (MANDATORY)

39. External system emits event.
40. Adapter receives event.
41. Event is persisted to DB.
42. Engine evaluates resume conditions from DB.
43. Scenario is marked resume-ready.
44. ResumeEngine triggers scenario continuation.
45. Test proceeds without re-execution.

**No shortcuts allowed.**

---

## ✅ PAUSE & RESUME MODEL

46. If required data is missing → **pause scenario**.
47. Paused scenarios MUST throw `SkipException`.
48. Skip is a **valid state**, not a failure.
49. Resume MUST occur only after data persistence.
50. Resume MUST be idempotent.
51. Resume MUST survive JVM restarts.

---

## ✅ STEP DEFINITION RULES

52. Step definitions MUST be thin.
53. Steps MUST call only client SDK APIs.
54. Steps MUST never block.
55. Steps MUST never wait.
56. Steps MUST never poll.
57. Steps MUST never assert time.
58. Steps MUST allow scenario pausing.
59. Steps MUST be parallel-safe.

---

## ✅ ASSERTION MODEL

60. Assertions MUST validate **state**, not timing.
61. Assertions MUST validate persisted outcomes.
62. Assertions MUST be deterministic.
63. Assertions MUST be replay-safe.
64. Assertions MUST not depend on execution order.

---

## ✅ PARALLEL EXECUTION

65. All scenarios MUST be parallel-safe.
66. Scenario state MUST be isolated by key.
67. No static mutable state in client tests.
68. Resume logic MUST support parallel resumption.

---

## ✅ LOGGING & OBSERVABILITY

69. Engine MUST log lifecycle transitions.
70. Resume decisions MUST be traceable.
71. Scenario keys MUST be logged consistently.
72. Logs MUST explain *why* a scenario paused or resumed.

---

## ✅ VERSIONING & STABILITY

73. Public client APIs MUST be stable.
74. Internal refactors MUST NOT break clients.
75. Backward compatibility is mandatory.
76. Breaking changes require explicit versioning.

---

## ✅ AI & AUTOMATION USAGE

77. AI MAY generate code **only within these rules**.
78. AI output MUST be reviewed against `DO_NOT_DO.md`.
79. AI MUST NOT invent new abstractions.
80. AI MUST NOT simplify architecture incorrectly.
81. AI MUST follow module boundaries strictly.

---

## ✅ FINAL RULE

82. **If a feature requires breaking these rules, the feature is invalid.**

AUTWIT exists to make tests:
- Deterministic  
- Resumable  
- Parallel  
- Event-driven  

Convenience is never a justification for violation.
