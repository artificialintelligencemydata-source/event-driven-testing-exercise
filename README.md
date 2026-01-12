# Autwit – Event-Driven Testing Framework

## 1. Purpose
This framework provides a pure event-driven testing model where test execution
never relies on sleeps, waits, or timeouts.

Tests pause and resume based on real system events.

## 2. Core Principles (NON-NEGOTIABLE)
### 2.1 Event-Driven Only (No Exceptions)
This framework is purely event-driven
Test progression depends only on real system events
Time-based logic is forbidden

❌ **Forbidden:**
Thread.sleep
Explicit waits
Polling loops in test steps
Retry logic inside steps

✅ **Allowed:**
Event notifications
Scheduler-driven resumption
Database state transitions

### 2.2 Tests Must Pause, Never Block
A test step may pause execution
A test step must never block a thread
Paused tests must allow other scenarios to continue
This enables:
Parallel execution
High throughput
Deterministic behavior

### 2.3 Database Is the Single Source of Truth
Kafka is transient
Database is authoritative
Test decisions are made only from persisted state
Event flow is fixed:
Kafka → Listener → Database → Scheduler → ResumeEngine → Test Step

❌ **Test steps must never:**
Listen to Kafka directly
Query Kafka offsets
Depend on message timing

### 2.4 Clear Separation of Responsibilities
#### Core Framework (autwit-core)
**Owns**
Event matching
Scheduling
Resume logic
State transitions
Must be client-agnostic
Must not contain client logic

#### Runner / Client Layer (autwit-runner)
**Owns**
Feature files
Step definition
Client configuration
Must never:
Access Kafka
Access DB adapters directly
Implement scheduling logic

### 2.5 Adapters Are Pluggable, Not Leaky
Database support is via adapters only
Supported adapters:
MongoDB
PostgreSQL
H2
Core logic must not:
Know DB-specific APIs
Contain DB-specific conditionals

### 2.6 No Secrets in Git — Ever
No API tokens
No passwords
No credentials
No connection secrets

Rules:
Git contains templates only
Secrets live in:
Environment variables
Local ignored files
Push protection violations are treated as fatal errors

### 2.7 Backward Safety Over Convenience
Stability > speed
Determinism > shortcuts
Explicit design > implicit behavior
Any optimization that:
Breaks determinism
Introduces timing dependency
Adds hidden coupling
❌ is rejected.

### 2.8 README Is the Contract
README.md is the single source of truth
Any architectural change must update README first
AI-generated changes must comply with README
If code and README disagree → README wins.

## 3. High-Level Architecture
- Core framework is isolated from client runners
- Clients only depend on public APIs / annotations
- Internal logic is fully encapsulated

## 4. Module Structure
- autwit-core: core event-driven engine
- autwit-runner: client-facing execution layer
- adapters: database-specific implementations (Mongo, Postgres, H2)

## 5. Authoritative Event Flow
Kafka → Listener → Persistence → Scheduler → ResumeEngine → Test Step

## 6. Client Responsibilities
- Write scenarios and step definitions only
- Never access Kafka, DB, or schedulers directly
- Never introduce waits or retries

## 7. Configuration Rules
- No secrets committed to Git
- Local configuration via ignored files or env variables only

## 8. AI Collaboration Rules
- README.md is the contract
- No architectural changes without updating README
- No refactors that violate core principles

## 9. Future Scope
- Annotation-based abstraction (`@Autwit`)
- ResumeEngine enhancements
- Client SDK simplification

## 10. Client Entry Abstraction – `@Autwit`

All client test runners must use the `@Autwit` annotation as the single entry
point into the framework.

### Purpose
- Hide all internal framework wiring
- Enforce event-driven execution by construction
- Prevent clients from interacting with Kafka, databases, schedulers, or resume logic

### Client Usage Contract
Client code must be limited to:

```java
@Autwit
public class ClientTestRunner {
}

