# Research: Agent Autoreplication

**Feature**: 004-agent-autoreplication | **Date**: 2026-06-01

## R-001: Blake3 Hashing Library for Java

**Decision**: Use `software.amazon.awssdk.crt` Blake3 or implement via `org.bouncycastle` SHA-3/Keccak as fallback. Prefer the `com.github.luben:zstd-jni` pattern — a JNI-backed library. Evaluate `io.github.nicholasmata:blake3-java` or write a thin wrapper around the reference C implementation via JNI/JNA.

**Rationale**: Blake3 is not in the Java standard library or Bouncy Castle. The most reliable approach for a Spring Boot application is to use a well-maintained Java Blake3 library. If no suitable pure-Java library exists at implementation time, a JNI wrapper around the official Blake3 C reference implementation is the fallback.

**Alternatives considered**:
- SHA-256 (standard Java): Rejected — spec explicitly requires Blake3 for its speed and security properties
- Bouncy Castle SHA-3: Rejected — SHA-3 is not Blake3; different algorithm
- External CLI tool (`b3sum`): Rejected — adds external dependency on target hosts

## R-002: PKI Key Pair and Signing Mechanism

**Decision**: Use RSA-2048 or Ed25519 key pairs via Bouncy Castle (`org.bouncycastle:bcprov-jdk18on`). Central loads private key from environment variable (`REPLICATION_PRIVATE_KEY`), agents load public key from environment variable (`CENTRAL_PUBLIC_KEY`). Keys are PEM-encoded. Signing uses `SHA256withRSA` or `Ed25519` signature algorithm.

**Rationale**: Bouncy Castle is the de facto Java crypto library. PEM format is human-readable and easy to inject via environment variables. Ed25519 is preferred for its smaller key size and faster verification, but RSA-2048 is more universally supported. The choice depends on the Blake3 library's compatibility.

**Alternatives considered**:
- Java KeyStore (JKS): Rejected — harder to inject via environment variables, requires file-based storage
- Self-signed X.509 certificates: Rejected — overkill for hash signing; raw key pairs are simpler
- HMAC (shared secret): Rejected — symmetric; any agent with the secret could forge signatures

## R-003: Binary Serving Strategy

**Decision**: Central serves the agent binary from a configurable filesystem path (`agent.binary.path` property). The binary is read once at startup (or on first request) and cached in memory. The Blake3 hash is computed and signed at startup. Both the binary bytes and the signed manifest (hash + signature) are served via the same token-authenticated endpoint.

**Rationale**: The agent binary is a static artifact that changes only on deployment. Caching it in memory avoids repeated disk I/O. Computing the hash and signature at startup ensures they are always consistent with the served binary.

**Alternatives considered**:
- Store binary in MongoDB GridFS: Rejected — unnecessary complexity for a file that belongs on the filesystem
- Store binary in S3/object storage: Rejected — adds external dependency; Central should be self-contained
- Compute hash on every request: Rejected — wasteful; the binary is immutable between deployments

## R-004: Replication Token Design

**Decision**: UUID v4 generated server-side when a replication request is approved. Stored in the `ReplicationRequest` document. The token has a 5-minute TTL (stored as `expiresAt`). The binary download endpoint validates the token and checks expiration. Token is single-use — invalidated after successful download.

**Rationale**: UUID v4 provides sufficient randomness (122 bits of entropy). Server-side generation ensures the token is never exposed to the agent before approval. The 5-minute TTL limits the window of exposure if a token is intercepted.

**Alternatives considered**:
- JWT token: Rejected — overkill for a simple bearer token; no claims needed beyond expiration
- HMAC-signed token: Rejected — adds complexity without meaningful security benefit over UUID + TTL
- No expiration: Rejected — security risk; tokens would be valid indefinitely

## R-005: Reverse Shell Mechanism

**Decision**: Use SSH as the reverse shell mechanism. The exploit script (received from EXPLOITATION_KNOWLEDGE) is expected to establish SSH access to the target. The agent uses the SSH session to execute commands on the target (download binary, configure, launch). SSH is assumed to be available on target Linux hosts.

**Rationale**: SSH is universally available on Linux systems, provides encrypted communication, and supports command execution without an interactive session. The exploit script is responsible for setting up SSH access (e.g., copying an SSH key or using password-based auth).

**Alternatives considered**:
- Netcat reverse shell: Rejected — unencrypted, less reliable, no built-in file transfer
- Custom TCP protocol: Rejected — unnecessary complexity; SSH provides everything needed
- Metasploit Meterpreter: Rejected — heavy dependency, not suitable for a security monitoring agent

## R-006: CommandExecutor Implementation

**Decision**: Implement `CommandExecutor` using `ProcessBuilder` with configurable timeout. Capture stdout and stderr. Return `TaskResult` with success/failure based on exit code. This is a prerequisite for all step handlers that execute commands.

**Rationale**: `ProcessBuilder` is the standard Java mechanism for spawning subprocesses. It supports timeout, stream capture, and working directory configuration. The current stub throws `UnsupportedOperationException` — a real implementation is required for any command execution.

**Alternatives considered**:
- Apache Commons Exec: Rejected — adds dependency for functionality that `ProcessBuilder` handles well
- JNA/JNI native calls: Rejected — unnecessary complexity and platform coupling

## R-007: Replication Policy Evaluation

**Decision**: Policy evaluation is a synchronous check during replication request creation. The `ReplicationPolicyService` loads the project's policy, checks the mode (AUTO_APPROVE vs MANUAL_APPROVE), and if AUTO_APPROVE, checks the severity threshold. If the request meets the criteria, it is auto-approved. Otherwise, it falls back to MANUAL_APPROVE.

**Rationale**: Synchronous evaluation keeps the agent's polling loop simple — it gets an immediate APPROVED or PENDING response. The severity threshold check is a simple string comparison (CRITICAL > HIGH > MEDIUM > LOW).

**Alternatives considered**:
- Async evaluation with callback: Rejected — adds complexity; the agent already polls for status
- Rule engine (Drools): Rejected — overkill for a simple mode + threshold check

## R-008: Agent Binary Configuration Injection

**Decision**: The generic binary is downloaded without embedded configuration. After download and integrity verification, the agent writes a configuration file (`application.properties` or environment variables) alongside the binary on the target. The configuration includes `agent.central-url`, `agent.api-key` (the new agent's API key, generated by Central and returned in the approval response), and `agent.agent-id`.

**Rationale**: Separating the binary from its configuration allows the same binary to be deployed across different environments. The configuration is injected post-download via the reverse shell, keeping the binary generic and the configuration secure.

**Alternatives considered**:
- Embed configuration in binary at build time: Rejected — requires a separate build per agent instance
- Pass configuration via command-line arguments: Rejected — visible in process listings; file-based config is more secure
- Fetch configuration from Central on first boot: Rejected — adds complexity; the parent agent already has the config and can inject it

## R-009: Duplicate Replication Detection

**Decision**: Central checks for existing PENDING or APPROVED replication requests with the same `targetIp` + `exploitId` combination before creating a new request. If a match is found, Central returns a `DUPLICATE` status (mapped to the existing request's status). The requesting agent treats DUPLICATE the same as DENIED.

**Rationale**: Prevents multiple agents from simultaneously exploiting the same target, which could cause conflicts or resource exhaustion. The `targetIp + exploitId` combination is the natural deduplication key.

**Alternatives considered**:
- Distributed lock: Rejected — overkill; a simple query check is sufficient
- Allow concurrent replication: Rejected — risks conflicts and wasted resources

## R-010: Pre-authorization Code for Replicated Agents

**Decision**: Central generates a new pre-authorization code (same mechanism as existing target preauthCode) when approving a replication request. This code is included in the approval response and injected into the target's configuration by the parent agent. The new agent uses this code during registration via the existing `POST /api/agent/{orgId}/{projId}/{targetId}` endpoint.

**Rationale**: Reuses the existing registration flow without modification. The preauthCode mechanism already handles one-time use and target binding. Central creates or updates the Target document with the preauthCode before approving the replication request.

**Alternatives considered**:
- New dedicated replication registration endpoint: Rejected — duplicates existing functionality
- API key pre-generation: Rejected — the existing flow generates the API key during registration
