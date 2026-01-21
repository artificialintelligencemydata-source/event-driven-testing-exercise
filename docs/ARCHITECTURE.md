# ARCHITECTURE.md  
## AUTWIT — Event-Driven Resumable Test Orchestration Architecture

---

## 1. PURPOSE OF THIS DOCUMENT

This document defines the **final, locked architecture** of AUTWIT.

It answers:
- What AUTWIT **is**
- How AUTWIT **is structured**
- How AUTWIT **executes tests**
- Where responsibilities **begin and end**

This document is **normative**.  
If code or design contradicts this document, the code is wrong.

---

## 2. WHAT AUTWIT IS (AND IS NOT)

### AUTWIT IS
- An **event-driven test orchestration platform**
- A **resumable execution engine**
- Designed for **eventual consistency**
- Built for **parallel execution**
- Independent of business domains

### AUTWIT IS NOT
- A traditional automation framework
- A synchronous test runner
- A polling / waiting framework
- A timeout-based validation tool
- A business-specific solution

---

## 3. CORE ARCHITECTURAL PRINCIPLE

> **Tests do not wait for events.  
> Tests pause, and the system resumes them when truth appears.**

This single rule drives the entire architecture.

---

## 4. HIGH-LEVEL SYSTEM VIEW

External System
│
▼
[ Adapter Layer ]
│
▼
[ Persistent Store ]
│
▼
[ Resume Engine ]
│
▼
[ Test Execution ]


**Key insight**:  
Tests never talk directly to Kafka, DB, or timers.

---

## 5. MODULE LAYOUT (FINAL)

autwit-root
│
├── autwit-core
│ ├── autwit-shared
│ ├── autwit-domain
│ ├── autwit-engine
│ ├── autwit-adapter-*
│ └── autwit-internal-testkit
│
├── autwit-client-sdk
│
├── autwit-runner
│
└── client-tests


This layout may be split into repos later,  
but **logical boundaries never change**.

---

## 6. MODULE RESPONSIBILITIES (STRICT)

### 6.1 autwit-core

**Purpose**
- Owns all architectural truth

**Contains**
- Domain models
- Ports (interfaces)
- Engine contracts
- Canonical identifiers

**Does NOT contain**
- Spring Boot
- TestNG / Cucumber
- Client logic
- Execution lifecycle

---

### 6.2 autwit-engine

**Purpose**
- Decides *when* a scenario pauses or resumes

**Responsibilities**
- Evaluate persisted event state
- Determine resume readiness
- Enforce idempotency
- Maintain correctness across restarts

**Rules**
- No blocking
- No waiting
- No timeouts
- No direct DB polling from tests

---

### 6.3 Adapter Modules (`autwit-adapter-*`)

**Purpose**
- Translate external systems into AUTWIT-understandable events

**Examples**
- Kafka adapter
- Mongo adapter
- Postgres adapter
- H2 adapter

**Responsibilities**
- Listen to external systems
- Persist events
- Normalize data

**Rules**
- Never resume tests
- Never contain business logic
- Never reference test frameworks

---

### 6.4 autwit-internal-testkit

**Purpose**
- Internal glue for execution lifecycle

**Contains**
- Test listeners
- Resume triggers
- Execution hooks

**Rules**
- Not visible to clients
- Not depended on directly
- Subject to internal refactoring

---

### 6.5 autwit-client-sdk (FACADE)

**Purpose**
- The **ONLY** API visible to client tests

**Responsibilities**
- Express intent:
  - `awaitEvent`
  - `expectState`
  - `scenario()`
- Hide all internals
- Remain stable across versions

**Rules**
- No Spring annotations required in client tests
- No timing APIs
- No engine exposure

---

### 6.6 autwit-runner

**Purpose**
- Execution shell only

**Responsibilities**
- Bootstrap Spring
- Configure TestNG / Cucumber
- Wire listeners
- Start execution

**Rules**
- No feature files
- No step definitions
- No assertions
- No business logic

---

### 6.7 client-tests

**Purpose**
- Where test intent lives

**Contains**
- Feature files
- Step definitions
- Scenario descriptions

**Rules**
- Depends ONLY on `autwit-client-sdk`
- No direct DB / Kafka / API clients
- No waits, sleeps, retries

---

## 7. EVENT FLOW (CANONICAL)

1. External system emits event
2. Adapter receives event
3. Event persisted to DB
4. ResumeEngine evaluates state
5. Scenario marked resume-ready
6. Scenario resumes execution
7. Assertions validate final state

**Important**:  
Steps never wait for step 2–4.

---

## 8. PAUSE / RESUME MECHANISM

### Pause
- Happens when required state is missing
- Implemented via `SkipException`
- Is intentional and expected

### Resume
- Triggered by data, not time
- Can happen minutes or hours later
- Survives JVM restarts
- Re-executes only remaining steps

---

## 9. PARALLELISM MODEL

- Each scenario has a **canonical key**
- All state is isolated per key
- ResumeEngine supports concurrent resumes
- No global locks
- No shared mutable state

---

## 10. FAILURE MODEL

| Situation | Result |
|---------|--------|
| Data missing | Scenario paused |
| Event delayed | Scenario paused |
| External system down | Scenario paused |
| Assertion fails | Test failure |
| Infrastructure error | Explicit failure |

Pauses are **not failures**.

---

## 11. WHY THIS ARCHITECTURE EXISTS

Traditional automation fails because it:
- Assumes immediacy
- Assumes determinism
- Assumes linear execution

AUTWIT assumes:
- Events arrive eventually
- Systems are async
- Truth lives in data

---

## 12. NON-NEGOTIABLE RULE

> **If a design introduces waiting, polling, or timing assumptions — it violates AUTWIT.**

---

## 13. STABILITY GUARANTEE

- Client SDK is stable
- Internal modules may evolve
- Architecture principles do not change

---

## 14. FINAL STATEMENT

AUTWIT is not optimized for speed.  
AUTWIT is optimized for **correctness under reality**.

If something feels harder in AUTWIT,
it is because reality is harder.

That is intentional.
