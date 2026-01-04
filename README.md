# Autwit – Event-Driven Testing Framework

## 1. Purpose
This framework provides a pure event-driven testing model where test execution
never relies on sleeps, waits, or timeouts.

Tests pause and resume based on real system events.

## 2. Core Principles (NON-NEGOTIABLE)
- No Thread.sleep(), waits, or polling inside test steps
- Kafka → Database → Scheduler → ResumeEngine is the only flow
- Tests pause, never block
- Database is the single source of truth
- Framework must support parallel execution safely

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
