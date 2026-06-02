# Implementation Plan: Agent Autoreplication

**Branch**: `004-agent-autoreplication` | **Date**: 2026-06-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/004-agent-autoreplication/spec.md`

## Summary

Extend the existing agent-Central communication system to support autonomous agent self-replication. An active agent detects an exploitable vulnerability on an unregistered target, requests approval from Central (evaluated against a configurable per-project replication policy), executes the exploit to establish remote access, downloads the agent binary from Central (with Blake3+PKI integrity verification), installs and launches it on the target, and registers the new agent with Central. The implementation spans all three modules: new domain package and endpoints in `api/`, new step handlers and HTTP methods in `agents/unix/`, and a new Replication Requests page plus project settings extension in `ui/`.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.1.3 for API, Spring Boot 3.5.7 for agent), TypeScript (Angular 17)
**Primary Dependencies**: Spring Boot Web, Spring Data MongoDB, RestTemplate, Lombok, PrimeNG, Bouncy Castle (PKI signing), Blake3 (via `b3` Java library or custom impl)
**Storage**: MongoDB — new `replication_requests` collection, extended `agents` and `projects` documents
**Testing**: Spring Boot Test + Mockito (API), JUnit 5 (agent), Jasmine/Karma (UI)
**Target Platform**: Linux server (API + agent native binary), Web browser (UI)
**Project Type**: Web application (API + agent + frontend) — multi-module: `api/`, `agents/unix/`, `ui/`
**Performance Goals**: Replication cycle <3 min (AUTO_APPROVE), admin UI loads <3s for 500 requests, new agent registers within 30s of launch
**Constraints**: Replication token TTL 5 min, Blake3 hash + PKI signature required before binary execution, CommandExecutor must be implemented (currently a stub), reverse shell mechanism is an implementation prerequisite
**Scale/Scope**: One new domain package (`domain/replication`), 6 new endpoints, 4 new StepAction values, 3 new agent step handlers, 1 new UI page, project settings extension, alert system extension

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` and relevant `.agents/skills/*/SKILL.md` — Java/Spring Boot and Angular conventions apply
- [x] English-only rule satisfied for code, UI text, docs, and comments
- [x] Proposed design is the smallest correct change — adds one domain package, extends existing agent/project models, adds one UI page, extends project settings
- [x] Stack rules captured: constructor injection via Lombok, DTO boundaries, `@ControllerAdvice` for error handling, existing API key auth for agents, Angular standalone components with `$localize`, PrimeNG tables/dialogs
- [x] Verification steps identified: unit tests for ReplicationRequestService, ReplicationPolicyService, BinaryIntegrityService; integration tests for agent replication flow; UI tests for replication requests page
- [x] Git actions identified: no git commands auto-executed — explicit user approval required
- [x] Unknown or ambiguous requirements resolved: binary integrity mechanism (Blake3+PKI), reverse shell approach, binary serving strategy, policy evaluation logic

## Project Structure

### Documentation (this feature)

```text
specs/004-agent-autoreplication/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research decisions
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
├── contracts/           # Phase 1 API contracts
│   └── api-contracts.md
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
api/src/main/java/com/spulido/tfg/
├── domain/replication/                                    # NEW domain package
│   ├── controller/
│   │   ├── ReplicationRequestController.java              # Agent-facing endpoints (POST request, GET status)
│   │   ├── ReplicationAdminController.java                # Admin-facing endpoints (GET list, PUT approve/deny)
│   │   └── AgentBinaryController.java                     # Binary download endpoint (GET binary/{token})
│   ├── db/
│   │   └── ReplicationRequestRepository.java              # MongoDB repository
│   ├── model/
│   │   ├── ReplicationRequest.java                        # MongoDB document
│   │   ├── ReplicationRequestStatus.java                  # Enum: PENDING, APPROVED, DENIED, EXPIRED
│   │   ├── ReplicationPolicy.java                         # Value object (embedded in Project)
│   │   ├── ReplicationApprovalMode.java                   # Enum: AUTO_APPROVE, MANUAL_APPROVE
│   │   ├── BinaryManifest.java                            # Value object (hash + signature)
│   │   └── dto/
│   │       ├── CreateReplicationRequest.java              # Agent request DTO
│   │       ├── ReplicationRequestResponse.java            # Central response to agent
│   │       ├── ReplicationRequestInfo.java                # Admin list item DTO
│   │       ├── ReplicationRequestsList.java               # Paginated list wrapper
│   │       └── ReplicationStatusResponse.java             # Polling status response
│   ├── exception/
│   │   └── ReplicationException.java                      # Domain exception
│   └── services/
│       ├── ReplicationRequestService.java                 # Interface: create, approve, deny, expire, find
│       ├── ReplicationPolicyService.java                  # Interface: evaluate policy for a request
│       ├── BinaryIntegrityService.java                    # Interface: hash binary, sign hash, verify integrity
│       ├── AgentBinaryService.java                        # Interface: serve binary + manifest
│       ├── ReplicationRequestServiceMapper.java           # Mapper component
│       └── impl/
│           ├── ReplicationRequestServiceImpl.java         # Business logic
│           ├── ReplicationPolicyServiceImpl.java           # Policy evaluation
│           ├── BinaryIntegrityServiceImpl.java            # Blake3 + PKI implementation
│           └── AgentBinaryServiceImpl.java                # Binary serving
├── domain/agent/model/
│   └── Agent.java                                         # MODIFY — add replicatedFrom, replicatedAt, replicationExploit
├── domain/agent/model/dto/
│   └── AgentInfo.java                                     # MODIFY — add replication metadata fields
├── domain/plan/model/
│   └── StepAction.java                                    # MODIFY — add REQUEST_REPLICATION, EXECUTE_EXPLOIT, TRANSFER_AGENT, REPLICATE
├── domain/project/model/
│   └── Project.java                                       # MODIFY — add replicationPolicy field
├── domain/project/model/dto/
│   ├── ProjectInfo.java                                   # MODIFY — add replicationPolicy
│   ├── UpdateProjectRequest.java                          # MODIFY — add replicationPolicy
│   └── UpdateReplicationPolicyRequest.java                # NEW — dedicated policy update DTO
├── domain/alerts/model/
│   └── WhenCondition.java                                 # MODIFY — add ON_REPLICATION_REQUESTED, ON_AGENT_REPLICATED
├── config/security/
│   └── WebSecurity.java                                   # MODIFY — permitAll for binary download endpoint
└── common/exception/
    └── ErrorCode.java                                     # MODIFY — add replication error codes

agents/unix/src/main/java/com/spulido/agent/
├── domain/task/
│   └── StepAction.java                                    # MODIFY — add REQUEST_REPLICATION, EXECUTE_EXPLOIT, TRANSFER_AGENT, REPLICATE
├── worker/step/
│   ├── RequestReplicationStepHandler.java                 # NEW — POST replication request, poll status
│   ├── ExecuteExploitStepHandler.java                     # NEW — execute exploit, establish reverse shell
│   └── TransferAgentStepHandler.java                      # NEW — download binary+manifest, verify integrity, install, launch
├── worker/http/
│   ├── AgentHttpClient.java                               # MODIFY — add replication request, status poll, binary download methods
│   └── dto/
│       ├── ReplicationRequestBody.java                    # NEW — request DTO sent to Central
│       ├── ReplicationRequestResponse.java                # NEW — response DTO from Central
│       └── ReplicationStatusResponse.java                 # NEW — polling response DTO
├── worker/
│   └── BinaryIntegrityVerifier.java                       # NEW — Blake3 hash + PKI signature verification
└── config/
    └── AgentConfig.java                                   # MODIFY — add central-public-key property

ui/src/app/pages/
├── replication-requests/                                  # NEW page
│   ├── data-access/
│   │   ├── replication-requests.service.ts                # HTTP service
│   │   └── replication-requests.model.ts                  # TypeScript interfaces
│   └── feature/
│       ├── replication-requests.component.ts              # List component with p-table
│       └── replication-requests.component.html            # Template
├── agents/data-access/
│   └── agents.model.ts                                    # MODIFY — add StepAction values, replication metadata
├── agents/feature/
│   └── agents-list/agents-list.component.ts               # MODIFY — add replication badge column
├── templates/data-access/
│   └── templates.model.ts                                 # MODIFY — add StepAction values
├── project-selector/data-access/
│   ├── projects.model.ts                                  # MODIFY — add ReplicationPolicy interface
│   └── project.service.ts                                 # MODIFY — add updateReplicationPolicy method
├── project-selector/feature/
│   └── project-selector.component.ts                      # MODIFY — add replication policy settings section
└── alerts/data-access/
    └── alerts.model.ts                                    # MODIFY — add replication WhenCondition values

ui/src/app/
└── app-routing.module.ts                                  # MODIFY — add replication-requests route
```

**Structure Decision**: Follows the existing `domain/{feature}/` pattern in the API module (like `exploitation/`, `vulnerability/`, `alerts/`). The replication domain is split into agent-facing and admin-facing controllers to maintain clear auth boundaries (`hasRole('AGENT')` vs `isAuthenticated()`). The agent module extends the existing `step/` handler pattern from feature 003. UI adds one new page following the existing `data-access/` + `feature/` convention and extends existing project settings.

## Complexity Tracking

No constitution violations. No complexity justifications needed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| New `domain/replication` package | Replication is a distinct domain concept with its own lifecycle, policy engine, and binary serving — separate from agent management | Embedding replication logic in `domain/agent` would conflate agent lifecycle management with a separate approval/exploitation/installation workflow |
| Separate admin vs agent controllers | Agent endpoints use `hasRole('AGENT')` auth, admin endpoints use `isAuthenticated()` — mixing them would require complex per-method auth | A single controller with mixed auth annotations would be harder to secure and audit |
| Dedicated `BinaryIntegrityService` | Blake3+PKI is a cross-cutting security concern that may be reused for other binary distribution scenarios | Inlining hash/sign logic in the replication service would make it harder to test and evolve independently |
| `ReplicationPolicy` as embedded value object in Project | Policy is tightly coupled to a project's configuration and changes infrequently | A separate collection would add unnecessary query complexity for a 1:1 relationship |
