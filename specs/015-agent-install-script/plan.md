# Implementation Plan: Agent Self-Installing Shell Script

**Branch**: `015-agent-install-script` | **Date**: 2026-07-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-agent-install-script/spec.md`

## Summary

Transform the API-served install script (`unix.sh.ftl`) from an echo-only placeholder into a fully automated installer that downloads the agent binary from the central platform, verifies its integrity via Blake3 hash and RSA signature, writes configuration (API key, agent ID, central URL, public key), and launches the agent as a background daemon process — all from a single `curl ... | bash` command. This also fixes a critical auth gap: the agent's `RestTemplate` currently lacks an interceptor to send `X-Agent-Api-Key` and `X-Agent-Id` headers, making agent-to-platform communication non-functional.

## Technical Context

**Language/Version**: Java 21 (api/, agents/unix/), TypeScript 5.x (N/A for this feature)  
**Primary Dependencies**: Spring Boot 3 (API + Agent), FreeMarker (API template engine), GraalVM native-image (agent build)  
**Storage**: MongoDB (API), filesystem (agent config: `/tmp/agent.properties`)  
**Testing**: JUnit 5 + Mockito (API), integration tests (agent shell scripts)  
**Target Platform**: Linux server (API), any Linux x86_64 (agent target), POSIX-compatible shell  
**Project Type**: Web service + native CLI agent  
**Performance Goals**: Install script completes in <60s on broadband; binary download <30s for ~30MB binary  
**Constraints**: POSIX shell compatibility (no bashisms), `/tmp` writable, curl or wget available  
**Scale/Scope**: Single-install per target; hundreds of concurrent installs possible

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Repository guidance reviewed**: `AGENTS.md` and skill files read — all shell scripts MUST live in resource template files (`.ftl`), never built inline
- [x] **English-only rule satisfied**: All user-facing text in install script is English
- [x] **Minimal change**: The change is scoped to one FreeMarker template (`unix.sh.ftl`), one service method (`ScriptServiceImpl`), one new API endpoint, and one agent config fix. No unnecessary abstractions.
- [x] **Stack rules captured**: `api/` (Spring Boot + FreeMarker), `agents/unix/` (GraalVM-native + `ClassPathResource` for templates; no inline script building)
- [x] **Verification steps identified**: Unit tests for ScriptServiceImpl, contract test for install endpoint response, manual integration test for full curl-pipe-bash flow
- [x] **Git actions**: No git operations needed from this plan; explicit approval required before any commit
- [x] **Unknown requirements resolved**: Auth gap identified and included in scope; download URL approach decided in research

## Project Structure

### Documentation (this feature)

```text
specs/015-agent-install-script/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── install-script-api.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
api/
├── src/main/resources/scripts/
│   ├── unix.sh.ftl              # [MODIFY] Install script template - add download, verify, launch
│   └── unix-error.sh.ftl        # [NO CHANGE] Error script unchanged
├── src/main/java/com/spulido/tfg/
│   ├── domain/script/services/
│   │   ├── ScriptService.java           # [NO CHANGE] Interface unchanged
│   │   └── impl/ScriptServiceImpl.java  # [MODIFY] Pass new template variables
│   ├── domain/agent/
│   │   ├── services/impl/AgentServiceImpl.java  # [MODIFY] Generate install token + pass new vars
│   │   └── controller/AgentController.java       # [MODIFY] Include download token in response flow
│   └── domain/replication/controller/
│       └── AgentInstallBinaryController.java     # [NEW] Public binary download for install flow

agents/unix/
├── src/main/java/com/spulido/agent/
│   ├── config/WorkerPoolConfig.java     # [MODIFY] Add API key header interceptor to RestTemplate
│   └── config/AgentConfig.java          # [NO CHANGE] Already has apiKey, agentId, centralPublicKey
├── src/main/resources/
│   ├── scripts/
│   │   ├── install-agent-http.sh.tmpl   # [MODIFY] Write agent.api-key + agent.agent-id (not preauth-code)
│   │   └── install-agent-transfer.sh.tmpl  # [MODIFY] Same fix
│   └── application.properties           # [NO CHANGE] Config placeholders already present
```

**Structure Decision**: This project uses the established multi-module layout (`api/`, `agents/unix/`, `ui/`). Only `api/` and `agents/unix/` are affected. The existing `unix.sh.ftl` template is extended rather than replaced. A new controller (`AgentInstallBinaryController`) is added for the public binary download endpoint used by the install script.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New controller class | Public binary download endpoint needed for install script differs from existing replication-token and dashboard-download flows | Existing endpoints require either replication token (different lifecycle) or user authentication (not available on target machine). A dedicated endpoint with a short-lived install token is the smallest change that meets the requirement. |
