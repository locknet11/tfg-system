# Implementation Plan: Remote Agent Transfer

**Branch**: `005-remote-agent-transfer` | **Date**: 2026-06-02 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/005-remote-agent-transfer/spec.md`

## Summary

Extend the agent auto-replication flow (spec 004) with a proper remote command execution layer over SSH and a two-path agent binary transfer mechanism. Currently the `TransferAgentStepHandler` embeds the binary as base64 in a shell script and executes it locally — this replaces that with: (1) a `RemoteCommandExecutor` interface + `SshRemoteCommandExecutor` implementation that runs commands on the exploited target via SSH, (2) session context propagation so `ExecuteExploitStepHandler` produces a verified `TargetSession` consumed by `TransferAgentStepHandler`, and (3) two-path transfer logic where the target downloads the binary directly from Central via HTTP (Path A, preferred) or the agent pushes the binary over SSH (Path B, fallback). The existing `CommandExecutor` remains unchanged for local-only commands.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.5.7, GraalVM native)
**Primary Dependencies**: Spring Boot (no-web), RestTemplate, Lombok, Bouncy Castle (Blake3), java.util.Base64, ProcessBuilder (SSH/SCP)
**Storage**: N/A — agent module is stateless (no database); config files written to target `/tmp/`
**Testing**: JUnit 5 + Mockito (mock ProcessBuilder for SSH commands)
**Target Platform**: Linux/macOS (GraalVM native image) — agent binary, target is Unix-like with SSH
**Project Type**: Agent module extension (`agents/unix/`) — no changes to `api/` or `ui/`
**Performance Goals**: Remote command execution <15s, Path A end-to-end <30s, Path B end-to-end <60s (50MB binary)
**Constraints**: Replication token 5-min TTL, binary must pass Blake3+PKI before any execution, SSH with StrictHostKeyChecking=no for exploit-established sessions, GraalVM-safe (no FreeMarker, simple String.replace for templates)
**Scale/Scope**: 4 new interfaces/classes, 2 new templates, 2 modified step handlers, 2 modified config classes, ~15 files total

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md` — Java/Spring Boot conventions apply; `agents/unix/` uses GraalVM-safe patterns (ClassPathResource + String.replace, no FreeMarker)
- [x] English-only rule satisfied for all code, identifiers, templates, docs, and comments
- [x] Proposed design is the smallest correct change — adds one interface + impl for remote execution, one value object for session, one provisioner, two shell templates, extends two existing handlers and two config classes
- [x] Stack rules captured: constructor injection via Lombok, ProcessBuilder for external processes, existing step handler pattern with `StepHandler` interface, context propagation via `Map<StepAction, StepResult>`, script templates under `resources/scripts/`
- [x] Verification steps identified: unit tests for `SshRemoteCommandExecutor` (mocked ProcessBuilder), unit tests for `TransferAgentStepHandler` both paths + fallback, compile verification via `./mvnw compile`
- [x] Git actions identified: no git commands auto-executed — explicit user approval required
- [x] Unknown or ambiguous requirements resolved: all specified in detail by the feature requirements (REQ-1 through REQ-8)

## Project Structure

### Documentation (this feature)

```text
specs/005-remote-agent-transfer/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research decisions
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
├── contracts/           # Phase 1 interface contracts
│   └── remote-command-executor.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
agents/unix/src/main/java/com/spulido/agent/
├── remote/                                                 # NEW package
│   ├── RemoteCommandExecutor.java                          # NEW interface
│   ├── SshRemoteCommandExecutor.java                       # NEW implementation
│   ├── TargetSession.java                                  # NEW value object
│   └── SshSessionProvisioner.java                          # NEW session verification
├── config/
│   ├── AgentConfig.java                                    # MODIFY — add agent.exploit.* properties
│   └── WorkerPoolConfig.java                               # MODIFY — wire new beans, update handlers
├── worker/
│   ├── WorkerCoordinator.java                              # MODIFY — update createDefaultStepHandlers()
│   └── step/
│       ├── ExecuteExploitStepHandler.java                  # MODIFY — add SshSessionProvisioner, record session info
│       └── TransferAgentStepHandler.java                   # REWRITE — 2-path logic, remote exec, tool probing
└── resources/
    └── scripts/
        ├── install-agent.sh.tmpl                           # RENAME → install-agent-transfer.sh.tmpl
        ├── install-agent-http.sh.tmpl                      # NEW — target downloads binary from Central
        └── install-agent-transfer.sh.tmpl                  # RENAMED — binary pushed by agent (no base64 blob)

agents/unix/src/test/java/com/spulido/agent/
├── remote/
│   └── SshRemoteCommandExecutorTest.java                   # NEW — unit tests with mocked ProcessBuilder
└── worker/step/
    └── TransferAgentStepHandlerTest.java                   # NEW — unit tests both paths + fallback
```

**Structure Decision**: New `remote/` package under `agents/unix/` for all remote execution concerns (separate from local `CommandExecutor` in `worker/`). This follows the existing pattern where domain concepts get their own packages (e.g., `worker/http/` for HTTP, `worker/step/` for step handlers). The `remote/` package is independent and self-contained — no circular dependencies with `worker/`.

## Complexity Tracking

No constitution violations. No complexity justifications needed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New `remote/` package | Remote execution is a distinct concern from local execution — separate interface, implementation, session model, and provisioner | Inlining remote logic into `CommandExecutor` would violate single-responsibility and make local execution (which should remain simple) carry SSH overhead |
| Separate `SshSessionProvisioner` from `ExecuteExploitStepHandler` | Session verification is a reusable concern — the provisioner can be used by other steps or retry logic independently | Embedding the probe logic directly in the handler would make retry and error handling harder to test in isolation |
| Two shell templates instead of one | Path A (HTTP download by target) and Path B (agent push) have fundamentally different content — Path A needs DOWNLOAD_URL, Path B needs the binary already on disk | A single template with conditionals would be harder to reason about and test; two templates are each simple and independently verifiable |
