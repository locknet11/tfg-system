# Quickstart: Agent Self-Installing Shell Script

**Feature**: 015-agent-install-script  
**Date**: 2026-07-08

## Prerequisites

- Central platform running (`api/` started, e.g., `http://host.docker.internal:8080`)
- A registered Organization, Project, and Target with a valid preauth code
- Agent binary available on the platform (built via `cd agents/unix && ./mvnw clean package -Pnative`)
- A target Linux machine with curl or wget

## Quick Verification

### 1. Run the install command

```bash
curl -sSL -X POST \
  "http://host.docker.internal:8080/api/agent/{ORG}/{PROJ}/{TARGET}?preauthCode={CODE}" \
  | bash
```

### 2. Expected output

```
Installing security agent for target: {TARGET}
API URL: http://host.docker.internal:8080
Organization: {ORG}
Project: {PROJ}
Target: {TARGET}

=== Downloading agent binary ===
Agent binary downloaded: 31457280 bytes

=== Verifying Blake3 hash ===
Blake3 hash: OK (abc123...)

=== Verifying RSA signature ===
RSA signature: VERIFIED

=== Installing agent ===
Agent started (PID: 12345)
INSTALL_OK
```

### 3. Verify agent is running

```bash
ps aux | grep agent
cat /tmp/agent.log
```

### 4. Verify agent connected to platform

Check the central platform dashboard — the agent should appear as ACTIVE and connected.

## Test Scenarios

### Happy Path
1. `curl ... | bash` → agent downloads, verifies, launches
2. Agent appears in dashboard as ACTIVE
3. Agent sends heartbeat within 30 seconds

### Error: Invalid preauth code
```bash
curl -sSL -X POST "http://host.docker.internal:8080/api/agent/X/Y/Z?preauthCode=bad" | bash
# Expected: Error script with "Invalid preauth code"
```

### Error: No curl/wget
```bash
# On a minimal system without curl/wget:
curl ... | bash
# Expected: "FATAL: Neither curl nor wget available"
```

### Error: Hash mismatch
```bash
# Manually corrupt the downloaded binary in /tmp before verification completes
# Expected: "FATAL: Blake3 hash MISMATCH"
```

### Already installed
```bash
# Run install command on a machine that already has the agent running
# Expected: Warning about existing agent, or reinstall
```

## Build & Deploy

### Build the agent binary
```bash
cd agents/unix
./mvnw clean package -Pnative
# Binary at: agents/unix/target/agent
```

### Start the API with binary loaded
```bash
cd api
./mvnw spring-boot:run -Dspring-boot.run.arguments="--agent.binary.path=../agents/unix/target/agent"
```

## Files Created by Install Script

| Path | Content | Lifecycle |
|------|---------|-----------|
| `/tmp/agent_raw` | Downloaded binary + manifest | Deleted after extraction |
| `/tmp/agent` | Extracted agent binary | Persisted (the running agent) |
| `/tmp/agent.properties` | Agent configuration | Persisted |
| `/tmp/agent.log` | Agent stdout/stderr | Persisted (grows over time) |
| `/tmp/central_pubkey.pem` | Public key (temp) | Deleted after verification |
| `/tmp/hash_to_verify.bin` | Hash for sig verify (temp) | Deleted after verification |
| `/tmp/signature.bin` | Signature for verify (temp) | Deleted after verification |
