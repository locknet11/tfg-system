# Phase 0 Research: Unix Agent Self-Destruction & Self-Cleanup

Resolves the three open questions carried from the spec's Constitution Notes plus the core teardown technique. Each decision is grounded in the existing codebase (feature 015 install script, feature 011 heartbeat, feature 001 step execution) and the repo's GraalVM-native + script/template constraints.

## Decision 1 — Binary + install-script self-removal technique

**Decision**: The running agent unlinks its own on-disk artifacts via a short **detached POSIX shell** rendered from `self-destruct.sh.tmpl`. On both supported targets (Linux `linux-amd64`, macOS `darwin-arm64`), the kernel keeps an executing binary's inode alive until the process exits, so `rm -f` on the running binary's path succeeds immediately while the process is still running. No `memfd_create` re-exec or in-memory relaunch is required. The agent flushes its final report **first**, then spawns the detached script (`nohup sh <script> &` fully detached from the agent process group), then calls `AgentLifecycle.stop()`. The detached script removes the binary path, sweeps any residual artifacts idempotently, removes OS registration, and finally removes itself.

**Rationale**:
- Unlink-while-running is POSIX-portable and works on Linux and macOS; it is the simplest correct approach and avoids fragile native re-exec tricks that are hostile to GraalVM native images.
- The install script (feature 015) launches the binary as `/tmp/agent` via `nohup`, and writes `/tmp/agent.properties` and `/tmp/agent.log`. These are concrete, known paths the agent can target. The install script itself is delivered by `curl … | bash` (runs from stdin, not persisted), so in the default flow there is no install script *file* on disk — the removal step treats it as `NOT_PRESENT` unless a persisted copy is found. The template still removes a known install-script path when one exists (covers transfer/replication installs that stage a script file).
- Detaching the final unlink into a separate process guarantees the binary is removed even though the Java process holds it; the agent process exiting immediately afterward leaves nothing on disk.

**Alternatives considered**:
- *`memfd_create` + re-exec from anonymous memory, then unlink*: more "textbook" for load-into-memory removal, but adds native syscall complexity, is Linux-only (no macOS equivalent), and conflicts with GraalVM-native packaging. Rejected — unlink-while-running already achieves "no binary left on disk after exit" on both targets.
- *Java `Files.delete` of the running binary from within the JVM*: unreliable/again process-holds-file semantics differ; and it cannot cleanly delete the very executable whose exit we depend on. Kept Java-side deletion for **non-binary** artifacts (config, logs, tools dir) where per-artifact outcome is easy to capture, and delegated only the binary + residual sweep to the detached script.

## Decision 2 — Threshold for implicit self-destruct on failed authentication

**Decision**: Distinguish **authenticated rejection** from **transport failure**. The agent self-destructs implicitly **only** on sustained *authenticated rejection* — the platform is reachable and explicitly says this agent no longer exists / is not authorized: HTTP `401`, `403`, or `404 agent-not-found` on the heartbeat endpoint. Threshold: **3 consecutive authenticated-rejection heartbeats** (≈ 90s at the 30s interval). Connection timeouts, `5xx`, DNS failures, and network-unreachable errors are **transport failures**: they never trigger teardown and never advance the rejection counter (the counter resets on any successful heartbeat or transport failure).

**Rationale**:
- A transient platform outage or network partition must not destroy a healthy agent (SC-007 spirit + spec assumption). Only an explicit, authenticated "you are gone" from the reachable platform is trustworthy.
- 3 consecutive rejections tolerates a single spurious rejection (e.g. mid-rotation) while still self-destructing within ~2 minutes of genuine revocation, satisfying SC-003 (gone within ~one heartbeat interval of the signal being *observed as sustained*).
- Reuses the existing 30s `@Scheduled` heartbeat; no new polling loop.

**Alternatives considered**:
- *Any single 401/403 triggers teardown*: too aggressive — one spurious auth blip would irreversibly destroy the agent. Rejected.
- *Time-window only (e.g. 10 min of failures)*: conflates transport and auth failures and is slow. Rejected in favor of an explicit-rejection counter.

## Decision 3 — Behavior when a de-provision signal arrives mid-plan

**Decision**: On a de-provision signal (or plan-completion) the agent **stops accepting new steps immediately** but **lets the current in-flight atomic step finish** (best-effort, bounded by that step's existing timeout) before tearing down. It does not start the next step. A `tearingDown` flag gates the `WorkerCoordinator` poll/step loop so no new job or step is picked up.

**Rationale**:
- Interrupting a remediation mid-write can leave a target in a broken half-remediated state (edge case in spec). Finishing the current atomic step is safer and steps are already time-bounded (`TaskDefinition` carries a timeout), so teardown is not delayed unboundedly.
- New-work suppression is cheap: check the single-shot `tearingDown` flag at the top of `pollCentralPlatform()`/`runJob()` and before dispatching each step.

**Alternatives considered**:
- *Hard-abort the current step immediately*: faster teardown but risks broken targets and partial writes. Rejected.
- *Wait for the whole plan to finish before honoring de-provision*: violates the operator's intent to retire the agent now, and a long/stuck plan would block teardown. Rejected.

## Decision 4 — De-provision signal delivery + authentication (agent ⇆ api)

**Decision**: Extend the existing heartbeat response (`PUT /api/agent/comm/heartbeat`) with a boolean `deprovision` flag (plus an optional `deprovisionReason`). When an operator calls the existing `DELETE /api/agent/{id}` (`AgentController.deleteAgent`), the api first **soft-marks** the agent de-provisioned (status `KILLED` / a `deprovisioned=true` flag) rather than immediately hard-deleting, so the agent's *next authenticated heartbeat* returns `deprovision=true`. The heartbeat is already keyed by the agent's identity/credential (agentId + api key/preauth as today), so the signal is inherently bound to and authenticated for that specific agent — an unauthenticated caller cannot elicit a `deprovision=true` for someone else's agent. Hard deletion / record reaping happens after the teardown-outcome report is received (or after a grace TTL if the agent never reports).

**Rationale**:
- Minimal, additive change to the current heartbeat contract and the existing delete endpoint — no new agent-facing control channel, no push infrastructure. Reuses feature 011's 30s heartbeat as the delivery mechanism exactly as the technical view requested.
- Authentication piggybacks on the existing agent-communication auth, satisfying the security requirement (FR: signal must be authenticated to this agent; SC-007: spoofed signal → 0 self-destructions).

**Alternatives considered**:
- *Immediate hard delete on `DELETE /agent/{id}`*: then the next heartbeat 404s and the agent would rely solely on the auth-rejection path (Decision 2), losing the explicit reason and the audit trail of the outcome report. Kept soft-mark-then-reap so the explicit signal + audit record both work; the auth-rejection path remains the fallback for agents deleted the old (hard) way.
- *New dedicated `/deprovision` poll endpoint*: extra surface for no benefit; heartbeat already runs. Rejected.

## Decision 5 — Teardown outcome reporting + audit persistence

**Decision**: The agent computes a per-artifact `TeardownOutcome` for every artifact it removes from Java (config, logs, tools dir, OS-registration files it can see, install-script file) — each `REMOVED | FAILED | NOT_PRESENT` — plus the trigger type and a timestamp, and `POST`s it to a new `POST /api/agent/comm/teardown` endpoint **before** spawning the detached binary-removal script and exiting. The api persists it as an `AgentTeardownRecord` (MongoDB) for audit and only then reaps the agent record. When the host is offline, the report is skipped (best-effort) and central reconciles the agent's disappearance via the existing heartbeat/offline detection (feature 011).

**Rationale**: satisfies FR-013 (record + report per-step outcome), SC-002 (final report precedes local removal in online cases), and the traceability requirement, while remaining best-effort for the offline case.

## Cross-cutting confirmations

- **Single-shot**: an `AtomicBoolean.compareAndSet(false, true)` guard in `TeardownService.selfDestruct(trigger)` makes concurrent triggers (plan-completion + de-provision arriving together) a no-op after the first (FR-014).
- **Best-effort**: each Java-side removal is wrapped independently; a failure records `FAILED` and continues (FR-011). The detached shell uses `rm -f` and `|| true` per step so no step aborts the rest.
- **Idempotent**: removing an already-absent path yields `NOT_PRESENT` (Java) / silent success (`rm -f`), so re-running teardown is harmless (FR-012).
- **No collateral removal**: `ArtifactSet` only enumerates paths the agent/installer created (from `AgentConfig` + the known `/tmp/agent*` install layout + `BundledToolProvisioner.getExtractionDirectory()`); it never globs host directories (FR-016).
- **Template boundary**: `self-destruct.sh.tmpl` under `src/main/resources/scripts/`, rendered by `ScriptTemplateService.renderTemplate(...)` with `{{PLACEHOLDER}}` replacements — identical pattern to `install-agent-http.sh.tmpl`. No inline script construction.
