# Implementation Plan: Docker Container Remediation Skip

**Branch**: `012-docker-remediation-skip` | **Date**: 2026-07-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-docker-remediation-skip/spec.md`

**User directive**: Use at least three detection methods: `/.dockerenv`, `/proc/1/cgroup`, and `/proc/self/mountinfo`. Feel free to explore additional methods.

## Summary

Implement a container detection mechanism in the agent that runs as a pre-condition gate before the remediation step. The agent inspects its runtime environment using filesystem-based checks (`/.dockerenv`, `/proc/1/cgroup`, `/proc/self/mountinfo`). If a container runtime environment is detected (Docker, Podman, containerd, or LXC), the remediation step is skipped entirely, a `SKIPPED` remediation report is generated with a human-readable `skipReason`, and plan execution continues to the next step. If detection is inconclusive, remediation is skipped as a safety precaution.

## Technical Context

**Language/Version**: Java 17 (both modules)
**Primary Dependencies**: Spring Boot 3.1.3 (api/), Spring Boot 3.5.7 (agents/unix/), Spring Data MongoDB (api/), Spring RestTemplate (agents/unix/)
**Storage**: MongoDB — `remediation_records` collection (new `skipReason` field)
**Testing**: JUnit 5 + Mockito + AssertJ (agents/unix/), spring-boot-starter-test + Mockito (api/)
**Target Platform**: Linux server (api/), Linux/macOS native binary via GraalVM (agents/unix/)
**Project Type**: Agent module change + API module field addition
**Performance Goals**: Container detection completes in under 1 second with no network calls
**Constraints**: GraalVM-native safe — all detection uses `java.nio.file.Files` (no reflection, no classpath scanning); detection must be filesystem-only (no external commands spawned)
**Scale/Scope**: All agents performing remediation; hundreds of agents across organizations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md`
- English-only rule satisfied: all code, comments, identifiers, and user-facing `skipReason` text in English
- Proposed design is the smallest correct change: a new `ContainerDetector` class in `agents/unix/`, a guard clause in `RemediationStepHandler`, and a `skipReason` field on existing entities
- Stack rules captured:
  - `agents/unix/`: new `container/ContainerDetector.java` using `java.nio.file.Files` (GraalVM-safe), integrated into existing step handler via constructor injection
  - `api/`: new `skipReason` field on `RemediationRecord`, `RemediationReportRequest`, and `RemediationInfo` — backward-compatible MongoDB document change
- Verification steps identified:
  - `agents/unix/`: unit tests for `ContainerDetector` with mocked filesystem, integration tests for `RemediationStepHandler` with container-detected scenario
  - `api/`: verify `skipReason` is persisted and returned in API responses
- Git actions identified; explicit user approval required before any git command runs
- All requirements resolved from spec — no unknowns

## Project Structure

### Documentation (this feature)

```text
specs/012-docker-remediation-skip/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── remediation-report-api.md
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
agents/unix/
└── src/main/java/com/spulido/agent/
    ├── container/                                     # NEW package
    │   └── ContainerDetector.java                     # Detects container runtime via filesystem checks
    ├── worker/step/
    │   └── RemediationStepHandler.java                # MODIFIED: add pre-remediation container check
    └── worker/http/dto/
        └── RemediationReportRequest.java              # MODIFIED: add skipReason field

api/
└── src/main/java/com/spulido/tfg/
    └── domain/remediation/
        ├── model/
        │   └── RemediationRecord.java                 # MODIFIED: add skipReason field
        └── model/dto/
            ├── RemediationReportRequest.java          # MODIFIED: add skipReason field
            └── RemediationInfo.java                   # MODIFIED: add skipReason field
```

**Structure Decision**: A single new `container` package in the agent module encapsulates all detection logic. The remediation step handler in the existing `worker/step` package gains a pre-condition check. The API module only needs a single field addition across three existing classes. No new collections, no new endpoints, no new cross-module dependencies.

## Complexity Tracking

> No constitution violations. Design uses smallest correct change: one new class, one guard clause, one new field. All detection is filesystem-based (GraalVM-safe), no external libraries needed.
