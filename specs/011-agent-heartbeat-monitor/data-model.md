# Data Model: Agent Heartbeat Monitor

**Date**: 2026-06-26  
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Overview

This feature does **not** introduce new entities or collections. It reuses existing entities with enhanced behavior:

- **Agent** entity — `lastConnection` field serves as heartbeat timestamp; `status` transitions to/from UNRESPONSIVE
- **Target** entity — `status` transitions between ONLINE and OFFLINE based on agent heartbeat health

## Existing Entities (Enhanced Behavior)

### Agent

```text
┌──────────────────────────────────────────────────────┐
│ Agent                                                │
├──────────────────────────────────────────────────────┤
│ id: String (MongoDB _id)                             │
│ name: String                                         │
│ status: AgentStatus  ←── ENHANCED: transitions       │
│   • ACTIVE → UNRESPONSIVE (scheduler detects stale)  │
│   • UNRESPONSIVE → ACTIVE (heartbeat received)       │
│   • KILLED: excluded from heartbeat evaluation       │
│   • IN_CREATION: excluded from heartbeat evaluation  │
│ version: String                                      │
│ lastConnection: LocalDateTime  ←── this IS the       │
│   heartbeat signal. Updated on every heartbeat.       │
│ organizationId: String                               │
│ projectId: String                                    │
│ apiKey: String (unique)                              │
│ assignedAgent: String (on Target)  ←── links back   │
└──────────────────────────────────────────────────────┘
```

**State Machine — Agent Status:**

```text
                    [ACTIVE]
                   /        \
     stale (>2min)           \ heartbeat received
                 /            \
        [UNRESPONSIVE]    [ACTIVE]
                |              ↑
                |  heartbeat   │
                └──────────────┘
                
    [KILLED] and [IN_CREATION] — excluded from evaluation
    [CREATED] — evaluated (may have stale heartbeat from registration)
```

### Target

```text
┌──────────────────────────────────────────────────────┐
│ Target                                               │
├──────────────────────────────────────────────────────┤
│ id: String (MongoDB _id)                             │
│ systemName: String                                   │
│ description: String                                  │
│ os: OperatingSystem                                  │
│ uniqueId: String (unique)                            │
│ organizationId: String                               │
│ projectId: String                                    │
│ ipOrDomain: String                                   │
│ status: TargetStatus  ←── ENHANCED: transitions      │
│   • OFFLINE → ONLINE (agent registers / heartbeat)   │
│   • ONLINE → OFFLINE (scheduler detects stale agent) │
│   • IN_REVIEW: not affected by heartbeat monitoring  │
│ assignedAgent: String  ←── links to Agent.id         │
│ preauthCode: String (unique)                         │
└──────────────────────────────────────────────────────┘
```

**State Machine — Target Status (heartbeat-related only):**

```text
    Agent registers          Scheduler detects
    (first heartbeat)        stale agent heartbeat
          │                       │
          ▼                       ▼
    [OFFLINE] ──────→ [ONLINE] ──────→ [OFFLINE]
                          ↑                │
                          │  heartbeat     │
                          │  recovery      │
                          └────────────────┘
                          
    [IN_REVIEW] — unaffected by heartbeat transitions
```

## Relationships

```text
Organization (1) ──── (*) Project (1) ──── (*) Agent (1) ──── (1) Target
                                        │ assignedAgent       │ assignedAgent
                                        │                     │
                                        └──── target.assignedAgent = agent.id
                                        └──── agent links via organizationId + projectId
```

## New Repository Query Methods

### AgentRepository (api/)

```text
// Unscoped — crosses all org/project boundaries
// Used by: AgentHeartbeatMonitorService (scheduler)
// Finds agents in candidate statuses with stale heartbeats
findByStatusInAndLastConnectionBefore(
    List<AgentStatus> statuses,  // [ACTIVE, CREATED]
    LocalDateTime cutoff         // now - 2 minutes
): List<Agent>
```

### TargetRepository (api/) — No new methods needed

Existing `findByAssignedAgent(String agentId)` is sufficient for looking up a target by its assigned agent ID during recovery and timeout transitions.

## Validation Rules

| Entity | Field | Rule |
|--------|-------|------|
| Agent | lastConnection | MUST be set to current time on registration AND on every heartbeat |
| Agent | status | MUST be ACTIVE, CREATED, or UNRESPONSIVE to be evaluated; KILLED and IN_CREATION are excluded |
| Target | status | MUST be ONLINE when agent is ACTIVE/has recent heartbeat; MUST be OFFLINE when agent is UNRESPONSIVE |
| Target | status | IN_REVIEW targets are NOT affected by heartbeat monitoring |

## Data Flow

```text
  Agent (unix)              Central (api/)                MongoDB
      │                          │                          │
      │   PUT /heartbeat         │                          │
      │─────────────────────────>│                          │
      │                          │   update lastConnection   │
      │                          │   restore if UNRESPONSIVE │
      │                          │─────────────────────────>│
      │   HeartbeatResponse       │                          │
      │<─────────────────────────│                          │
      │                          │                          │
      │    (@Scheduled 30s)      │    (@Scheduled 30s)      │
      │                          │                          │
      │                          │  query stale agents      │
      │                          │─────────────────────────>│
      │                          │  update agent+target     │
      │                          │─────────────────────────>│
```
