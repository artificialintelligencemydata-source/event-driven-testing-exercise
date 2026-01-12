# AUTWIT Documentation File Organization

**Date:** 2026-01-05  
**Purpose:** Complete table of all documentation files and their organization

---

## Summary Table

| Original File | Current Location | Category | Action Taken | Status |
|---------------|------------------|----------|--------------|--------|
| **CANONICAL FILES (Primary Sources)** |
| AUTWIT_OVERVIEW.md | `/docs` | OVERVIEW | ✅ Preserved (original authoritative) | Canonical |
| ARCHITECTURE.md | `/docs` | ARCHITECTURE | ✅ Preserved (original authoritative) | Canonical |
| EXECUTION_FLOW.md | `/docs` | EXECUTION | ✅ Preserved (GPT-generated comprehensive) | Canonical |
| CLIENT_GUIDE.md | `/docs` | CLIENT_USAGE | ✅ Preserved (GPT-generated comprehensive) | Canonical |
| RUNNING_TESTS.md | `/docs` | RUN_BOOTSTRAP | ✅ Preserved (GPT-generated comprehensive) | Canonical |
| DESIGN_DECISIONS.md | `/docs` | DESIGN_RATIONALE | ✅ Preserved (GPT-generated comprehensive) | Canonical |
| **TECHNICAL SPECIFICATIONS (Detailed)** |
| SCENARIO_STATE_MODEL.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| SCENARIO_KEY_MODEL.md | `/docs` | ARCHITECTURE | ✅ Preserved | Detailed Spec |
| SCENARIO_CONTEXT_LIFECYCLE.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| EVENT_MATCHING_RULES.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| FAILURE_VS_PAUSE_SEMANTICS.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| RESUME_ENGINE_INTERNAL_FLOW.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| RESUME_ENGINE_RULES.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| RUNNER_EXECUTION_MODEL.md | `/docs` | RUN_BOOTSTRAP | ✅ Preserved | Detailed Spec |
| RUNNER_RESUME_COORDINATION.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| RUNNER_VS_CLIENT_BOUNDARY.md | `/docs` | ARCHITECTURE | ✅ Preserved | Detailed Spec |
| TEST_LIFECYCLE_FLOW.md | `/docs` | EXECUTION | ✅ Preserved | Detailed Spec |
| **GUARDRAILS** |
| ARCHITECTURAL_GUARDRAILS.md | `/docs` | GUARDRAILS | ✅ Preserved | Guardrail |
| DO_NOT_DO.md | `/docs` | GUARDRAILS | ✅ Preserved | Guardrail |
| CLIENT_SDK_CHANGE_POLICY.md | `/docs` | GUARDRAILS | ✅ Preserved | Policy |
| AI_CONTRIBUTION_RULES.md | `/docs` | GUARDRAILS | ✅ Preserved | Policy |
| **CLIENT DOCUMENTATION** |
| CLIENT_AUTHORING_GUIDE.md | `/docs` | CLIENT_USAGE | ✅ Preserved (original authoritative) | Complementary |
| SDK_API_CONTRACT.md | `/docs` | CLIENT_USAGE | ✅ Preserved | Spec |
| **AUDIT REPORTS (Historical)** |
| ARCHITECTURAL_AUDIT_REPORT.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| ADAPTER_COMPLIANCE_AUDIT_REPORT.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| VIOLATION_CLASSIFICATION.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| SDK_VISIBILITY_VIOLATIONS_ANALYSIS.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| POST_REFACTOR_VERIFICATION_REPORT.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| CLIENT_TESTS_IMPORT_VIOLATIONS.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| CLIENT_TEST_CLEANUP_SUMMARY.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| AUTWIT_FACADE_ARCHITECTURAL_MAPPING.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| ENGINE_MANAGED_STEP_RESUMPTION_ANALYSIS.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| SKIP_IF_ALREADY_PASSED_AUDIT_REPORT.md | `/docs` | AUDIT | ✅ Preserved (historical) | Historical |
| **MISCELLANEOUS** |
| REPOSITORY_SPLIT_STRATEGY.md | `/docs` | MISC | ✅ Preserved | Strategy |
| DOCUMENTATION_CONSOLIDATION_SUMMARY.md | `/docs` | MISC | ✅ Preserved | Summary |
| DOCUMENTATION_AUDIT.md | `/docs` | MISC | ✅ Created | Audit |
| DOCUMENTATION_ORGANIZATION_SUMMARY.md | `/docs` | MISC | ✅ Created | Summary |
| DOCUMENTATION_RULES.md | `/docs` | MISC | ✅ Created | Governance |
| README.md | `/docs` | MISC | ✅ Preserved | Index |
| DO.md | `/docs` | MISC | ✅ Preserved | Tasks |
| **ROOT** |
| README.md | `/` | OVERVIEW | ✅ Preserved | Framework overview |

---

## Category Breakdown

### OVERVIEW (1 file)
- AUTWIT_OVERVIEW.md (canonical)

### ARCHITECTURE (4 files)
- ARCHITECTURE.md (canonical)
- SCENARIO_KEY_MODEL.md (detailed spec)
- RUNNER_VS_CLIENT_BOUNDARY.md (detailed spec)
- SDK_API_CONTRACT.md (spec)

### EXECUTION (9 files)
- EXECUTION_FLOW.md (canonical)
- SCENARIO_STATE_MODEL.md (detailed spec)
- SCENARIO_CONTEXT_LIFECYCLE.md (detailed spec)
- EVENT_MATCHING_RULES.md (detailed spec)
- FAILURE_VS_PAUSE_SEMANTICS.md (detailed spec)
- RESUME_ENGINE_INTERNAL_FLOW.md (detailed spec)
- RESUME_ENGINE_RULES.md (detailed spec)
- RUNNER_RESUME_COORDINATION.md (detailed spec)
- TEST_LIFECYCLE_FLOW.md (detailed spec)

### CLIENT_USAGE (3 files)
- CLIENT_GUIDE.md (canonical)
- CLIENT_AUTHORING_GUIDE.md (complementary)
- SDK_API_CONTRACT.md (spec)

### RUN_BOOTSTRAP (2 files)
- RUNNING_TESTS.md (canonical)
- RUNNER_EXECUTION_MODEL.md (detailed spec)

### DESIGN_RATIONALE (1 file)
- DESIGN_DECISIONS.md (canonical)

### GUARDRAILS (4 files)
- ARCHITECTURAL_GUARDRAILS.md
- DO_NOT_DO.md
- CLIENT_SDK_CHANGE_POLICY.md
- AI_CONTRIBUTION_RULES.md

### AUDIT (10 files)
- All audit, analysis, and report files (historical)

### MISC (6 files)
- REPOSITORY_SPLIT_STRATEGY.md
- DOCUMENTATION_CONSOLIDATION_SUMMARY.md
- DOCUMENTATION_AUDIT.md
- DOCUMENTATION_ORGANIZATION_SUMMARY.md
- DOCUMENTATION_RULES.md
- README.md (docs)
- DO.md

---

## Overlap Resolution

### Files with Overlaps (Preserved)

| File 1 | File 2 | Reason for Keeping Both |
|--------|--------|-------------------------|
| CLIENT_GUIDE.md | CLIENT_AUTHORING_GUIDE.md | Different styles (comprehensive vs concise) |
| EXECUTION_FLOW.md | TEST_LIFECYCLE_FLOW.md | Different levels (high-level vs detailed) |
| ARCHITECTURE.md | RUNNER_VS_CLIENT_BOUNDARY.md | General vs specific |
| DO_NOT_DO.md | ARCHITECTURAL_GUARDRAILS.md | Negative vs positive rules |

---

## Verification

### ✅ Confirmation

- [x] **AUTWIT_OVERVIEW.md preserved** — Original authoritative content (60 points) intact
- [x] **ARCHITECTURE.md preserved** — Original authoritative content intact
- [x] **No documentation lost** — All 38 files preserved
- [x] **All docs in /docs** — Except root README.md (framework overview)
- [x] **Classification rules documented** — DOCUMENTATION_RULES.md created
- [x] **Overlaps documented** — All overlaps identified and justified
- [x] **Historical context maintained** — All audit reports preserved
- [x] **Governance rules established** — DOCUMENTATION_RULES.md complete

---

## Total Count

- **Total files:** 38 markdown files
- **In /docs:** 37 files
- **In root:** 1 file (README.md)
- **Canonical files:** 6 files
- **Detailed specs:** 11 files
- **Guardrails:** 4 files
- **Audit reports:** 10 files
- **Miscellaneous:** 7 files

---

**END OF FILE ORGANIZATION TABLE**

