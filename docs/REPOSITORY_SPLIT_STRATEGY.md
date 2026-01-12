REPOSITORY_SPLIT_STRATEGY.md
AUTWIT — REPOSITORY SPLIT STRATEGY

This document defines HOW and WHY AUTWIT repositories
must be split, and what belongs in each repository.

This strategy is mandatory.
Deviation leads to coupling, client breakage, and maintenance debt.

------------------------------------------------------------

1. CORE PRINCIPLE

AUTWIT is NOT a single repo product.

AUTWIT is a PLATFORM.

The platform MUST be split to:
- Protect clients from internal churn
- Allow independent release cycles
- Enforce architectural boundaries
- Enable AI-assisted contribution safely

------------------------------------------------------------

2. FINAL TARGET REPOSITORIES

AUTWIT MUST be split into the following repositories:

1) autwit-core
2) autwit-client-sdk
3) autwit-runner
4) client-test-repos (many, external)

Each repository has a STRICT responsibility.

------------------------------------------------------------

3. autwit-core REPOSITORY

Purpose:
- Own all AUTWIT business-agnostic logic

Contains:
- Domain models
- Engine (ResumeEngine, orchestration)
- Ports (interfaces)
- Adapters (Mongo, Kafka, DBs)
- Internal testkit
- Internal event bus
- Resume logic
- State management

Does NOT contain:
- Feature files
- Step definitions
- Test runners
- Client-specific logic

Audience:
- AUTWIT framework maintainers
- NOT client teams

------------------------------------------------------------

4. autwit-client-sdk REPOSITORY

Purpose:
- Be the ONLY client-facing API

Contains:
- Facade APIs (Autwit, AwaitEvent, Scenario)
- Fluent DSLs
- Annotations (if any)
- DTOs meant for client use
- Zero execution logic

Does NOT contain:
- Engine classes
- Ports
- Adapters
- Resume logic
- Spring configuration
- TestNG / Cucumber configuration

Audience:
- Client test authors
- Client automation teams

------------------------------------------------------------

5. autwit-runner REPOSITORY

Purpose:
- Execution shell ONLY

Contains:
- TestNG runner
- Cucumber runner
- Spring Boot bootstrap
- Profiles and config loading
- Internal listeners
- Wiring of core + adapters

Does NOT contain:
- Feature files
- Step definitions
- Assertions
- Business logic
- Client intent

Audience:
- AUTWIT maintainers
- CI/CD systems

------------------------------------------------------------

6. client-test REPOSITORIES

Purpose:
- Express business intent

Each client gets:
- Its own repository
- Its own release cadence
- Its own test ownership

Contains:
- Feature files
- Step definitions
- Client data builders
- Domain-specific assertions

Depends ONLY on:
- autwit-client-sdk
- (optionally) autwit-runner as test dependency

Does NOT contain:
- Core logic
- Resume logic
- Execution wiring
- Adapters

------------------------------------------------------------

7. TEMPORARY MONOREPO (DEV MODE)

During development ONLY:
- A single aggregator repo MAY exist
- Modules mimic future repositories

Rules:
- Folder boundaries must match future repos
- No cross-module shortcuts
- No internal imports just because code is local

This mode is TEMPORARY and MUST be removable.

------------------------------------------------------------

8. MIGRATION STRATEGY (CURRENT STATE → FINAL STATE)

Phase 1:
- Single repo
- Multi-module Maven
- Enforced package boundaries

Phase 2:
- Extract autwit-client-sdk
- Publish snapshot artifacts

Phase 3:
- Extract autwit-runner
- Make runner reusable across clients

Phase 4:
- Client repos consume SDK + runner
- Core becomes invisible to clients

------------------------------------------------------------

9. VERSIONING RULES

autwit-core:
- Can change frequently
- Can introduce breaking changes internally

autwit-client-sdk:
- STRICT backward compatibility
- Semantic versioning required

autwit-runner:
- Compatible with SDK versions
- Minor updates preferred

Client repos:
- Control their own upgrade pace

------------------------------------------------------------

10. AI CONTRIBUTION IMPLICATIONS

AI tools (ChatGPT, Claude, etc.) MUST:
- Modify ONLY one repo at a time
- Respect repo responsibility
- Never introduce cross-repo leakage
- Never move code across boundaries silently

Each repo is an isolated contract.

------------------------------------------------------------

11. COMMON ANTI-PATTERNS (FORBIDDEN)

Forbidden:
- Putting features in runner
- Importing core classes in client tests
- Adding execution logic to SDK
- Sharing internal testkit with clients
- Making runner depend on client code

These break the platform model.

------------------------------------------------------------

12. ENFORCEMENT MECHANISMS

Enforce via:
- Maven module boundaries
- Package visibility
- Dependency rules
- Code reviews
- Documentation (this file)

Architecture is enforced, not trusted.

------------------------------------------------------------

13. FINAL RULE

If you cannot decide which repo code belongs to,
it probably belongs in autwit-core,
NOT in client code.

Never optimize for convenience.
Optimize for longevity.

------------------------------------------------------------

END OF FILE
