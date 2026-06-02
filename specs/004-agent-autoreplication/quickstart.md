# Quickstart: Agent Autoreplication

**Feature**: 004-agent-autoreplication | **Date**: 2026-06-01

## Prerequisites

1. **API module** running (`cd api && ./mvnw spring-boot:run`)
2. **Agent binary** compiled (`cd agents/unix && ./mvnw clean package` or `sh package-macos.sh`)
3. **UI module** running (`cd ui && npm start`)
4. **MongoDB** accessible (configured via `api/src/main/resources/application.properties`)
5. **PKI key pair** generated and configured:
   ```bash
   # Generate Ed25519 key pair (example)
   openssl genpkey -algorithm Ed25519 -out private.pem
   openssl pkey -in private.pem -pubout -out public.pem
   ```
   Set environment variables:
   - `REPLICATION_PRIVATE_KEY` — PEM-encoded private key (Central/API)
   - `CENTRAL_PUBLIC_KEY` — PEM-encoded public key (Agent)

## Environment Variables

### API (Central)

| Variable | Description | Default |
|----------|-------------|---------|
| `REPLICATION_PRIVATE_KEY` | PEM-encoded private key for signing binary hashes | (required) |
| `AGENT_BINARY_PATH` | Filesystem path to the agent binary to serve | `agents/unix/target/agent` |

### Agent

| Variable | Description | Default |
|----------|-------------|---------|
| `CENTRAL_PUBLIC_KEY` | PEM-encoded public key for verifying binary integrity | (required) |
| `AGENT_API_KEY` | Agent API key (set during registration) | (empty) |
| `AGENT_ID` | Agent identifier (set during registration) | (empty) |

## Verification Steps

### 1. API Build and Test
```bash
cd api
./mvnw clean package
./mvnw test
```

### 2. Agent Build and Test
```bash
cd agents/unix
./mvnw clean package
./mvnw test
```

### 3. UI Build
```bash
cd ui
npm ci
npm run build
npx prettier --check .
```

### 4. Integration Smoke Test

1. Start Central (API) with PKI keys configured
2. Register an agent on a target host
3. Assign a plan with steps: `SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE → REQUEST_REPLICATION → EXECUTE_EXPLOIT → TRANSFER_AGENT`
4. Set project replication policy to `AUTO_APPROVE` via UI or API
5. Verify the agent:
   - Completes scan steps
   - Submits replication request → receives APPROVED
   - Downloads binary + manifest
   - Verifies Blake3 hash against PKI signature
   - Installs and launches new agent on target
   - New agent registers with Central
6. Check UI:
   - Replication Requests page shows the request with APPROVED status
   - Agents list shows the new agent with replication badge
   - Admin received notification

### 5. Manual Approval Flow

1. Set project replication policy to `MANUAL_APPROVE`
2. Trigger a replication request from an agent
3. Verify the request appears as PENDING in the Replication Requests page
4. Click Approve
5. Verify the agent proceeds with exploitation and replication

## Key Files to Modify

| Module | File | Change |
|--------|------|--------|
| API | `StepAction.java` | Add 4 new enum values |
| API | `Agent.java` | Add replication metadata fields |
| API | `Project.java` | Add `replicationPolicy` field |
| API | `WhenCondition.java` | Add 2 replication event types |
| API | `WebSecurity.java` | Add permitAll for binary endpoint |
| API | `ErrorCode.java` | Add replication error codes |
| Agent | `StepAction.java` | Add 4 new enum values |
| Agent | `AgentHttpClient.java` | Add 3 new HTTP methods |
| Agent | `AgentConfig.java` | Add public key property |
| UI | `app-routing.module.ts` | Add replication-requests route |
| UI | `agents.model.ts` | Add StepAction values + replication fields |
| UI | `templates.model.ts` | Add StepAction values |
| UI | `projects.model.ts` | Add ReplicationPolicy interface |
| UI | `alerts.model.ts` | Add replication WhenCondition values |
