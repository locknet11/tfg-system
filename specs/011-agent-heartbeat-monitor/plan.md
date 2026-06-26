# Implementation Plan: Agent Heartbeat Monitor

**Branch**: `feature/011-agent-heartbeat-monitor` | **Date**: 2026-06-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/011-agent-heartbeat-monitor/spec.md`

**User directive**: Use `@Scheduled` for the 30-second cron on both central (api/) and agent (agents/unix/).

## Summary

Implement a heartbeat monitoring system where agents send heartbeats every 30 seconds to the central platform. The first heartbeat is recorded at agent registration (setting the target to ONLINE). A `@Scheduled` task on central checks every 30 seconds if any agent has been silent for more than 2 minutes and marks it UNRESPONSIVE (target OFFLINE). On the agent side, a dedicated `@Scheduled` component calls the heartbeat endpoint every 30 seconds.

## Technical Context

**Language/Version**: Java 17 (both modules)
**Primary Dependencies**: Spring Boot 3.1.3 (api/), Spring Boot 3.5.7 (agents/unix/), Spring Data MongoDB (api/), Spring RestTemplate (agents/unix/)
**Storage**: MongoDB — `agents` collection (lastConnection field), `targets` collection (status field)
**Testing**: spring-boot-starter-test + Mockito (api/), JUnit 5 + Mockito + AssertJ (agents/unix/)
**Target Platform**: Linux server (api/), Linux/macOS native binary via GraalVM (agents/unix/)
**Project Type**: Web service (api/) + CLI daemon (agents/unix/)
**Performance Goals**: Scheduler cycle completes within 30 seconds for up to 500 agents
**Constraints**: Scheduler must not block or degrade other API endpoints; agent heartbeat must not block plan polling
**Scale/Scope**: Hundreds of agents across multiple organizations and projects

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md`
- English-only rule satisfied: all code, comments, identifiers in English
- Proposed design is the smallest correct change: leverages existing `lastConnection` field, existing `sendHeartbeat()` HTTP client method, existing status enums
- Stack rules captured:
  - `api/`: `@EnableScheduling` on main class, new `@Service` with `@Scheduled`, Spring Data derived query for unscoped lookup
  - `agents/unix/`: new `@Component` with `@Scheduled(fixedDelay = 30000)`, uses existing `AgentHttpClient`, GraalVM-compatible (no reflection-based scheduling)
- Verification steps identified:
  - `api/`: unit test for `AgentHeartbeatMonitorService` with mocked repositories
  - `agents/unix/`: unit test for `HeartbeatSender` with mocked HTTP client
- Git actions identified; explicit user approval required before any git command runs
- All requirements resolved from spec — no unknowns

## Project Structure

### Documentation (this feature)

```text
specs/011-agent-heartbeat-monitor/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── heartbeat-api.md # Heartbeat endpoint contract (already exists, enhanced)
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
api/
└── src/main/java/com/spulido/tfg/
    ├── WsApplication.java                          # Add @EnableScheduling
    └── domain/agent/
        ├── db/AgentRepository.java                  # Add unscoped query for stale agents
        ├── services/
        │   └── impl/AgentCommunicationServiceImpl.java # Already handles ACTIVE restore on heartbeat
        └── monitoring/                               # NEW package
            ├── AgentHeartbeatMonitorService.java     # @Scheduled - evaluates all agents
            └── HeartbeatConfig.java                  # Optional config properties

agents/unix/
└── src/main/java/com/spulido/agent/
    ├── heartbeat/                                     # NEW package
    │   └── HeartbeatSender.java                       # @Scheduled - sends heartbeat every 30s
    └── config/AgentConfig.java                        # Add heartbeat interval property
```

**Structure Decision**: Two-module approach matching existing architecture. Central gets a new `monitoring` package under the existing `agent` domain. Agent gets a dedicated `heartbeat` package. No cross-module dependencies.

## Complexity Tracking

> No constitution violations. Design uses smallest correct change: new `@Scheduled` services in both modules, one new repository query method, no new entities or collections.
