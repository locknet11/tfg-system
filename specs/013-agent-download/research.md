# Research: Agent Download Portal

**Feature**: 013-agent-download
**Date**: 2026-07-07

## Research Questions

### RQ1: How should the user-facing download endpoint differ from the replication endpoint?

**Decision**: Create a separate `/api/agent/download` endpoint under standard user authentication (JWT session), distinct from `/api/agent/binary/{replicationToken}` which uses token-based auth for agent-to-agent replication.

**Rationale**:
- The replication endpoint (`AgentBinaryController.downloadBinary`) is designed for short-lived tokens (5 min expiry) tied to replication requests, which auto-expire after single use. A user-facing download needs session-based auth, no consumption tracking, and no expiry.
- Separating the endpoints keeps replication logic isolated and prevents accidental token consumption by UI downloads.
- The new endpoint reuses `AgentBinaryService.getBinaryBytes()` and `getSignedManifest()` — the same binary + signed manifest served by the replication endpoint.

**Alternatives considered**:
- Adding a query parameter to the existing endpoint (`?auth=session`): rejected — conflates two auth models and complicates the single-use token logic.
- Making the replication endpoint accept JWT auth too: rejected — replication tokens are org-scoped; JWT sessions are user-scoped. Mixing auth models in one endpoint is error-prone.

### RQ2: How should the agent binary be loaded from API classpath?

**Decision**: Extend `AgentBinaryServiceImpl` to check two locations in order: (1) filesystem path configured by `agent.binary.path` (existing behavior, for dev), (2) classpath resources under `agents/{platform}/agent` (new behavior, for production deployments).

**Rationale**:
- The `@Value("${agent.binary.path:agents/unix/target/agent}")` currently points to the agent build output. In production or when the agent module isn't present, loading from classpath (`src/main/resources/agents/`) is needed.
- Using `ClassPathResource` + `StreamUtils.copyToByteArray()` is standard Spring Boot. The existing service already loads eagerly in `@PostConstruct`.
- A new config property `agent.binary.resource-path` (default: `agents`) controls the classpath root.

**Alternatives considered**:
- Serving from external storage (S3, etc.): rejected — over-engineering for a binary that's ~50-100 MB and changes infrequently.
- Embedding as a Maven dependency: rejected — the agent is a GraalVM native image, not a Java library.

### RQ3: How should the installation script verify binary integrity on the target?

**Decision**: Enhance `install-agent-http.sh.tmpl` to download two files: the binary and its manifest. The script computes the Blake3 hash locally (using `b3sum` if available, or a bundled helper), compares it to the manifest hash, then verifies the RSA signature against the embedded public key. If verification fails, the script aborts with a clear error.

**Rationale**:
- The current script just downloads and runs — no verification. The `BinaryIntegrityVerifier` on the agent side already does this, but that's for the parent agent validating the binary before transferring to the target (Path B).
- For Path A (HTTP download from target), the target itself must verify. The script needs to be self-contained.
- The manifest format (JSON with `blake3Hash`, `signature`, `algorithm`) is already defined by `AgentBinaryManifest` in the API. The install script downloads this manifest from a companion endpoint (`/api/agent/download/{platform}/manifest`).

**Manifest endpoint design**: The companion endpoint serves just the JSON manifest (no binary), allowing the install script to download manifest + binary as separate requests, then verify.

**Tools needed on target**:
- `curl` or `wget` (already required)
- `openssl` (for RSA signature verification — available on virtually all Linux distros)
- `b3sum` or a fallback (script can compute Blake3 with `openssl` too, or we provide a compact binary)
- **Decision**: Use `openssl dgst -blake3` (available in OpenSSL 3.0+) for hash computation, `openssl pkeyutl -verify` for signature verification. Both `curl` and `openssl` are standard on Linux targets.

**Alternatives considered**:
- Bundling a Blake3 binary: rejected — adds complexity and architecture-specific binaries. OpenSSL 3.0+ (`openssl dgst -blake3`) is widespread.
- Using `sha256sum` instead of Blake3: rejected — breaks compatibility with existing Blake3-based signing in `BinaryIntegrityService`.
- Verifying on the agent side only: rejected — defeats the purpose for user-initiated downloads where no parent agent is involved.

### RQ4: How should platform variants be managed?

**Decision**: The agent binary is stored in platform-specific subdirectories under `api/src/main/resources/agents/{platform}/agent`. The platform is identified by a path variable in the download endpoint. Available platforms are discovered by listing subdirectories at startup.

**Rationale**:
- Platform identifiers follow convention: `linux-x86_64`, `macos-aarch64`, `macos-x86_64`.
- The build-and-package script (`scripts/build-agent-and-package.sh`) runs the GraalVM native build, then copies the binary to the appropriate `api/src/main/resources/agents/{platform}/` directory.
- At startup, `AgentBinaryServiceImpl` scans `agents/` for subdirectories and loads binaries per platform. The `listPlatforms()` method returns available platforms for the UI.

**Alternatives considered**:
- Version-tagged binaries: deferred — version tracking (Story 2 in spec) is a P2 feature. This feature focuses on the latest binary per platform.
- Platform detection from User-Agent: rejected — the server doesn't know the target platform; the administrator selects it.

### RQ5: How should download audit records be stored?

**Decision**: New MongoDB collection `agent_download_records` with fields: userId, organizationId, platform, agentVersion (nullable), downloadTimestamp, clientIp. The record is created synchronously on each successful download.

**Rationale**:
- Simple, flat document matches MongoDB's strengths. No complex relationships needed.
- Organization-scoped via the existing `ProjectScopeMongoEventListener` pattern — records are filtered by organization automatically.
- Timestamps use `LocalDateTime` consistent with other entities (`ReplicationRequest`, `RemediationRecord`).

### RQ6: How should the UI download section integrate with the existing agents page?

**Decision**: Add a "Download Agent" button in the agents list page header (alongside the search bar). Clicking it opens a sidebar panel or modal with: platform selector dropdown, download button, and version info. Follows existing Primeng component patterns.

**Rationale**:
- The agents page is the natural place — administrators manage agents there, and downloading an agent is part of that workflow.
- A modal/sidebar keeps the feature self-contained without altering the page layout significantly.
- Reuses the existing `AgentsService` to call the new download endpoint.

**Alternatives considered**:
- Separate "Downloads" page in the menu: rejected — adds menu clutter for a simple action. Download is a sub-action of agent management.
- Inline in the agents table: rejected — download is not per-agent; it's for acquiring the binary to create new agents.
