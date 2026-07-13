# Implementation Plan: Unix Agent Self-Destruction & Self-Cleanup

**Branch**: `021-agent-self-destruct` | **Date**: 2026-07-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/021-agent-self-destruct/spec.md`

## Summary

Give the unix agent (`agents/unix`, Spring Boot 3 + GraalVM native) a teardown lifecycle that fully removes it and its artifacts from the host once it is no longer needed. Two triggers: (1) **plan completion** — the existing step-execution loop detects all steps terminal, reports the final plan result, then tears down; (2) **platform de-provision** — the central API marks the agent deleted and the next heartbeat response carries an authenticated de-provision signal; the agent also self-destructs implicitly after sustained *authenticated rejection* (its registration was revoked). Teardown removes the install script, agent binary, env/config file, working data (logs, downloaded tool binaries, working dirs), and any OS-level registration (systemd/launchd/cron), then exits without restarting. Cleanup is single-shot, best-effort (each step independent, failures recorded), idempotent, and reports a per-artifact outcome record to central for audit before the process exits. All teardown shell lives in a `*.sh.tmpl` resource rendered via the existing `ScriptTemplateService` (`ClassPathResource` + `String.replace()`), never built inline — per the repo script/template boundary.

## Technical Context

**Language/Version**: Java 21 (agents/unix, GraalVM native image; api Spring Boot 3), TypeScript 5 / Angular 17 (ui — only if a de-provision status surfaces; none required by this feature)
**Primary Dependencies**: Spring Boot 3, Spring `@Scheduled`, `RestTemplate` (agent HTTP), Lombok, `ClassPathResource` + `ScriptTemplateService` for shell templates; api uses MongoDB (Spring Data)
**Storage**: MongoDB (api) — persist the teardown-outcome audit record and the agent de-provision flag; agent host filesystem is the teardown target (no agent-side DB)
**Testing**: JUnit 5 + Mockito (agent + api), deterministic unit tests with no real network/filesystem; one controlled sandbox/integration test for teardown shell rendering + execution
**Target Platform**: Linux (linux-amd64) and macOS (darwin-arm64) unix hosts — the agent's existing supported targets; Windows out of scope
**Project Type**: Multi-module (agent + web-service api + Angular ui); this feature is agent-primary with an additive api change
**Performance Goals**: Teardown completes within one heartbeat interval (~30s) of trigger; final report flushed before local removal
**Constraints**: GraalVM-native-safe (no reflection-heavy or `String.format`-built scripts; templates only). Best-effort, idempotent, single-shot. Must not remove host data the agent did not create. De-provision signal must be authenticated to this agent.
**Scale/Scope**: Per-agent, per-host operation; fleet-wide but each teardown is local and independent

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Repository guidance reviewed** (`AGENTS.md`/`CLAUDE.md`, `.agents/skills/*`): YES. The script/template boundary rule is the governing constraint — all teardown shell goes in `agents/unix/src/main/resources/scripts/self-destruct.sh.tmpl` and is rendered via `ScriptTemplateService` (`ClassPathResource` + `String.replace()`); no inline `String.format`/`StringBuilder`/concatenation. FreeMarker only in `api/`.
- **English-only rule**: YES. All code, identifiers, comments, and any operator-facing status text authored in English; UI (if touched) uses existing i18n patterns.
- **Smallest correct change / no unnecessary abstraction**: YES. Reuse `AgentLifecycle`, `ScriptTemplateService`, `AgentHttpClient`, the existing heartbeat/plan loop, the existing `StepAction.SELF_DESTRUCT` enum value, `AgentController.deleteAgent`, and `AgentStatus.KILLED`. Add one `TeardownService` (agent), one `.sh.tmpl`, additive DTO fields, one api endpoint + one flag. No new frameworks.
- **Stack rules for affected modules**: `agents/unix` (constructor injection, Lombok where present, ordered imports, no wildcards, template boundary); `api/` (constructor injection, DTO boundaries, `jakarta.validation`, `@ControllerAdvice` mapping, MongoDB via Spring Data). `ui/` not required.
- **Verification steps per module**: agent → `./mvnw test` + native build sanity + sandboxed teardown integration test; api → `./mvnw test`. Documented in quickstart.md.
- **Git actions**: none run without explicit user approval. The `before_*`/`after_*` git hooks are all `optional: true` and are skipped unless the user asks to commit.
- **Unknowns resolved or called out**: three open questions from the spec (failed-auth threshold, mid-plan deletion behavior, binary self-removal mechanism) are resolved in `research.md` with conservative decisions; no `NEEDS CLARIFICATION` remains.

**Result: PASS** (no violations; Complexity Tracking not required).

## Project Structure

### Documentation (this feature)

```text
specs/021-agent-self-destruct/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output — decisions on the 3 open questions + technique
├── data-model.md        # Phase 1 output — entities: TeardownOutcome, ArtifactSet, DeprovisionSignal
├── quickstart.md        # Phase 1 output — build/test/verify steps
├── contracts/           # Phase 1 output — heartbeat + teardown-report + deprovision contracts
│   ├── heartbeat-deprovision.md
│   ├── teardown-report.md
│   └── agent-deprovision-flag.md
└── checklists/
    └── requirements.md  # Spec quality checklist (from /speckit.specify)
```

### Source Code (repository root)

```text
agents/unix/src/main/java/com/spulido/agent/
├── teardown/
│   ├── TeardownService.java          # NEW — orchestrates single-shot, best-effort, idempotent teardown
│   ├── TeardownTrigger.java          # NEW — enum: PLAN_COMPLETION, PLATFORM_DEPROVISION, AUTH_REVOKED
│   ├── ArtifactSet.java              # NEW — resolves host paths of agent artifacts to remove
│   ├── ArtifactRemovalResult.java    # NEW — per-artifact outcome (REMOVED/FAILED/NOT_PRESENT)
│   └── TeardownOutcome.java          # NEW — aggregate record reported to central
├── heartbeat/
│   └── HeartbeatSender.java          # EDIT — inspect response for deprovision signal; count auth rejections
├── worker/
│   ├── WorkerCoordinator.java        # EDIT — on plan fully complete, report final status then trigger teardown
│   └── step/
│       └── SelfDestructStepHandler.java  # NEW — SELF_DESTRUCT step delegates to TeardownService (replaces Echo stub)
├── worker/http/
│   ├── AgentHttpClient.java          # EDIT — add reportTeardownOutcome(...)
│   └── dto/
│       ├── HeartbeatResponse.java    # EDIT — add deprovision flag(s)
│       └── TeardownReportRequest.java# NEW — DTO for outcome report
└── utils/
    └── AgentLifecycle.java           # REUSE — stop() already present

agents/unix/src/main/resources/scripts/
└── self-destruct.sh.tmpl             # NEW — detached POSIX cleanup: unlink binary + sweep residual artifacts + OS registration, then exit

api/src/main/java/com/spulido/tfg/domain/agent/
├── controller/AgentCommunicationController.java     # EDIT — heartbeat response carries deprovision signal; NEW teardown-report endpoint
├── services/impl/AgentCommunicationServiceImpl.java # EDIT — resolve deprovision state; persist teardown outcome
├── services/impl/AgentServiceImpl.java              # EDIT — deleteAgent marks de-provision reachable by heartbeat (soft-delete before hard delete)
├── model/dto/HeartbeatResponse.java                 # EDIT — add deprovision fields
├── model/dto/TeardownReportRequest.java             # NEW — inbound outcome DTO
└── model/AgentTeardownRecord.java                   # NEW — audit record persisted in MongoDB

agents/unix/src/test/java/com/spulido/agent/teardown/
├── TeardownServiceTest.java          # NEW — single-shot, best-effort, idempotent, ordering (report-before-remove)
├── ArtifactSetTest.java              # NEW — path resolution, not-present handling
└── SelfDestructStepHandlerTest.java  # NEW
agents/unix/src/test/java/com/spulido/agent/heartbeat/
└── HeartbeatSenderTest.java          # EDIT — deprovision signal + sustained-auth-rejection triggers teardown
agents/unix/src/test/.../integration/
└── SelfDestructSandboxIT.java        # NEW — renders + runs self-destruct.sh.tmpl in a temp sandbox; verifies removal, idempotency, best-effort
api/src/test/java/com/spulido/tfg/domain/agent/
└── AgentCommunicationServiceTeardownTest.java # NEW — deprovision signal on deleted agent; outcome persistence
```

**Structure Decision**: Agent-primary. New `com.spulido.agent.teardown` package encapsulates the lifecycle so triggers (heartbeat, worker loop, SELF_DESTRUCT step) all funnel into one `TeardownService`. The api change is minimal and additive to the existing agent-communication controller/service and the existing `deleteAgent` path. No UI change is required by the spec; if de-provision status is later surfaced it reuses existing agent-list views.

## Complexity Tracking

No constitution violations — section intentionally empty.
