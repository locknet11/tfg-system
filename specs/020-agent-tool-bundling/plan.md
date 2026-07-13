# Implementation Plan: Agent Tool Bundling

**Branch**: `020-agent-tool-bundling` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/020-agent-tool-bundling/spec.md`

## Summary

Replace the current `EchoStepHandler` placeholders behind `NETWORK_SCAN`, `SERVICE_SCAN`, and `SYSTEM_SCAN` with real, tool-backed step handlers, and give any agent-executed command (remediation/exploit scripts included) access to a file-retrieval capability equivalent to `curl`/`wget` — all without depending on the target host having any of these tools pre-installed. Tool binaries (nmap, rustscan, nc, curl) are bundled as classpath resources in `agents/unix`, extracted to a runtime-writable directory at agent startup, added to the `PATH` used by `CommandExecutor`'s process invocations, and invoked with an enforced timeout that maps failures to a failed `StepResult` — reusing the existing `StepHandler` / `CommandExecutor` / `TaskResult` patterns already used by `RemediationStepHandler` and `ExecuteExploitStepHandler`. A necessary prerequisite, discovered during planning, is threading the plan's `targetIp` (already fetched by `WorkerCoordinator` but currently never passed to handlers) through `TaskExecutionService` into `StepHandler.handle(...)`, since the scan handlers have no way to know what to scan today.

## Technical Context

**Language/Version**: Java 17 (agents/unix, Spring Boot 3 / GraalVM native image)
**Primary Dependencies**: Spring Boot 3, Spring AOT (`RuntimeHintsRegistrar` for native-image resource inclusion), bundled native CLI tools (nmap, rustscan, nc, curl) as classpath resources — no new Java library dependencies
**Storage**: N/A (no persistence; bundled binaries are classpath resources, extracted to a runtime temp directory)
**Testing**: JUnit 5 + Mockito (existing agent test pattern, e.g. `ExecuteExploitStepHandlerTest.java`); unit tests use a fake/mock `CommandExecutor` and do not require the real tool binaries or network access
**Target Platform**: Linux amd64 (primary deployment target for the native agent) and macOS (local native builds via `package-macos.sh`), unix-like hosts only
**Project Type**: Single native-image application module (`agents/unix/`), no UI or API changes
**Performance Goals**: Bundled tool extraction adds negligible startup overhead (<1s, one-time per agent process); individual scan/fetch step timeouts bounded per FR-009 (default budgets consistent with existing `RemediationStepHandler`/`ExecuteExploitStepHandler` timeouts, tunable per step type)
**Constraints**: Native-image build must remain GraalVM-native-safe (no reflection-heavy resource/XML binding); bundled binaries must be statically linked or dependency-free enough to run on a bare target host; no shell scripts or command strings built via inline concatenation — must follow the `ClassPathResource` + `String.replace()` template convention already used for `install-agent-*.sh.tmpl`
**Scale/Scope**: 4 bundled tool capabilities (network discovery, port/service scan, raw TCP, file retrieval), 2 new step handlers (`NETWORK_SCAN`, `SERVICE_SCAN`/`SYSTEM_SCAN`), 1 new provisioning component, signature change to the `StepHandler` interface and its 6 existing implementations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` applies to `agents/unix/` — GraalVM native build, `ClassPathResource` + `String.replace()` script/template boundary (no inline `String.format`/concatenation for scripts), standard Java import ordering, Lombok where present.
- [x] English-only rule satisfied: all logs, error messages, and code comments authored in English.
- [x] Proposed design is the smallest correct change: reuses the existing `StepHandler` / `CommandExecutor` / `TaskResult` / `ServiceInfo` abstractions; no new frameworks or persistence introduced; the one interface-signature change (`StepHandler.handle` gaining a `targetIp` parameter) is the minimum needed to make scan handlers functional, since the plan's target is currently fetched by `WorkerCoordinator` but never reaches handlers.
- [x] Stack rules captured: `agents/unix` changes follow the module's existing patterns (constructor injection via `WorkerPoolConfig` `@Bean` wiring, `StepHandler` interface implementations, `ClassPathResource`-based templates for any generated script/config content).
- [x] Verification steps identified: unit tests per new/changed handler with a mocked `CommandExecutor` (no real tool execution or network access in unit scope, per `AGENTS.md` testing tips); a manual `./mvnw -Pnative native:compile` + local run against the existing `lab/` Docker targets to verify bundled tools execute and produce real findings.
- [x] Git actions identified: branch `020-agent-tool-bundling` already created via the `speckit.git.feature` hook; no further git actions without explicit user approval.
- [x] Unknown requirements resolved: tool selection (nmap, rustscan, nc, curl) confirmed by the user's technical view; packaging/extraction strategy resolved in Phase 0 research below.

**Post-Phase 1 re-check**: Design (data-model.md, contracts/step-handler-interface.md, quickstart.md) introduces no new violations beyond the single documented `StepHandler` signature change in Complexity Tracking. No new persistence, no new external API surface, no new frameworks. Gate remains PASSED.

## Project Structure

### Documentation (this feature)

```text
specs/020-agent-tool-bundling/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md         # Phase 1 output
├── quickstart.md         # Phase 1 output
├── contracts/             # Phase 1 output
│   └── step-handler-interface.md
└── tasks.md               # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
agents/unix/
├── src/main/resources/tools/
│   ├── linux-amd64/
│   │   ├── nmap
│   │   ├── rustscan
│   │   ├── nc
│   │   └── curl
│   └── darwin-arm64/               # matches existing package-macos.sh local dev target
│       ├── nmap
│       ├── rustscan
│       ├── nc
│       └── curl
├── src/main/java/com/spulido/agent/
│   ├── worker/
│   │   ├── CommandExecutor.java                       # MODIFY: PATH augmented with extracted tools dir
│   │   ├── TaskExecutionService.java                   # MODIFY: thread targetIp into handler.handle(...)
│   │   ├── WorkerCoordinator.java                      # MODIFY: pass targetIp; wire new handlers; replace EchoStepHandler for scan actions
│   │   ├── tools/                                      # NEW package
│   │   │   ├── BundledTool.java                        # NEW: enum of bundled capabilities (NMAP, RUSTSCAN, NC, CURL)
│   │   │   ├── BundledToolProvisioner.java              # NEW: extracts classpath resource -> runtime temp dir, chmod +x, resolves PATH entry
│   │   │   └── ToolExtractionException.java             # NEW: thrown when no matching binary for current os/arch
│   │   └── step/
│   │       ├── StepHandler.java                         # MODIFY: handle(StepAction, Map<StepAction,StepResult>, String targetIp)
│   │       ├── EchoStepHandler.java                     # MODIFY: signature only
│   │       ├── RemediationStepHandler.java               # MODIFY: signature only
│   │       ├── ExecuteExploitStepHandler.java            # MODIFY: signature only
│   │       ├── ExploitationKnowledgeStepHandler.java      # MODIFY: signature only
│   │       ├── RequestReplicationStepHandler.java         # MODIFY: signature only
│   │       ├── TransferAgentStepHandler.java              # MODIFY: signature only
│   │       ├── NetworkScanStepHandler.java                # NEW: NETWORK_SCAN via bundled nmap host discovery
│   │       └── ServiceScanStepHandler.java                # NEW: SERVICE_SCAN/SYSTEM_SCAN via bundled rustscan + nmap service detection
│   └── config/
│       └── WorkerPoolConfig.java                         # MODIFY: provision tools bean, inject into new handlers, extend CommandExecutor PATH
└── src/test/java/com/spulido/agent/worker/
    ├── tools/
    │   └── BundledToolProvisionerTest.java                # NEW
    └── step/
        ├── NetworkScanStepHandlerTest.java                 # NEW
        ├── ServiceScanStepHandlerTest.java                 # NEW
        └── (existing handler tests updated for new signature)
```

**Structure Decision**: Single-module change confined to `agents/unix/`. No changes to `ui/`, `api/`, or `lab/` are required — the central platform's reporting contracts (`RemediationReportRequest`, plan step responses) are unchanged; only what happens *inside* the agent's own step execution changes. Bundled tool binaries are stored per OS/arch subdirectory under `src/main/resources/tools/` so a single build can embed the variants needed for both the macOS local-dev native build and the Linux amd64 deployment target; `BundledToolProvisioner` picks the matching subdirectory at runtime via `os.name`/`os.arch`.

## Complexity Tracking

> One deviation from "smallest possible change" is justified below; no other violations.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|---------------------------------------|
| `StepHandler.handle(...)` signature change across all 6 implementations | Scan handlers need the plan's target address, which `WorkerCoordinator` already has (`planResponse.getTargetIp()`) but currently never passes past itself; the `context` map is typed `Map<StepAction, StepResult>` and cannot carry a raw target string without a synthetic/fake `StepAction` entry, which would be a worse abstraction | Stuffing `targetIp` into a fake `StepResult` under a placeholder `StepAction` was rejected: it overloads `StepResult`'s meaning (a step outcome) to also carry plan-level input data, which is more surprising and harder to maintain than a single added parameter |
