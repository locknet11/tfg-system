# Phase 1 Data Model: Agent Tool Bundling

No new persisted entities are introduced (no database, no new DTOs sent to the central platform). This feature adds in-memory/runtime domain concepts local to `agents/unix`.

## BundledTool (enum)

Identifies a bundled capability by role rather than by product name, per the spec's Key Entities.

| Value | Capability | Backing binary (this build) |
|-------|------------|------------------------------|
| `NETWORK_DISCOVERY` | Identify reachable hosts on a target range | `nmap` (host-discovery mode) |
| `PORT_SERVICE_SCAN` | Identify open ports and running services | `rustscan` (fast port sweep) + `nmap` (service/version detection on open ports) |
| `RAW_TCP` | Low-level TCP connectivity / banner interaction, usable from remediation/exploit scripts | `nc` |
| `FILE_RETRIEVAL` | Fetch a remote file/resource | `curl` |

Fields (as modeled in `BundledTool.java`, an enum with per-constant metadata):
- `binaryName: String` — the executable filename inside the extracted tools directory (e.g. `nmap`).
- `capabilityDescription: String` — human-readable purpose, used in log/error messages.

## Resolved Tool Path (runtime value, not a persisted entity)

Produced by `BundledToolProvisioner` during startup extraction.

- `tool: BundledTool`
- `absolutePath: Path` — location of the extracted, executable binary on the local filesystem.
- Validation: extraction MUST fail fast with a `ToolExtractionException` (captured at startup, logged) if no binary exists for the current `os.name`/`os.arch` combination — this is what allows FR-005 ("report as failed with a descriptive error") to apply even before any step runs.

## Step Result additions (reuses existing `StepResult` / `ServiceInfo`, no schema change)

- `NETWORK_SCAN` steps populate `StepResult.logs` with one `HOST_FOUND:<address>` entry per reachable host discovered (mirrors the existing `"targetId:"`/`"NO_VULNS:"`-prefixed log-line convention already used by `RemediationStepHandler`), and `StepResult.success/failure` per Assumptions.
- `SERVICE_SCAN` / `SYSTEM_SCAN` steps populate `StepResult.services` with real `ServiceInfo(name, version, port)` entries discovered by the bundled scan (reusing the exact entity `RemediationStepHandler` already consumes from `context.get(StepAction.SERVICE_SCAN)`), so no downstream handler needs to change how it reads scan results.
- Any step whose failure is caused by a missing/failed bundled tool includes a log line prefixed `TOOL_ERROR:<tool>:<reason>` so operators can distinguish "no findings" from "the tool itself failed" (supports FR-005 / SC-003).

## StepHandler contract change

`StepHandler.handle(StepAction action, Map<StepAction, StepResult> context, String targetIp)` — see [contracts/step-handler-interface.md](./contracts/step-handler-interface.md) for the full before/after contract and migration notes for existing implementations.
