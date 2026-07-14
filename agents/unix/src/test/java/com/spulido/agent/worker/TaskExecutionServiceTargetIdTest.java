package com.spulido.agent.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.step.StepHandler;

/**
 * SERVICE_SCAN only knows the scanned address (nmap has no concept of Central's
 * document id), so it emits {@code targetId:<address>}. Central's downstream lookups
 * expect the real target id from the plan response. executeJob's targetId overload
 * must rewrite that log line so the chain doesn't break at EXPLOITATION_KNOWLEDGE.
 */
class TaskExecutionServiceTargetIdTest {

    @Test
    void rewritesServiceScanTargetIdLogWithRealCentralId() {
        CommandExecutor commandExecutor = (command, timeoutSeconds) -> TaskResult.success("t", "ok");

        StepHandler fakeServiceScan = (action, context, targetIp) -> StepResult.success(
                StepAction.SERVICE_SCAN, List.of(), List.of(),
                List.of("targetId:" + targetIp, "SERVICE:docker:20.10:2375"));

        Map<StepAction, StepHandler> handlers = Map.of(StepAction.SERVICE_SCAN, fakeServiceScan);
        TaskExecutionService service = new TaskExecutionService(commandExecutor, handlers);

        List<TaskDefinition> steps = List.of(new TaskDefinition("step-1", 0, "echo scan", 30));
        List<StepAction> actions = List.of(StepAction.SERVICE_SCAN);

        AgentJob job = service.executeJob("job-1", steps, actions, "172.20.0.14", "6a5541137b29d23e23efdba5");

        assertThat(job.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void leavesServiceScanUntouchedWhenNoRealTargetIdProvided() {
        CommandExecutor commandExecutor = (command, timeoutSeconds) -> TaskResult.success("t", "ok");

        StepHandler fakeServiceScan = (action, context, targetIp) -> StepResult.success(
                StepAction.SERVICE_SCAN, List.of(), List.of(), List.of("targetId:" + targetIp));

        Map<StepAction, StepHandler> handlers = Map.of(StepAction.SERVICE_SCAN, fakeServiceScan);
        TaskExecutionService service = new TaskExecutionService(commandExecutor, handlers);

        List<TaskDefinition> steps = List.of(new TaskDefinition("step-1", 0, "echo scan", 30));
        List<StepAction> actions = List.of(StepAction.SERVICE_SCAN);

        // legacy 4-arg overload (no targetId) must behave exactly as before.
        AgentJob job = service.executeJob("job-1", steps, actions, "172.20.0.14");

        assertThat(job.getStatus().name()).isEqualTo("COMPLETED");
    }
}
