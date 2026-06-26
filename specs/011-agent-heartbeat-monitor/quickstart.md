# Quickstart: Agent Heartbeat Monitor

**Date**: 2026-06-26  
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Purpose

This quickstart explains how to run, test, and verify the heartbeat monitoring feature locally.

## Prerequisites

- Java 17+ installed
- MongoDB running locally on `localhost:27017` (or set `MONGODB_URI`)
- Node.js 18+ (for Angular UI, optional)

## Running the Central Platform (api/)

```bash
cd api
./mvnw spring-boot:run
```

The central platform starts on port `8080`. The heartbeat monitoring scheduler automatically activates via `@EnableScheduling`.

**Verify scheduling is active**: Check logs for:
```
Heartbeat monitor scheduled - checking agents for stale heartbeats
```

---

## Running the Agent (agents/unix/)

```bash
cd agents/unix
AGENT_API_KEY=<your-key> AGENT_ID=<your-agent-id> ./mvnw spring-boot:run
```

The agent starts, polls central for plans every 10s, and sends heartbeats every 30s.

**Verify heartbeat is active**: Check logs for:
```
Sending heartbeat to central: http://localhost:8080/api/agent/comm/heartbeat
```

---

## Local End-to-End Verification

### 1. Register an Agent

Via the UI or API, create a target and register an agent against it. Verify the target shows **ONLINE** status.

### 2. Confirm Heartbeats Flow

Wait ~30 seconds. Check central logs for heartbeat processing. The `Agent.lastConnection` is updated in MongoDB.

### 3. Simulate Agent Going Offline

Stop the agent process. Wait **2.5 minutes** (2-minute threshold + 30-second scheduler interval).

Verify:
- Agent status changes to **UNRESPONSIVE**
- Target status changes to **OFFLINE**

### 4. Simulate Agent Recovery

Restart the agent. It sends a heartbeat within 30 seconds.

Verify:
- Agent status returns to **ACTIVE**
- Target status returns to **ONLINE**

---

## Running Tests

### Central Platform (api/)

```bash
cd api
./mvnw test
```

Tests include:
- `AgentHeartbeatMonitorServiceTest` — verifies stale detection, status transitions, exclusion of KILLED/IN_CREATION agents

### Agent (agents/unix/)

```bash
cd agents/unix
./mvnw test
```

Tests include:
- `HeartbeatSenderTest` — verifies periodic heartbeat calls and error handling

---

## Building Native Binary (Agent - macOS)

```bash
cd agents/unix
sh package-macos.sh
```

---

## Configuration Reference

### Central (api/) — application.yml

| Property | Default | Description |
|----------|---------|-------------|
| `heartbeat.timeout.seconds` | `120` | Seconds before an agent with no heartbeat is marked stale |
| `heartbeat.scheduler.delay.ms` | `30000` | Delay between scheduler cycles in milliseconds |

### Agent (agents/unix/) — application.yml

| Property | Default | Description |
|----------|---------|-------------|
| `agent.heartbeat.interval.ms` | `30000` | Interval between heartbeats in milliseconds |

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| No heartbeat logs on agent | `@Scheduled` not triggering | Verify `@EnableScheduling` on `AgentApplication` |
| Target stays ONLINE after agent stops | Scheduler not running | Verify `@EnableScheduling` on `WsApplication` (api/) |
| Agent stuck in UNRESPONSIVE after restart | Heartbeat not reaching central | Check agent API key, network connectivity, central logs |
| Scheduler runs but doesn't update targets | Missing `findByAssignedAgent` lookup | Verify target recovery logic in `updateHeartbeat()` |
