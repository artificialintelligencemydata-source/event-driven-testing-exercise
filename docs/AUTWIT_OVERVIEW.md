## AUTWIT — INDIVIDUAL POINTS (AUTHORITATIVE)

1. AUTWIT is not a traditional automation framework.  
2. AUTWIT is an event-driven test orchestration platform.  
3. AUTWIT is designed for asynchronous and eventually consistent systems.  
4. AUTWIT assumes Kafka / database–driven workflows.  
5. AUTWIT treats tests as long-running workflows, not short executions.  
6. AUTWIT never relies on time.  
7. AUTWIT forbids Thread.sleep, waits, polling, and client-side timeouts.  
8. AUTWIT forbids retry-based validation.  
9. AUTWIT uses pause and resume instead of retry.  
10. AUTWIT treats the database as the single source of truth.  
11. AUTWIT treats in-memory state as an optimization only.  
12. AUTWIT requires tests to express intent, not mechanics.  
13. AUTWIT forbids client code from reasoning about Kafka, databases, or resume logic.  
14. AUTWIT forbids client code from touching engine ports.  
15. AUTWIT forbids client code from knowing how resume works.  
16. AUTWIT forbids client code from knowing when resume happens.  
17. AUTWIT forbids time parameters in any public API.  
18. AUTWIT requires all client interaction to happen through autwit-client-sdk only.  
19. AUTWIT exposes a single facade to client tests.  
20. AUTWIT client tests must import only the SDK and nothing else.  
21. AUTWIT client tests must never import internal-testkit, runner, or core modules.  
22. AUTWIT step definitions must never access database repositories directly.  
23. AUTWIT step definitions must never poll Kafka or message brokers.  
24. AUTWIT step definitions must never block execution.  
25. AUTWIT uses SkipException to pause scenarios.  
26. A paused scenario is not a failure in AUTWIT.  
27. AUTWIT allows other scenarios to continue when one scenario is paused.  
28. AUTWIT resumes scenarios only when database state confirms readiness.  
29. AUTWIT never wakes tests directly from Kafka listeners or adapters.  
30. AUTWIT uses a ResumeEngine to re-execute paused scenarios.  
31. AUTWIT ResumeEngine operates completely outside test code.  
32. AUTWIT ResumeEngine scans the database for resume-ready scenarios.  
33. AUTWIT ResumeEngine triggers re-execution via TestNG, not direct callbacks.  
34. AUTWIT engine owns all state and orchestration logic.  
35. AUTWIT engine internals must never leak to client code.  
36. AUTWIT runner is bootstrap-only infrastructure.  
37. AUTWIT runner owns Spring Boot startup and the test execution shell.  
38. AUTWIT runner must not own feature files.  
39. AUTWIT runner must not own step definitions.  
40. AUTWIT client-tests module owns all feature files and step definitions.  
41. AUTWIT internal-testkit owns hooks, listeners, and lifecycle glue.  
42. AUTWIT internal-testkit is not a client-facing API.  
43. AUTWIT core is business-agnostic.  
44. AUTWIT core must remain independent of client logic.  
45. AUTWIT treats event payloads as opaque.  
46. AUTWIT does not parse or validate payload structure at the engine level.  
47. AUTWIT validates event presence and sequencing, not business payload meaning.  
48. AUTWIT supports parallel scenario execution by design.  
49. AUTWIT prioritizes correctness over execution speed.  
50. AUTWIT enforces strict layer boundaries.  
51. AUTWIT architecture must not be weakened for convenience.  
52. AUTWIT design must remain stable even if internals evolve.  
53. AUTWIT client tests must not break when engine internals change.  
54. AUTWIT enables resumable testing without rerunning the full test suite.  
55. AUTWIT is designed for enterprise-scale distributed systems.  
56. AUTWIT is not a DSL-heavy testing framework.  
57. AUTWIT prefers minimal, intention-revealing APIs.  
58. AUTWIT enforces discipline over flexibility.  
59. AUTWIT assumes both humans and AI will contribute to the repository.  
60. Any AI interacting with AUTWIT must obey all the above rules.
