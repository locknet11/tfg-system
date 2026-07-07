# Implementation Plan: Agent Download Portal

**Branch**: `013-agent-download` | **Date**: 2026-07-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-agent-download/spec.md`

**User directive**: Agent binary (GraalVM native image) must be signed. Installation script must support download + verification on target. Build agent binary first and add to api classpath (`src/main/resources`). Test with Docker containers from lab folder. Modify code as needed but do not commit or push.

## Summary

Add a user-facing authenticated agent download capability to the central platform. The existing system already signs agent binaries (Blake3 hash + RSA signature) and serves them for agent-to-agent replication via `/api/agent/binary/{replicationToken}`. This feature extends that to:

1. A new **user-facing download endpoint** accessible from the dashboard by authenticated administrators (no replication token needed)
2. **Agent binary packaged in API classpath** (`api/src/main/resources/agents/`) so it survives deployment independently of the agent build directory
3. **Enhanced installation script** (`install-agent-http.sh.tmpl`) that downloads the binary + manifest from Central, verifies Blake3 hash and RSA signature, then installs and launches
4. **UI download section** in the agents management page with platform selection and download button
5. **Download audit records** tracking who downloaded what and when

## Technical Context

**Language/Version**: Java 17 (api), Java 17 + GraalVM 21 (agents/unix), TypeScript 5.x + Angular 17 (ui)
**Primary Dependencies**: Spring Boot 3.1.3 (api), Spring Boot 3.5.7 (agents/unix), Spring Data MongoDB, Spring Security, Primeng 17 (ui), Bouncy Castle Blake3 (both api and agents/unix)
**Storage**: MongoDB — new `agent_download_records` collection
**Testing**: JUnit 5 + Mockito (api), JUnit 5 + Mockito + AssertJ (agents/unix), Jasmine + Karma (ui), Docker Compose lab containers (integration)
**Target Platform**: Linux server (api), Linux/macOS GraalVM native binary (agent), Web browser (ui)
**Project Type**: Web application — API backend + Angular UI + native agent binary
**Performance Goals**: Binary download serves files up to 100 MB; download initiation from UI in under 30 seconds; hash verification on target in under 5 seconds
**Constraints**: Agent binary must be signed (Blake3 + RSA already implemented); installation script must run on minimal Linux targets (only curl/wget + sh required); GraalVM-native safe (installation script, not Java); UI must follow existing Angular patterns
**Scale/Scope**: Hundreds of administrators across multiple organizations; 2-3 platform variants initially (linux-x86_64, macos-aarch64)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md`, `.agents/skills/angular-component/SKILL.md`
- [x] English-only rule satisfied: all code, UI text (with i18n annotations), script content, docs, and comments in English
- [x] Proposed design is the smallest correct change:
  - API: one new controller + one new service (reuses existing `AgentBinaryService` and `BinaryIntegrityService`)
  - UI: one new component on the existing agents page (reuses existing `AgentsService`)
  - agents/unix: modify existing install script template (add verification steps)
  - Script: new build-and-package script to place binary in API classpath
- [x] Stack rules captured:
  - `api/`: new `AgentDownloadController`, `AgentDownloadService`, `AgentDownloadRecord` entity — follows existing Spring Boot patterns (constructor injection, DTO boundaries, `jakarta.validation`)
  - `ui/`: new download component on agents page — follows Angular standalone component pattern with signal inputs, OnPush, i18n
  - `agents/unix/`: modify `install-agent-http.sh.tmpl` — follows existing template via `String.replace()` (GraalVM-safe)
- [x] Verification steps identified:
  - `api/`: unit test `AgentDownloadService`, integration test download endpoint with authentication
  - `ui/`: component renders correctly, download button triggers correct API call
  - `agents/unix/`: test that modified install script verifies hash and signature correctly
  - Integration: build agent binary, place in API resources, start API, download via endpoint, run install script on Docker target, verify agent registers
- [x] Git actions identified: explicit user approval required before any git command
- [x] All requirements resolved from spec and user directive — no unknowns

## Project Structure

### Documentation (this feature)

```text
specs/013-agent-download/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── agent-download-api.md
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
api/
└── src/
    ├── main/java/com/spulido/tfg/domain/replication/
    │   ├── controller/
    │   │   ├── AgentBinaryController.java              # EXISTING — replication-only binary serving
    │   │   └── AgentDownloadController.java            # NEW — user-facing download endpoint
    │   ├── model/
    │   │   └── AgentDownloadRecord.java                # NEW — download audit entity
    │   ├── db/
    │   │   └── AgentDownloadRecordRepository.java      # NEW — MongoDB repository
    │   ├── model/dto/
    │   │   ├── AgentDownloadInfo.java                  # NEW — download response DTO
    │   │   └── AgentPlatformInfo.java                  # NEW — platform availability info
    │   ├── services/
    │   │   ├── AgentBinaryService.java                 # EXISTING — interface
    │   │   ├── BinaryIntegrityService.java             # EXISTING — interface
    │   │   └── AgentDownloadService.java               # NEW — download orchestration
    │   └── services/impl/
    │       ├── AgentBinaryServiceImpl.java             # MODIFIED — support classpath + version/plat awareness
    │       ├── BinaryIntegrityServiceImpl.java         # EXISTING — no changes
    │       └── AgentDownloadServiceImpl.java           # NEW — download service impl
    └── main/resources/
        ├── application.properties                      # MODIFIED — add agent.binary.resource-path
        └── agents/                                     # NEW directory — agent binaries placed here
            ├── linux-x86_64/
            │   └── agent                               # Built GraalVM Linux binary
            └── macos-aarch64/
                └── agent                               # Built GraalVM macOS binary

agents/unix/
└── src/main/resources/scripts/
    └── install-agent-http.sh.tmpl                      # MODIFIED — add hash + signature verification

ui/
└── src/app/pages/agents/
    ├── feature/agents-list/
    │   ├── agents-list.component.ts                    # MODIFIED — add download section trigger
    │   └── agents-list.component.html                  # MODIFIED — add download button
    └── feature/
        └── agent-download/                             # NEW component
            ├── agent-download.component.ts
            ├── agent-download.component.html
            └── agent-download.component.scss

scripts/
└── build-agent-and-package.sh                          # NEW — build GraalVM native image + copy to api resources
```

**Structure Decision**: The download feature extends the existing replication domain package since it reuses `AgentBinaryService` and `BinaryIntegrityService`. A new `AgentDownloadController` provides the user-facing endpoint while `AgentBinaryController` continues to serve replication-only downloads. The UI downloads live as a new component within the existing agents feature module.

## Complexity Tracking

> No constitution violations. Design reuses existing signing infrastructure (`BinaryIntegrityService`, `BinaryIntegrityVerifier`), existing binary loading (`AgentBinaryService`), and existing UI patterns. New code is limited to: 1 API controller, 1 API service, 1 MongoDB entity, 1 UI component, 1 modified script template, 1 build helper script.
