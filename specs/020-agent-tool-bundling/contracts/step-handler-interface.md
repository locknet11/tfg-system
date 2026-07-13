# Contract: StepHandler interface

This feature is internal to `agents/unix` and adds no new HTTP/API surface. The one interface contract that changes, and that every current and future `StepHandler` implementation must honor, is documented here.

## Before

```java
public interface StepHandler {
    StepResult handle(StepAction action, Map<StepAction, StepResult> context);
}
```

## After

```java
public interface StepHandler {
    StepResult handle(StepAction action, Map<StepAction, StepResult> context, String targetIp);
}
```

## Semantics

- `targetIp` is the address of the current plan's target, as provided by the central platform (`PlanResponse.getTargetIp()`), forwarded unchanged from `WorkerCoordinator.runJob()` through `TaskExecutionService.executeJob(...)`.
- `targetIp` MAY be `null` or blank for plans/tests that don't set a target (e.g. `ECHO`-only jobs); handlers that don't need a target (most existing handlers) MUST ignore the parameter rather than fail on a missing value.
- `targetIp` MUST NOT be mutated or reinterpreted by a handler as a list of hosts, a CIDR range, or anything beyond the single address the platform sent — scope expansion beyond the declared target is out of bounds (spec FR-008).

## Implementations required to migrate (signature-only change, no behavior change)

- `EchoStepHandler`
- `RemediationStepHandler`
- `ExecuteExploitStepHandler`
- `ExploitationKnowledgeStepHandler`
- `RequestReplicationStepHandler`
- `TransferAgentStepHandler`

## New implementations introduced by this feature

- `NetworkScanStepHandler` — uses `targetIp` as the network-discovery target.
- `ServiceScanStepHandler` — uses `targetIp` as the port/service-scan target.

## Test impact

Any existing unit test that calls `handler.handle(action, context)` directly must be updated to pass a third argument (a test target IP, or `null`/`""` where the handler under test does not use it).
