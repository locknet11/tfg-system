# Quickstart: Agent Tool Bundling

## Prerequisites

- GraalVM JDK 21 configured for native builds (see `agents/unix/package-macos.sh` for the macOS local-dev pointer).
- Docker available if validating against the `lab/` vulnerable targets (`docker compose up` from `lab/`).

## Build

```bash
cd agents/unix
./mvnw clean package
```

Native build (matches the platform you're building on):

```bash
cd agents/unix
sh package-macos.sh          # macOS/arm64 local dev
# or
./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication
```

## Verify bundled tools are embedded and extracted

1. Run the built agent locally (JVM mode is enough to verify extraction logic without a full native build):
   ```bash
   cd agents/unix
   ./mvnw spring-boot:run
   ```
2. Confirm startup logs show `BundledToolProvisioner` extracting each of `nmap`, `rustscan`, `nc`, `curl` to a temp directory, with no `ToolExtractionException`.

## Verify a scan step produces real results

1. Start a target from the existing lab (`cd lab && docker compose up -d`), note its container IP.
2. Trigger a plan against that target containing `NETWORK_SCAN` and `SERVICE_SCAN` steps (via the existing central-platform plan-assignment flow used by other features, e.g. `014-assign-plan-modal`).
3. Confirm the reported step results contain real discovered hosts/ports/services (`ServiceInfo` entries), not the previous placeholder `"Executed echo step for action: ..."` log line.

## Verify failure is reported honestly

1. Point a `NETWORK_SCAN`/`SERVICE_SCAN` step at an unreachable address.
2. Confirm the step still completes (does not crash the plan run) and reports a result reflecting no reachable hosts, per spec User Story 1 Acceptance Scenario 3.
3. Temporarily rename/remove the extracted `nmap` binary in the runtime temp directory (or simulate on an unsupported `os.arch`) and confirm the step is reported as **failed** with a `TOOL_ERROR:` log line, not a false success — per spec User Story 3.

## Run unit tests

```bash
cd agents/unix
./mvnw test
```

Expect new/updated tests: `BundledToolProvisionerTest`, `NetworkScanStepHandlerTest`, `ServiceScanStepHandlerTest`, plus signature updates in `ExecuteExploitStepHandlerTest` and `TransferAgentStepHandlerTest` for the new `StepHandler.handle(action, context, targetIp)` signature.
