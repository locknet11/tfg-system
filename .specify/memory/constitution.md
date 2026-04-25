<!--
Sync Impact Report
Version change: 0.0.0 -> 1.0.0
Modified principles:
- [PRINCIPLE_1_NAME] -> I. Repository Guidance Is Authoritative
- [PRINCIPLE_2_NAME] -> II. English-First Code And UX Text
- [PRINCIPLE_3_NAME] -> III. Minimal, Correct, Verifiable Changes
- [PRINCIPLE_4_NAME] -> IV. Stack-Conformant Angular And Spring Boot
- [PRINCIPLE_5_NAME] -> V. Explicit User Control For Git And Unknowns
Added sections:
- Engineering Constraints
- Workflow And Quality Gates
Removed sections:
- None
Templates requiring updates:
- ✅ updated /Users/locknet/Projects/tfg-system/.specify/templates/plan-template.md
- ✅ updated /Users/locknet/Projects/tfg-system/.specify/templates/spec-template.md
- ✅ updated /Users/locknet/Projects/tfg-system/.specify/templates/tasks-template.md
- ⚠ pending .specify/templates/commands/*.md (directory not present)
- ⚠ pending README.md or docs/quickstart.md (not reviewed because files were not identified in this run)
Follow-up TODOs:
- None
-->

# TFG System Constitution

## Core Principles

### I. Repository Guidance Is Authoritative
`AGENTS.md` and the skills under `.agents/skills/` define the project's development
rules. Work MUST follow repository-specific guidance before generic framework habits.
When guidance overlaps, the more specific source wins for the affected scope.

Rationale: this repository mixes Angular UI, Spring Boot services, and native-agent
workflows that need consistent, local conventions.

### II. English-First Code And UX Text
All source code, identifiers, commit-ready documentation, user-facing copy, and code
comments MUST be written in English. UI text MUST use the repository i18n patterns:
Angular template text uses `i18n`, component text uses `$localize`, and translation
resources live in `ui/src/i18n/*.json`. Comments MUST be rare, concise, and limited to
non-obvious logic.

Rationale: a single language keeps the codebase readable across modules and makes the
translation workflow predictable.

### III. Minimal, Correct, Verifiable Changes
Changes MUST be the smallest correct implementation that satisfies the requirement.
Contributors MUST prefer existing patterns, avoid speculative abstractions, avoid
backward-compatibility code unless a concrete external need exists, and keep logic in
place unless reuse is clearly justified. Every non-trivial change MUST include an
appropriate verification step using the module's native tooling.

Rationale: small, validated changes reduce regressions and keep the system maintainable.

### IV. Stack-Conformant Angular And Spring Boot
UI work MUST follow the Angular repository rules: strict TypeScript, no `any` unless
unavoidable and justified, `readonly`/`const` preference, kebab-case file names,
PascalCase types, camelCase functions, observable names ending in `$`, grouped imports,
and centralized HTTP error handling. New Angular component work SHOULD follow the local
Angular skill guidance for standalone components, signal inputs/outputs, native control
flow, `host` metadata, and accessibility.

API and agent work MUST follow Spring Boot best practices used by this repository:
constructor injection, explicit DTO boundaries, validation with `jakarta.validation`,
centralized exception mapping with `@ControllerAdvice`, standard Java import ordering,
and secrets kept outside source control.

Rationale: stack-conformant code is easier to review, test, and extend across teams.

### V. Explicit User Control For Git And Unknowns
Git commands MUST NOT be run without explicit user approval. If a task requires git
state changes such as branching, committing, rebasing, or pushing, approval MUST be
requested first. When implementation details are unknown, unsafe, or underspecified,
the contributor MUST ask instead of guessing.

Rationale: source-control actions are high-impact and ambiguous requirements create
avoidable rework.

## Engineering Constraints

- Repository modules are `ui/` for Angular, `api/` for Spring Boot, and `agents/unix/`
  for the GraalVM-capable Spring Boot agent.
- UI formatting uses Prettier; UI linting relies on Angular strictness and Prettier.
- Unit tests MUST be deterministic and MUST avoid real network or filesystem access
  unless the test is explicitly integration-scoped.
- Production code MUST NOT use `console.log` for UI logging.
- Secrets MUST live in environment or configuration files designed for secret handling
  and MUST never be committed.

## Workflow And Quality Gates

- Before implementing, contributors MUST read the relevant repository guidance and any
  applicable local skill instructions.
- Plans MUST include a constitution check covering repository guidance, English-only
  output, minimal design, stack-specific constraints, and whether git approval is needed.
- Specifications MUST state user stories, functional requirements, measurable success
  criteria, and any i18n, accessibility, security, or validation expectations implied by
  the work.
- Tasks MUST reference concrete file paths, preserve user-story independence, and include
  verification tasks for affected modules.
- If a requested action would violate this constitution, the contributor MUST stop and
  surface the conflict.

## Governance

This constitution supersedes conflicting local planning defaults and generic agent
behavior for this repository. Compliance MUST be checked during specification, planning,
task generation, implementation, and review.

Amendments require:
1. An explicit request or repository decision.
2. A clear rationale and impact summary.
3. Updates to affected templates or guidance files in the same change when applicable.

Versioning policy:
1. MAJOR for incompatible governance changes or principle removals.
2. MINOR for new principles, sections, or materially expanded mandatory guidance.
3. PATCH for wording clarifications and non-semantic edits.

Review expectations:
1. Reviewers MUST verify compliance with all five core principles.
2. Any exception MUST be documented in the relevant plan's complexity or justification
   section.
3. Unknown implementation details MUST be resolved by asking, not by assumption.

**Version**: 1.0.0 | **Ratified**: 2026-04-25 | **Last Amended**: 2026-04-25
