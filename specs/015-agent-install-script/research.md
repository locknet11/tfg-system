# Research: Agent Self-Installing Shell Script

**Feature**: 015-agent-install-script  
**Date**: 2026-07-08

## Research Questions & Findings

### RQ1: How does the agent binary download work currently?

**Decision**: Use a new public endpoint `/api/agent/binary/download/{installToken}` with a short-lived, one-time token generated during agent registration.

**Rationale**:
- Existing `/api/agent/binary/{replicationToken}` (AgentBinaryController) is for the replication flow and uses replication tokens with their own lifecycle (authored by an admin, tied to replication requests).
- Existing `/api/agent/download/{platform}` (AgentDownloadController) requires user authentication — unavailable on target machines.
- The install script needs a URL that is:
  - Publicly accessible (no user auth)
  - Authorized by a token carried in the script
  - Short-lived to prevent reuse
- A new endpoint matches the existing security pattern: `/api/agent/binary/**` is already `.permitAll()` in WebSecurity config.
- The token is generated in `AgentServiceImpl.registerAgent()` alongside the API key, stored on the Agent entity, and invalidated after first use or expiry.

**Alternatives considered**:
1. Reuse the replication token endpoint — rejected because the replication request has a different lifecycle (created by admin, read by requesting agent) and would require creating a "dummy" replication request for every install, coupling unrelated flows.
2. Include the binary directly in the install script as base64 — rejected because it would bloat the script (30MB base64 ~= 40MB text), making curl-bash-pipe impractical and slow.
3. Use the preauth code as a download token — rejected because preauth codes are long-lived (stored on Target) and reusing them for binary download weakens security separation.

### RQ2: Does agent-to-platform authentication work?

**Finding: NOT FULLY WORKING — this is a blocking gap.**

**Details**:
- `AgentApiKeyFilter` (in `api/`) reads `X-Agent-Api-Key` and `X-Agent-Id` headers from requests to `/api/agent/comm/**` and authenticates the agent.
- The agent's `RestTemplate` (created in `WorkerPoolConfig`) is a plain `new RestTemplate()` with **no interceptor** to add these headers.
- The install scripts (`install-agent-http.sh.tmpl`, `install-agent-transfer.sh.tmpl`) write `agent.preauth-code` to the properties file, but:
  - `AgentConfig` has **no** `preauthCode` property (only `apiKey`, `agentId`, `centralUrl`, `centralPublicKey`).
  - The preauth code is never used by the agent binary itself.
- The agent binary loads `agent.api-key` and `agent.agent-id` from its properties/environment, but the install scripts never write these values.
- **Result**: agents installed via any flow cannot authenticate to the central platform.

**Fix required**:
1. **Agent side**: Add a `ClientHttpRequestInterceptor` to `RestTemplate` in `WorkerPoolConfig` that adds `X-Agent-Api-Key` and `X-Agent-Id` headers from `AgentConfig`.
2. **Install scripts** (all templates): Write `agent.api-key` and `agent.agent-id` instead of (or in addition to) `agent.preauth-code`.

**Decided**: Include both fixes in this feature scope. The install script cannot "just work" without the agent being able to authenticate.

### RQ3: What signature verification approach should the install script use?

**Decision**: Reuse the existing Blake3 + RSA verification logic from `install-agent-http.sh.tmpl` (agents/unix) in the API's `unix.sh.ftl`.

**Rationale**:
- The agent's `BinaryIntegrityVerifier` (Java) verifies Blake3 hash + RSA signatures using the central public key.
- The existing shell template `install-agent-http.sh.tmpl` already implements this in shell: parses manifest JSON from end of binary, computes Blake3 via openssl/b3sum, verifies RSA signature via openssl.
- For the API install script, embed the same logic. The manifest is embedded in the downloaded binary (last line after `\n`), so the download response is self-contained.
- Tools required: `openssl` (ubiquitous), `b3sum` (less common) — fall back to `openssl dgst -blake3` where available, else warn and skip.

**Alternatives considered**:
1. Separate manifest download — rejected because it adds a second HTTP request and coupling; the embedded manifest approach is already proven in the existing replication flow.
2. SHA256 instead of Blake3 — rejected because the existing infrastructure (BinaryIntegrityService) is built around Blake3 and changing it would create inconsistency.
3. Skip signature verification in shell script — rejected per user requirement: "we should find a way to validate the signature of the agent binary."

### RQ4: POSIX background process approach

**Decision**: Use `nohup /tmp/agent > /tmp/agent.log 2>&1 &` with a brief `sleep 2 && kill -0 $PID` check.

**Rationale**:
- `nohup` + `&` is standard POSIX (IEEE Std 1003.1) and works on virtually every Linux distribution.
- `disown` is a bash builtin, not POSIX — avoid it for compatibility with `sh` (dash, busybox ash).
- The existing `install-agent-http.sh.tmpl` already uses this pattern and it's proven.
- A 2-second sleep and PID check gives quick feedback without blocking the install.

**Alternatives considered**:
1. `systemd` service — rejected; not available on all distros and requires root.
2. `screen`/`tmux` — rejected; not installed by default.
3. `daemonize` command — rejected; not standard.

### RQ5: What configuration must the agent binary receive?

**Decision**: The install script must write a properties file with:

| Property | Source | Purpose |
|----------|--------|---------|
| `agent.central-url` | `apiUrl` template var | Where the agent connects |
| `agent.api-key` | `apiKey` (generated during registration) | Authenticates agent to platform |
| `agent.agent-id` | `agentId` (generated during registration) | Identifies agent to platform |
| `agent.central-public-key` | Central's public key | Verifies binary/manifest signatures |

**Rationale**:
- These exactly match `AgentConfig`'s `@ConfigurationProperties(prefix = "agent")` properties.
- The user also mentioned ORG, project, and target identifiers. These are NOT current `AgentConfig` properties but are available in the template variables (`organizationIdentifier`, `projectIdentifier`, `targetUniqueId`). The template can write them for future use, but the agent currently identifies org/project/target via the API key (which is scoped to them).
- Added `organizationIdentifier`, `projectIdentifier`, and `targetUniqueId` as additional properties for traceability.

### RQ6: Existing binary download infrastructure

**Finding**: `AgentBinaryServiceImpl` already loads binaries from classpath or filesystem, computes Blake3 hashes, signs them, and serves them with manifests. The binary is served as `[binary bytes]\n[manifest JSON]`.

**Decision**: Reuse `AgentBinaryService` in the new install download endpoint. The existing binary loading, hashing, and signing infrastructure is complete and doesn't need changes.
