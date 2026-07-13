# Quickstart: Unix Agent Self-Destruction & Self-Cleanup

How to build, test, and verify the teardown lifecycle across the affected modules.

## Prerequisites

- JDK 21 + Maven wrapper (`./mvnw`) for `agents/unix` and `api`.
- GraalVM (for the native build sanity check) — macOS: `agents/unix/package-macos.sh`.
- A unix host or container for the sandbox integration test.

## Build

```sh
# Agent
cd agents/unix && ./mvnw clean package

# API
cd api && ./mvnw clean package
```

## Unit tests (deterministic, no real network/filesystem)

```sh
cd agents/unix && ./mvnw test    # TeardownService, ArtifactSet, SelfDestructStepHandler, HeartbeatSender
cd api && ./mvnw test            # AgentCommunicationService teardown/deprovision behavior
```

Key unit cases:
- `TeardownServiceTest`: single-shot guard (second trigger is a no-op); best-effort (one failing removal does not abort the rest); ordering (final report attempted before local removal); idempotent re-run yields `NOT_PRESENT`.
- `HeartbeatSenderTest`: `deprovision=true` → `selfDestruct(PLATFORM_DEPROVISION)`; 3 consecutive 401/403/404 → `selfDestruct(AUTH_REVOKED)`; transport failure → no teardown, counter not advanced.
- `SelfDestructStepHandlerTest`: `SELF_DESTRUCT` step delegates to `TeardownService`.
- API: deleted agent → heartbeat returns `deprovision=true`; teardown report persists an `AgentTeardownRecord`; duplicate report is idempotent.

## Sandbox integration test (teardown shell)

`SelfDestructSandboxIT` renders `self-destruct.sh.tmpl` via `ScriptTemplateService` into a **temp sandbox** populated with fake artifacts (`agent`, `agent.properties`, `agent.log`, a tools dir, a fake systemd unit path under the sandbox root) and runs it:

```sh
cd agents/unix && ./mvnw -Dtest=SelfDestructSandboxIT verify
```

Asserts:
1. All seeded artifacts are removed after the script runs.
2. Re-running the script on the already-clean sandbox exits 0 with no error (idempotent).
3. With one artifact made non-removable (e.g. permission-locked), the remaining artifacts are still removed (best-effort).
4. The script removes itself last.

## Manual end-to-end verification

**Plan-completion teardown**:
1. Install an agent on a test host (`curl … | bash`, feature 015). Confirm `/tmp/agent`, `/tmp/agent.properties`, `/tmp/agent.log` exist and the process runs.
2. Assign a short plan whose steps all complete.
3. Observe: final plan status recorded in central → `POST /api/agent/comm/teardown` received (audit record persisted) → agent process gone → `/tmp/agent*` and tool dir removed → no restart over a 10-minute watch.

**Platform de-provision teardown**:
1. With the agent running, `DELETE /api/agent/{id}`.
2. On the next heartbeat (~30s) the response carries `deprovision=true`.
3. Observe teardown as above; verify the agent does not reappear.

**Auth-revoked fallback**:
1. Hard-remove/rotate the agent's credential so heartbeats get 401/403/404.
2. After 3 consecutive rejections (~90s) the agent self-destructs (`AUTH_REVOKED`), even with no explicit signal.

## Constitution / boundary checks

- Confirm `self-destruct.sh.tmpl` lives under `agents/unix/src/main/resources/scripts/` and is rendered only via `ScriptTemplateService` (`ClassPathResource` + `String.replace`). Grep for inline script construction:
  ```sh
  cd agents/unix && grep -rn "String.format\|StringBuilder\|+ \"#!/bin" src/main/java/com/spulido/agent/teardown && echo "VIOLATION" || echo "OK: no inline scripts"
  ```
- Confirm no wildcard imports and ordered imports in new Java files.
- Confirm all new user-facing/log text is English.
