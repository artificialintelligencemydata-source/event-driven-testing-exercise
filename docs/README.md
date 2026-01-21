# AUTWIT Documentation

This directory contains all AUTWIT documentation, organized for easy reference.

**📋 Documentation Governance:** See [DOCUMENTATION_RULES.md](DOCUMENTATION_RULES.md) for rules on adding new documentation.

---

## Getting Started

- **[AUTWIT_OVERVIEW.md](AUTWIT_OVERVIEW.md)** - Start here! Overview of AUTWIT, core principles, and what problems it solves.
- **[CLIENT_GUIDE.md](CLIENT_GUIDE.md)** - How to write step definitions and feature files using AUTWIT.
- **[RUNNING_TESTS.md](RUNNING_TESTS.md)** - How to run AUTWIT tests, where files must be located, and troubleshooting.

---

## Core Documentation

### Architecture & Design

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Complete architecture documentation, module responsibilities, and system design.
- **[DESIGN_DECISIONS.md](DESIGN_DECISIONS.md)** - Key design decisions and rationale.
- **[EXECUTION_FLOW.md](EXECUTION_FLOW.md)** - How tests execute, pause/resume mechanism, and event matching.

### Technical Specifications

- **[SCENARIO_STATE_MODEL.md](SCENARIO_STATE_MODEL.md)** - Scenario state transitions and semantics.
- **[SCENARIO_KEY_MODEL.md](SCENARIO_KEY_MODEL.md)** - Canonical key structure and usage.
- **[SCENARIO_CONTEXT_LIFECYCLE.md](SCENARIO_CONTEXT_LIFECYCLE.md)** - Context creation, restoration, and cleanup.
- **[EVENT_MATCHING_RULES.md](EVENT_MATCHING_RULES.md)** - Event matching principles and rules.
- **[FAILURE_VS_PAUSE_SEMANTICS.md](FAILURE_VS_PAUSE_SEMANTICS.md)** - Difference between failure and pause.

### Engine & Resume

- **[RESUME_ENGINE_INTERNAL_FLOW.md](RESUME_ENGINE_INTERNAL_FLOW.md)** - How ResumeEngine works internally.
- **[RESUME_ENGINE_RULES.md](RESUME_ENGINE_RULES.md)** - Rules and constraints for ResumeEngine.
- **[RUNNER_EXECUTION_MODEL.md](RUNNER_EXECUTION_MODEL.md)** - How the runner executes tests.
- **[RUNNER_RESUME_COORDINATION.md](RUNNER_RESUME_COORDINATION.md)** - How runner and engine coordinate resume.
- **[RUNNER_VS_CLIENT_BOUNDARY.md](RUNNER_VS_CLIENT_BOUNDARY.md)** - Boundary between runner and client code.

### Client SDK

- **[CLIENT_AUTHORING_GUIDE.md](CLIENT_AUTHORING_GUIDE.md)** - Original client authoring guide (see CLIENT_GUIDE.md for updated version).
- **[SDK_API_CONTRACT.md](SDK_API_CONTRACT.md)** - SDK API contract and stability guarantees.

---

## Guardrails & Policies

- **[ARCHITECTURAL_GUARDRAILS.md](ARCHITECTURAL_GUARDRAILS.md)** - Architectural rules and constraints.
- **[DO_NOT_DO.md](DO_NOT_DO.md)** - Forbidden patterns and anti-patterns.
- **[CLIENT_SDK_CHANGE_POLICY.md](CLIENT_SDK_CHANGE_POLICY.md)** - Policy for changing the client SDK.

---

## Internal Documentation

- **[TEST_LIFECYCLE_FLOW.md](TEST_LIFECYCLE_FLOW.md)** - Complete test lifecycle from start to finish.
- **[AI_CONTRIBUTION_RULES.md](AI_CONTRIBUTION_RULES.md)** - Rules for AI contributions to AUTWIT.
- **[REPOSITORY_SPLIT_STRATEGY.md](REPOSITORY_SPLIT_STRATEGY.md)** - Strategy for splitting the repository.

---

## Audit Reports & Analysis

### Architectural Audits

- **[ARCHITECTURAL_AUDIT_REPORT.md](ARCHITECTURAL_AUDIT_REPORT.md)** - Comprehensive architectural alignment audit.
- **[ADAPTER_COMPLIANCE_AUDIT_REPORT.md](ADAPTER_COMPLIANCE_AUDIT_REPORT.md)** - Adapter compliance with multi-persistence rules.
- **[VIOLATION_CLASSIFICATION.md](VIOLATION_CLASSIFICATION.md)** - Classification of architectural violations.

### SDK & Client Analysis

- **[SDK_VISIBILITY_VIOLATIONS_ANALYSIS.md](SDK_VISIBILITY_VIOLATIONS_ANALYSIS.md)** - Analysis of SDK visibility violations.
- **[POST_REFACTOR_VERIFICATION_REPORT.md](POST_REFACTOR_VERIFICATION_REPORT.md)** - Verification after SDK refactor.
- **[CLIENT_TESTS_IMPORT_VIOLATIONS.md](CLIENT_TESTS_IMPORT_VIOLATIONS.md)** - Client test import violations.
- **[CLIENT_TEST_CLEANUP_SUMMARY.md](CLIENT_TEST_CLEANUP_SUMMARY.md)** - Summary of client test cleanup.

### Feature Analysis

- **[AUTWIT_FACADE_ARCHITECTURAL_MAPPING.md](AUTWIT_FACADE_ARCHITECTURAL_MAPPING.md)** - Mapping of client functionality to Autwit facade.
- **[ENGINE_MANAGED_STEP_RESUMPTION_ANALYSIS.md](ENGINE_MANAGED_STEP_RESUMPTION_ANALYSIS.md)** - Analysis of engine-managed step resumption.
- **[SKIP_IF_ALREADY_PASSED_AUDIT_REPORT.md](SKIP_IF_ALREADY_PASSED_AUDIT_REPORT.md)** - Audit of skip-if-already-passed logic.

---

## Documentation Governance

**📋 Before adding new documentation, read [DOCUMENTATION_RULES.md](DOCUMENTATION_RULES.md)**

- **[DOCUMENTATION_RULES.md](DOCUMENTATION_RULES.md)** - **MANDATORY** - Governance rules for all documentation
- **[DOCUMENTATION_AUDIT.md](DOCUMENTATION_AUDIT.md)** - Complete audit of all 38 documentation files
- **[DOCUMENTATION_ORGANIZATION_SUMMARY.md](DOCUMENTATION_ORGANIZATION_SUMMARY.md)** - Organization summary and actions taken
- **[FILE_ORGANIZATION_TABLE.md](FILE_ORGANIZATION_TABLE.md)** - Complete file organization table

---

## Documentation Status

### ✅ Canonical Files (Primary Sources)

- **AUTWIT_OVERVIEW.md** - What AUTWIT is (authoritative 60 points)
- **ARCHITECTURE.md** - Module responsibilities and boundaries
- **EXECUTION_FLOW.md** - Runtime behavior and pause/resume
- **CLIENT_GUIDE.md** - How clients write tests
- **RUNNING_TESTS.md** - How to execute tests
- **DESIGN_DECISIONS.md** - Design rationale

### 📋 Detailed Specifications (Complementary)

- Technical specifications (SCENARIO_*, EVENT_*, RESUME_*, etc.)
- These complement canonical files with detailed information

### 📋 Reference (Historical)

- Original authoritative guides (CLIENT_AUTHORING_GUIDE.md, etc.)
- Audit reports (historical snapshots)
- Analysis documents (specific to refactoring efforts)

---

## Quick Reference

**For Test Authors:**
1. Read [AUTWIT_OVERVIEW.md](AUTWIT_OVERVIEW.md)
2. Read [CLIENT_GUIDE.md](CLIENT_GUIDE.md)
3. Read [RUNNING_TESTS.md](RUNNING_TESTS.md)

**For Framework Developers:**
1. Read [ARCHITECTURE.md](ARCHITECTURE.md)
2. Read [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md)
3. Read [ARCHITECTURAL_GUARDRAILS.md](ARCHITECTURAL_GUARDRAILS.md)

**For Understanding Internals:**
1. Read [EXECUTION_FLOW.md](EXECUTION_FLOW.md)
2. Read [RESUME_ENGINE_INTERNAL_FLOW.md](RESUME_ENGINE_INTERNAL_FLOW.md)
3. Read [SCENARIO_STATE_MODEL.md](SCENARIO_STATE_MODEL.md)

---

## Notes

- All documentation is authoritative
- If code contradicts documentation, code is wrong
- Documentation is updated as architecture evolves
- Historical audit reports are preserved for reference

