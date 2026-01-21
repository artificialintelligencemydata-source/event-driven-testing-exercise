# AUTWIT Overview

**AUTWIT** is an event-driven, resumable test orchestration platform designed for asynchronous and eventually consistent systems.

---

## What AUTWIT Is

AUTWIT is:
- An **event-driven test orchestration platform**
- A **resumable execution engine**
- Designed for **eventual consistency**
- Built for **parallel execution**
- Independent of business domains

## What AUTWIT Is Not

AUTWIT is NOT:
- A traditional automation framework
- A synchronous test runner
- A polling / waiting framework
- A timeout-based validation tool
- A business-specific solution

---

## Core Principles

### 1. Event-Driven Only (No Exceptions)

AUTWIT is purely event-driven. Test progression depends only on real system events. Time-based logic is **forbidden**.

**❌ Forbidden:**
- `Thread.sleep()`
- Explicit waits
- Polling loops in test steps
- Retry logic inside steps
- `CompletableFuture.get(timeout, TimeUnit)`
- `Awaitility`
- `withinSeconds()`

**✅ Allowed:**
- Event notifications
- Scheduler-driven resumption
- Database state transitions

### 2. Tests Must Pause, Never Block

A test step may **pause** execution. A test step must **never block** a thread. Paused tests must allow other scenarios to continue.

This enables:
- Parallel execution
- High throughput
- Deterministic behavior

### 3. Database Is the Single Source of Truth

- Kafka is transient
- Database is authoritative
- Test decisions are made only from persisted state

**Event flow is fixed:**
```
Kafka → Listener → Database → ResumeEngine → Test Step
```

**❌ Test steps must never:**
- Listen to Kafka directly
- Query Kafka offsets
- Depend on message timing

### 4. Clear Separation of Responsibilities

**Core Framework (autwit-core):**
- Owns event matching, scheduling, resume logic, state transitions
- Must be client-agnostic
- Must not contain client logic

**Runner / Client Layer (autwit-runner):**
- Owns feature files, step definitions, client configuration
- Must never access Kafka, DB adapters directly, or implement scheduling logic

### 5. No Secrets in Git — Ever

- No API tokens, passwords, credentials, or connection secrets
- Git contains templates only
- Secrets live in environment variables or local ignored files

---

## Core Architectural Principle

> **Tests do not wait for events.  
> Tests pause, and the system resumes them when truth appears.**

This single rule drives the entire architecture.

---

## What Problems AUTWIT Solves

### Problem 1: Flaky Tests in Async Systems

**Traditional approach:**
- Tests wait with timeouts
- Tests fail when systems are slow
- Tests become flaky and unreliable

**AUTWIT solution:**
- Tests pause when data is unavailable
- Tests resume when data arrives
- No time-based failures

### Problem 2: Long-Running Workflows

**Traditional approach:**
- Tests must complete in one execution
- Long workflows require complex retry logic
- State is lost on failure

**AUTWIT solution:**
- Tests can pause and resume across JVM restarts
- State is persisted and restored
- Workflows can span hours or days

### Problem 3: Eventual Consistency

**Traditional approach:**
- Tests assume immediate consistency
- Tests fail when events arrive out of order
- Tests require complex synchronization

**AUTWIT solution:**
- Tests wait for events to appear in database
- Out-of-order events are handled automatically
- Tests are deterministic regardless of timing

### Problem 4: Parallel Execution Complexity

**Traditional approach:**
- Shared state causes race conditions
- Tests interfere with each other
- Parallel execution is unreliable

**AUTWIT solution:**
- Each scenario has isolated state
- Canonical keys prevent collisions
- Parallel execution is safe by design

---

## Key Concepts

### Pause vs. Failure

**Pause:**
- Non-terminal execution state
- Indicates system state not yet ready
- Scenario can resume when data arrives
- Implemented via `SkipException`

**Failure:**
- Terminal test outcome
- Indicates incorrect system behavior
- Scenario cannot resume
- Implemented via assertion errors

### Event Matching

Events are matched based on:
- **Canonical Key** (e.g., `orderId + eventType`)
- **Event Type** (e.g., `"ORDER_CREATED"`)

Events are **NOT** matched based on:
- Payload fields
- Timestamps
- Time windows

### Resume Mechanism

Scenarios resume when:
- All expected events exist in database
- ResumeEngine marks scenario as `RESUME_READY`
- Runner schedules scenario for re-execution

Resume is **state-driven**, not time-driven.

---

## Module Structure

```
autwit-root
│
├── autwit-core
│   ├── autwit-shared
│   ├── autwit-domain
│   ├── autwit-engine
│   ├── autwit-adapter-* (Mongo, Postgres, H2, Kafka)
│   └── autwit-internal-testkit
│
├── autwit-client-sdk
│
├── autwit-runner
│
└── client-tests
```

---

## Stability Guarantee

- **Client SDK is stable** — client tests do not break when engine internals change
- **Internal modules may evolve** — engine, adapters, and testkit can be refactored
- **Architecture principles do not change** — core principles remain constant

---

## Final Statement

AUTWIT is not optimized for speed.  
AUTWIT is optimized for **correctness under reality**.

If something feels harder in AUTWIT, it is because reality is harder. That is intentional.
