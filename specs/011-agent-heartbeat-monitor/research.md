# Research: Agent Heartbeat Monitor

**Date**: 2026-06-26  
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Research Task 1: Spring @Scheduled for Periodic Agent Heartbeat

**Decision**: Use `@Scheduled(fixedDelay = 30000)` on a dedicated `HeartbeatSender` component in the agent module.

**Rationale**: The agent already uses `@EnableScheduling` and `@Scheduled` (in `WorkerCoordinator` for plan polling). Using the same mechanism for heartbeats is consistent, simple, and GraalVM-native safe. A separate component isolates heartbeat concerns from plan polling, avoids coupling scheduling intervals, and keeps each concern independently testable.

**Alternatives considered**:
- **Embedding heartbeat in WorkerCoordinator's 10s poll loop**: Rejected — conflates two different intervals (10s poll vs 30s heartbeat), would require manual timing logic, and makes testing harder.
- **Using a separate Thread with `ScheduledExecutorService`**: Rejected — unnecessary complexity when Spring's `@Scheduled` already handles this.
- **Using `fixedRate` instead of `fixedDelay`**: Rejected — `fixedRate` measures from start-to-start, which can cause overlapping sends if a heartbeat request is slow. `fixedDelay` measures from end-to-start, preventing overlap.

---

## Research Task 2: Central Scheduler for Heartbeat Evaluation

**Decision**: Create a new `AgentHeartbeatMonitorService` in `api/` with `@Scheduled(fixedDelay = 30000)`. Add `@EnableScheduling` to `WsApplication`. Use a Spring Data derived query that bypasses `ProjectContext` scoping to find stale agents across all orgs/projects.

**Rationale**: The API module doesn't currently use `@EnableScheduling`. Adding it to the main application class is the standard Spring Boot approach. The scheduler needs an unscoped query because it runs outside any user/project context — it evaluates agents across the entire system. Spring Data derived query methods (without `@Query` SpEL) naturally bypass the `ProjectContext` filter.

**Alternatives considered**:
- **Using `MongoTemplate` with `Criteria` queries**: Rejected — Spring Data derived query is simpler and consistent with the repository pattern already in use. `MongoTemplate` would be an introduction of a new pattern without existing precedent in this codebase.
- **Creating a separate `@Scheduled` bean per organization/project**: Rejected — does not scale, adds complexity, and is unnecessary when the scheduler can query all agents at once.
- **Using a cron expression (`@Scheduled(cron = "*/30 * * * * *")`) instead of `fixedDelay`**: Rejected — `fixedDelay` is safer because it prevents overlapping executions if a cycle takes longer than expected. Cron expressions fire at absolute wall-clock times which can overlap.

---

## Research Task 3: Heartbeat Timeout Detection Mechanism

**Decision**: Use the existing `lastConnection` field on `Agent` entity. Compare `lastConnection` against `LocalDateTime.now().minusMinutes(2)` to determine staleness.

**Rationale**: The `lastConnection` field already exists on the `Agent` entity and is already being updated by the heartbeat endpoint (`AgentCommunicationServiceImpl.updateHeartbeat()`). Reusing it avoids schema changes. The 2-minute threshold is computed as `now - 2 minutes`, which with a 30-second heartbeat interval provides a comfortable margin (4 missed beats before timeout).

**Alternatives considered**:
- **Creating a separate `Heartbeat` collection/document**: Rejected — unnecessary data duplication. The `lastConnection` timestamp already tracks the same information.
- **Using Spring Data `@LastModifiedDate` auditing**: Rejected — this tracks entity modification time, not heartbeat communication time. An agent that sends a heartbeat but receives no plan update shouldn't cause entity mutation.

---

## Research Task 4: Repository Query for Stale Agent Detection

**Decision**: Add a derived query method to `AgentRepository`:

```
List<Agent> findByStatusInAndLastConnectionBefore(List<AgentStatus> statuses, LocalDateTime cutoff)
```

This finds agents whose status is in the candidate set (ACTIVE, CREATED) AND whose `lastConnection` is before the cutoff time. It bypasses `ProjectContext` scoping because derived queries without `@Query` SpEL annotations are not scope-filtered.

**Rationale**: This single query efficiently retrieves only the agents that might need status transitions. Excluding KILLED and IN_CREATION agents at the query level avoids unnecessary processing. The `UNRESPONSIVE` status is also excluded because those agents are already flagged — the scheduler should not re-evaluate them.

**Alternatives considered**:
- **Querying ALL agents and filtering in Java**: Rejected — wasteful for large agent counts. The database should do the filtering.
- **Using a separate `@Query` with explicit MongoDB query**: Partially viable but unnecessary — the derived query is sufficient and more readable.

---

## Research Task 5: Agent Recovery Flow

**Decision**: Leverage the existing code in `AgentCommunicationServiceImpl.updateHeartbeat()` which already restores an UNRESPONSIVE agent to ACTIVE. The central heartbeat endpoint already handles both the timestamp update and the status restoration. Additionally, we need to update the associated target's status from OFFLINE to ONLINE during recovery.

**Rationale**: The existing heartbeat handler already checks `if (agent.getStatus() == AgentStatus.UNRESPONSIVE)` and transitions to ACTIVE. We extend this logic to also update the target status to ONLINE. This keeps the recovery logic at the heartbeat endpoint level, making it immediately responsive (no need to wait for the next scheduler cycle).

**Impact**: The `updateHeartbeat` method needs to look up the associated target and set its status to ONLINE when recovering from UNRESPONSIVE.
