# AUTWIT Documentation Rules

**This document defines the governance rules for all AUTWIT documentation.**

**Status:** MANDATORY — All contributors must follow these rules.

---

## Purpose

This document ensures:
- **Consistency** — All documentation follows the same structure
- **Discoverability** — Easy to find the right document
- **Maintainability** — Clear rules prevent documentation sprawl
- **No Duplication** — Single source of truth for each topic

---

## Documentation Categories

Every `.md` file in `/docs` MUST belong to exactly ONE category:

### 1. OVERVIEW

**Purpose:** What AUTWIT is, goals, principles, non-goals

**Canonical File:** `AUTWIT_OVERVIEW.md`

**Content:**
- What AUTWIT is and is not
- Core principles
- Problems AUTWIT solves
- Key concepts

**DO NOT include:**
- Implementation details
- API references
- Step-by-step guides

---

### 2. ARCHITECTURE

**Purpose:** Module responsibilities, dependency direction, public vs internal boundaries

**Canonical File:** `ARCHITECTURE.md`

**Content:**
- Module layout
- Module responsibilities
- Dependency direction
- Public vs internal boundaries
- Architecture diagrams

**DO NOT include:**
- Runtime execution details
- Client usage examples
- Design rationale (use DESIGN_DECISIONS.md)

---

### 3. EXECUTION

**Purpose:** Runtime behavior, scenario lifecycle, pause/resume mechanism

**Canonical File:** `EXECUTION_FLOW.md`

**Content:**
- How tests execute
- Pause/resume mechanism
- Event matching flow
- Context restoration
- State transitions

**Detailed Specs (Complementary):**
- `SCENARIO_STATE_MODEL.md` — State transitions
- `SCENARIO_CONTEXT_LIFECYCLE.md` — Context lifecycle
- `EVENT_MATCHING_RULES.md` — Event matching rules
- `FAILURE_VS_PAUSE_SEMANTICS.md` — Pause vs failure
- `RESUME_ENGINE_INTERNAL_FLOW.md` — ResumeEngine internals
- `RESUME_ENGINE_RULES.md` — ResumeEngine rules
- `RUNNER_RESUME_COORDINATION.md` — Runner-engine coordination
- `TEST_LIFECYCLE_FLOW.md` — Detailed lifecycle steps

**DO NOT include:**
- Client usage examples (use CLIENT_GUIDE.md)
- Design rationale (use DESIGN_DECISIONS.md)

---

### 4. CLIENT_USAGE

**Purpose:** How clients write step definitions, feature files, and use the SDK

**Canonical File:** `CLIENT_GUIDE.md`

**Complementary Files:**
- `CLIENT_AUTHORING_GUIDE.md` — Original authoritative guide
- `SDK_API_CONTRACT.md` — SDK API contract

**Content:**
- How to write step definitions
- Allowed vs forbidden imports
- Code examples
- Best practices
- Complete working examples

**DO NOT include:**
- Internal implementation details
- Engine internals
- Runner configuration (use RUNNING_TESTS.md)

---

### 5. RUN_BOOTSTRAP

**Purpose:** How to execute tests, runner behavior, feature discovery, common errors

**Canonical File:** `RUNNING_TESTS.md`

**Complementary File:**
- `RUNNER_EXECUTION_MODEL.md` — Detailed runner execution model

**Content:**
- Where files must be located
- Cucumber configuration
- TestNG setup
- Maven commands
- Common mistakes and solutions
- Troubleshooting guide

**DO NOT include:**
- Client code examples (use CLIENT_GUIDE.md)
- Design rationale (use DESIGN_DECISIONS.md)

---

### 6. DESIGN_RATIONALE

**Purpose:** Why things are designed the way they are

**Canonical File:** `DESIGN_DECISIONS.md`

**Content:**
- Why single Autwit facade
- Why reflection is used
- Why SkipException for pause
- Why thin runner
- Why SDK hides internals
- All key design decisions with rationale

**DO NOT include:**
- How things work (use ARCHITECTURE.md or EXECUTION_FLOW.md)
- What things are (use AUTWIT_OVERVIEW.md)

---

### 7. GUARDRAILS

**Purpose:** Hard rules, forbidden patterns, policies

**Canonical Files:**
- `ARCHITECTURAL_GUARDRAILS.md` — Required rules (positive)
- `DO_NOT_DO.md` — Forbidden patterns (negative)
- `CLIENT_SDK_CHANGE_POLICY.md` — SDK change policy
- `AI_CONTRIBUTION_RULES.md` — AI contribution rules

**Content:**
- Non-negotiable rules
- Forbidden patterns
- Policy documents
- Enforcement guidelines

**DO NOT include:**
- Design rationale (use DESIGN_DECISIONS.md)
- How things work (use other categories)

---

### 8. AUDIT (Historical)

**Purpose:** Audit reports, analysis documents, historical snapshots

**Files:**
- All `*_AUDIT*.md` files
- All `*_ANALYSIS*.md` files
- All `*_REPORT*.md` files
- All `*_VIOLATION*.md` files
- All `*_SUMMARY*.md` files (audit-related)

**Content:**
- Architectural audits
- Compliance audits
- Violation classifications
- Refactoring analysis
- Historical snapshots

**Rules:**
- **DO NOT modify** — These are historical records
- **DO NOT delete** — Preserve for context
- **DO NOT merge** — Each represents a specific audit/analysis

---

### 9. MISC (Uncategorized)

**Purpose:** Anything that does not cleanly fit other categories

**Files:**
- `REPOSITORY_SPLIT_STRATEGY.md` — Future strategy
- `DOCUMENTATION_CONSOLIDATION_SUMMARY.md` — Historical summary
- `README.md` — Documentation index
- `DO.md` — Task list

**Rules:**
- Use sparingly
- Prefer categorizing if possible
- Document why it doesn't fit elsewhere

---

## Naming Conventions

### Canonical Files (Primary Sources)

Must be named exactly as follows:
- `AUTWIT_OVERVIEW.md`
- `ARCHITECTURE.md`
- `EXECUTION_FLOW.md`
- `CLIENT_GUIDE.md`
- `RUNNING_TESTS.md`
- `DESIGN_DECISIONS.md`

**DO NOT:**
- Create variations (e.g., `AUTWIT_OVERVIEW_V2.md`)
- Use dates in canonical names
- Use author names in canonical names

### Detailed Specifications

Use descriptive names:
- `SCENARIO_STATE_MODEL.md`
- `EVENT_MATCHING_RULES.md`
- `RESUME_ENGINE_INTERNAL_FLOW.md`

**Pattern:** `[TOPIC]_[ASPECT].md`

### Audit Reports

Use descriptive names with type:
- `ARCHITECTURAL_AUDIT_REPORT.md`
- `SDK_VISIBILITY_VIOLATIONS_ANALYSIS.md`
- `POST_REFACTOR_VERIFICATION_REPORT.md`

**Pattern:** `[TOPIC]_[TYPE].md`

---

## Merge Rules

### When to Merge

**DO NOT merge** if:
- Files serve different audiences
- Files have different levels of detail
- Files are complementary (one high-level, one detailed)
- One is canonical, one is historical

**DO merge** if:
- Files are exact duplicates
- Files cover identical content with identical detail
- One file is clearly obsolete

### Merge Process

1. **Compare content** — Read both files completely
2. **Identify differences** — Note any unique content
3. **Preserve all content** — Never delete unique information
4. **Maintain history** — Add note about merge if needed
5. **Update references** — Update any links to merged file

---

## What NOT to Document

### In Client Guides

**DO NOT document:**
- Internal implementation details
- Engine internals
- Runner internals
- Port interfaces
- Adapter internals

### In Architecture Docs

**DO NOT document:**
- Step-by-step usage
- Code examples
- Troubleshooting guides

### In Execution Docs

**DO NOT document:**
- Design rationale (use DESIGN_DECISIONS.md)
- Client usage (use CLIENT_GUIDE.md)

---

## Review Checklist Before Adding New Documentation

Before creating a new `.md` file, verify:

- [ ] **Category identified** — Which category does it belong to?
- [ ] **Canonical file exists?** — Should this be added to canonical file instead?
- [ ] **Duplicate check** — Does similar content already exist?
- [ ] **Naming convention** — Does name follow conventions?
- [ ] **Location** — Should it be in `/docs` or elsewhere?
- [ ] **Audience** — Who is this for? (clients, developers, maintainers)
- [ ] **Scope** — Is scope appropriate for the category?
- [ ] **Links** — Should it link to/from canonical files?

---

## File Organization

### Directory Structure

```
/docs
├── README.md (navigation index)
├── AUTWIT_OVERVIEW.md (canonical)
├── ARCHITECTURE.md (canonical)
├── EXECUTION_FLOW.md (canonical)
├── CLIENT_GUIDE.md (canonical)
├── RUNNING_TESTS.md (canonical)
├── DESIGN_DECISIONS.md (canonical)
├── [Technical Specifications - detailed specs]
├── [Guardrails - rules and policies]
├── [Audit Reports - historical]
└── [Miscellaneous - uncategorized]
```

### All Files in `/docs`

**Rule:** All documentation files MUST be in `/docs` directory.

**Exception:** `README.md` in root (framework overview, not documentation)

---

## Content Guidelines

### Writing Style

- **Clear and direct** — No marketing language
- **Authoritative** — State facts, not opinions
- **Complete** — Cover the topic fully
- **Structured** — Use consistent headings

### Code Examples

- **Working examples** — Code must be correct
- **Complete examples** — Include necessary context
- **Annotated** — Explain what code does
- **Current** — Use current APIs

### Diagrams

- **ASCII diagrams** — Preferred for version control
- **Clear labels** — All components labeled
- **Consistent style** — Use same diagram style

---

## Update Rules

### When to Update

**Update canonical files when:**
- Architecture changes
- New features added
- APIs change
- Principles evolve

**DO NOT update:**
- Audit reports (historical)
- Analysis documents (snapshots)
- Historical summaries

### How to Update

1. **Read existing content** — Understand current state
2. **Identify changes** — What needs updating?
3. **Preserve history** — Don't delete historical context
4. **Update links** — Update any cross-references
5. **Review** — Ensure consistency with other docs

---

## Version Control

### Commit Messages

Use clear commit messages:
```
docs: Add [TOPIC] to [CATEGORY]

or

docs: Update [FILE] - [REASON]
```

### Branch Strategy

- **Main branch** — Canonical documentation
- **Feature branches** — Documentation for new features
- **Historical branches** — Preserve audit reports

---

## Enforcement

### Pre-Commit Checks

Before committing new documentation:

1. **Category check** — Does it fit a category?
2. **Naming check** — Does name follow conventions?
3. **Duplicate check** — Does similar content exist?
4. **Location check** — Is it in `/docs`?
5. **Link check** — Are cross-references correct?

### Review Process

All documentation changes should be reviewed for:
- **Accuracy** — Is content correct?
- **Completeness** — Is topic covered fully?
- **Consistency** — Does it match other docs?
- **Clarity** — Is it easy to understand?

---

## Summary

**Key Rules:**
1. Every file belongs to ONE category
2. Canonical files are primary sources
3. Detailed specs complement canonical files
4. Audit reports are historical (never modify)
5. All files in `/docs` (except root README.md)
6. No content loss — preserve all information
7. Clear naming conventions
8. Review before adding new files

**Violations:**
- Creating files outside `/docs`
- Duplicating canonical content
- Modifying audit reports
- Using non-standard names
- Mixing categories in one file

---

## Questions?

If unsure about:
- **Category** → Check this document
- **Naming** → Follow conventions above
- **Location** → Use `/docs`
- **Content** → Check canonical files first
- **Merge** → Preserve all content, document merge

**When in doubt, preserve content and ask for review.**

---

**END OF DOCUMENTATION RULES**

