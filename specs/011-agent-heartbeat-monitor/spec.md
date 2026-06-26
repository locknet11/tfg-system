# Feature Specification: Agent Heartbeat Monitor

**Feature Branch**: `feature/011-agent-heartbeat-monitor`  
**Created**: 2026-06-26  
**Status**: Draft  
**Input**: User description: "Implementar un heartbeat para monitorear si un target está en línea o no. El agent envía heartbeats cada 30 segundos a un endpoint en central. El primer heartbeat se registra cuando el agente se registra. Si tras 2 minutos sin heartbeats, se marca el target como fuera de línea. Un scheduled en central revisa cada 30 segundos el estado de heartbeats de todos los agentes por cada proyecto de cada organización."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automatic Target Online Detection on Agent Registration (Priority: P1)

When an agent successfully registers with the centralized platform, the system records the first heartbeat for that agent and immediately marks the associated target as **ONLINE**. This ensures that from the moment an agent is alive on a target machine, the platform reflects that the target is reachable.

**Why this priority**: This is the foundation of the entire heartbeat monitoring feature. Without recording the initial heartbeat and setting the target online at registration time, there is no baseline for subsequent monitoring.

**Independent Test**: Can be fully tested by registering an agent against the platform and verifying that the associated target status transitions from OFFLINE to ONLINE, and that a heartbeat record with the current timestamp exists for the agent.

**Acceptance Scenarios**:

1. **Given** a target has been created and is in OFFLINE status, **When** an agent registers and associates with that target, **Then** the target status changes to ONLINE and a heartbeat record is created with the registration timestamp.
2. **Given** an agent has just registered, **When** an administrator views the target list, **Then** the associated target shows status ONLINE.

---

### User Story 2 - Continuous Heartbeat Reporting by Agent (Priority: P1)

Once an agent is registered and running, it sends a heartbeat signal to the centralized platform every 30 seconds. Each heartbeat updates the agent's last-seen timestamp in the central database. If the agent was previously marked as UNRESPONSIVE (due to missed heartbeats), receiving a new heartbeat restores the agent to ACTIVE and the target back to ONLINE.

**Why this priority**: This is the core monitoring mechanism. Without continuous heartbeats, the system cannot determine whether targets are alive or unreachable. This is equal in priority to User Story 1 because both are required for a functioning system.

**Independent Test**: Can be tested by registering an agent, simulating heartbeat calls at 30-second intervals, and verifying that the central database correctly updates the last-seen timestamp with each heartbeat.

**Acceptance Scenarios**:

1. **Given** an agent is ACTIVE and registered, **When** the agent sends a heartbeat, **Then** the central system updates the agent's last heartbeat timestamp to the current time.
2. **Given** an agent was previously marked UNRESPONSIVE (target OFFLINE), **When** the agent sends a new heartbeat, **Then** the agent status becomes ACTIVE and the associated target status becomes ONLINE.
3. **Given** an agent sends a heartbeat, **When** the platform responds, **Then** the response confirms the heartbeat was received and includes the agent's current status.

---

### User Story 3 - Automatic Target Offline Detection via Scheduled Check (Priority: P1)

The centralized platform runs a scheduled task every 30 seconds that evaluates all registered agents across every organization and project. For each agent, the scheduler checks the time elapsed since the last recorded heartbeat. If more than 2 minutes have passed without a heartbeat, the system marks the associated agent as UNRESPONSIVE and the target as OFFLINE. This ensures that administrators always have an accurate, near-real-time view of which targets are reachable.

**Why this priority**: This is the core value proposition of the feature — automatic detection of unreachable targets without human intervention. It depends on User Stories 1 and 2 being in place.

**Independent Test**: Can be tested by registering an agent, sending a heartbeat, then waiting (or simulating time passage) beyond the 2-minute threshold without sending further heartbeats, and verifying that the scheduler marks the agent as UNRESPONSIVE and the target as OFFLINE.

**Acceptance Scenarios**:

1. **Given** an agent's last heartbeat was more than 2 minutes ago, **When** the scheduled check runs, **Then** the agent status changes to UNRESPONSIVE and the associated target status changes to OFFLINE.
2. **Given** multiple agents across different organizations and projects, **When** the scheduled check runs, **Then** all agents whose last heartbeat exceeds the threshold are evaluated independently and their statuses are updated accordingly.
3. **Given** an agent's last heartbeat was less than 2 minutes ago, **When** the scheduled check runs, **Then** the agent status remains ACTIVE and the target status remains ONLINE.
4. **Given** an agent is already marked UNRESPONSIVE, **When** the scheduled check runs and the heartbeat is still stale, **Then** no redundant status change occurs (status remains UNRESPONSIVE).

---

### User Story 4 - Dashboard Visibility of Target Online/Offline Status (Priority: P2)

Administrators can see the current online/offline status of each target in the targets list view. The status reflects the real-time heartbeat state as determined by the monitoring system. This gives operators immediate visibility into infrastructure health.

**Why this priority**: While the backend monitoring is the core feature, the ability for users to see the status is what delivers actionable value. This depends on Stories 1-3 being implemented but provides the user-facing benefit.

**Independent Test**: Can be tested by creating targets with agents in various states (ACTIVE, UNRESPONSIVE) and verifying that the target list correctly displays ONLINE and OFFLINE statuses.

**Acceptance Scenarios**:

1. **Given** a target has an ACTIVE agent (recent heartbeat), **When** an administrator views the targets list, **Then** the target shows status ONLINE.
2. **Given** a target has an UNRESPONSIVE agent (no heartbeat within threshold), **When** an administrator views the targets list, **Then** the target shows status OFFLINE.

---

### Edge Cases

- What happens when an agent registers but immediately loses connectivity? → The target starts as ONLINE, then after 2 minutes without heartbeats, the scheduler marks it OFFLINE.
- What happens when the central platform is temporarily unavailable and agents cannot send heartbeats? → Agents continue retrying; when connectivity is restored, the platform processes heartbeats and restores ONLINE status.
- What happens when there are hundreds of agents across many organizations? → The scheduler processes all agents across all org/projects within its 30-second interval without missing the evaluation window.
- What happens if a target has no agent assigned yet? → The target remains OFFLINE since there is no agent to send heartbeats.
- What happens if the scheduler itself misses a cycle (platform restart, high load)? → The next scheduler run catches up; agents that have been silent for more than 2 minutes are correctly flagged.
- What happens when an agent is intentionally killed (KILLED status)? → The scheduler should not evaluate KILLED agents for heartbeat timeout; they remain KILLED.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST record the first heartbeat timestamp for an agent at the moment of agent registration.
- **FR-002**: System MUST provide an endpoint for agents to send heartbeat signals.
- **FR-003**: System MUST update the agent's last heartbeat timestamp upon receiving each heartbeat.
- **FR-004**: System MUST set the associated target status to ONLINE when the first heartbeat is recorded at agent registration.
- **FR-005**: System MUST restore the agent status to ACTIVE and the target status to ONLINE when a heartbeat is received from a previously UNRESPONSIVE agent.
- **FR-006**: System MUST run a scheduled evaluation every 30 seconds across all organizations and projects.
- **FR-007**: System MUST mark an agent as UNRESPONSIVE if no heartbeat has been received within 2 minutes.
- **FR-008**: System MUST mark the associated target as OFFLINE when its agent is marked UNRESPONSIVE.
- **FR-009**: System MUST NOT evaluate agents with KILLED status for heartbeat timeout.
- **FR-010**: System MUST NOT evaluate agents in IN_CREATION status for heartbeat timeout.
- **FR-011**: System MUST scope the scheduled check to iterate over all agents belonging to all projects of all organizations.

### Cross-Cutting Requirements

- **Validation and Error Handling**: The heartbeat endpoint must validate that the agent is authenticated and exists. If the agent is not found, the endpoint must return an appropriate error without crashing the scheduler.
- **Security Constraints**: Heartbeat endpoints must require agent authentication (API key). Only authenticated agents can send heartbeats for their own identity.
- **Performance**: The scheduled task must complete its evaluation cycle within the 30-second interval even under high agent counts. Database queries must be scoped and indexed for efficient retrieval.

### Key Entities *(include if feature involves data)*

- **Agent**: The autonomous unit deployed on a target machine. Key attributes: heartbeat timestamp (last seen), status (ACTIVE, UNRESPONSIVE, KILLED, IN_CREATION, CREATED), organization and project scope.
- **Target**: The machine or infrastructure being monitored. Key attributes: status (ONLINE, OFFLINE, IN_REVIEW), assigned agent, organization and project scope.
- **Heartbeat Record**: The conceptual record of an agent's last-seen timestamp. Represents when the agent last successfully communicated with the central platform.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A target is marked ONLINE within 5 seconds of its agent's first successful heartbeat at registration.
- **SC-002**: A target is marked OFFLINE within 2.5 minutes of its agent's last heartbeat (2-minute threshold + up to 30-second scheduler interval + processing margin).
- **SC-003**: The scheduled evaluation completes within 30 seconds for up to 500 agents across all organizations and projects.
- **SC-004**: An agent that recovers connectivity is reflected as ONLINE in the platform within 60 seconds of sending its next heartbeat.
- **SC-005**: 100% of agents with heartbeats exceeding the 2-minute threshold are correctly identified and transitioned to UNRESPONSIVE status in each scheduler cycle.

## Assumptions

- The existing heartbeat endpoint (`PUT /api/agent/comm/heartbeat`) will be enhanced to track heartbeat timing; no entirely new communication channel is needed.
- The agent-side already has the capability to send HTTP requests to the central platform; the 30-second interval will be configured on the agent's scheduler.
- The existing `AgentStatus` enum values (ACTIVE, UNRESPONSIVE, KILLED, IN_CREATION, CREATED) are sufficient; no new statuses are needed.
- The existing `TargetStatus` enum values (ONLINE, OFFLINE, IN_REVIEW) are sufficient; no new statuses are needed.
- The `lastConnection` field on the Agent entity serves as the heartbeat timestamp.
- IN_REVIEW target status is not affected by heartbeat monitoring (it is set for other reasons).
- The agent scheduling mechanism for heartbeats is independent from the existing job polling cycle (which polls for plans every 10 seconds).
- Agents with KILLED status have been intentionally terminated and should not be monitored for heartbeat timeouts.

## Constitution Notes

- This feature spans both the centralized API (`api/` - Spring Boot) and the agent binary (`agents/unix/` - Spring Boot + GraalVM native).
- The agent-side heartbeat sender must be compatible with GraalVM native image compilation.
- The scheduled task on central uses Spring's `@Scheduled` annotation as specified in the project context.
- All new templates/scripts must follow the resource template boundary rule (no inline string concatenation for scripts).
- The existing project scoping mechanism (`ScopedEntity`, `ProjectContext`) must be respected when iterating agents in the scheduler.
- No open questions identified that require answers before implementation.
