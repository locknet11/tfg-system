package com.spulido.agent.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.teardown.TeardownService;
import com.spulido.agent.teardown.TeardownTrigger;
import com.spulido.agent.utils.AgentLifecycle;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.PlanResponse;
import com.spulido.agent.worker.http.dto.PlanStepResponse;

@ExtendWith(MockitoExtension.class)
class WorkerCoordinatorTeardownTest {

    @Mock
    private ThreadPoolTaskExecutor executor;
    @Mock
    private TaskExecutionService taskExecutionService;
    @Mock
    private AgentHttpClient httpClient;
    @Mock
    private AgentConfig config;
    @Mock
    private AgentLifecycle agentLifecycle;
    @Mock
    private TeardownService teardownService;
    @Mock
    private AgentJob job;

    private WorkerCoordinator coordinator() {
        return new WorkerCoordinator(executor, taskExecutionService, httpClient, config,
                agentLifecycle, teardownService);
    }

    private PlanResponse planWithOneStep() {
        PlanStepResponse step = new PlanStepResponse();
        step.setAction("ECHO");
        PlanResponse plan = new PlanResponse();
        plan.setTargetIp("10.0.0.5");
        plan.setSteps(List.of(step));
        return plan;
    }

    private void runInline() {
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(executor).submit(any(Runnable.class));
    }

    @Test
    void completedPlan_triggersPlanCompletionTeardown() {
        WorkerCoordinator coordinator = coordinator();
        runInline();
        when(httpClient.fetchPlan()).thenReturn(planWithOneStep());
        when(taskExecutionService.executeJob(anyString(), anyList(), anyList(), any())).thenReturn(job);
        when(job.getStatus()).thenReturn(JobStatus.COMPLETED);

        coordinator.pollCentralPlatform();

        verify(teardownService).selfDestruct(TeardownTrigger.PLAN_COMPLETION);
    }

    @Test
    void failedPlan_doesNotTriggerTeardown() {
        WorkerCoordinator coordinator = coordinator();
        runInline();
        when(httpClient.fetchPlan()).thenReturn(planWithOneStep());
        when(taskExecutionService.executeJob(anyString(), anyList(), anyList(), any())).thenReturn(job);
        when(job.getStatus()).thenReturn(JobStatus.FAILED);

        coordinator.pollCentralPlatform();

        verify(teardownService, never()).selfDestruct(any());
    }

    @Test
    void whenTearingDown_skipsPollingForNewWork() {
        WorkerCoordinator coordinator = coordinator();
        when(teardownService.isTearingDown()).thenReturn(true);

        coordinator.pollCentralPlatform();

        verify(executor, never()).submit(any(Runnable.class));
        verify(httpClient, never()).fetchPlan();
    }
}
