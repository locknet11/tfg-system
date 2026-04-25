package com.spulido.agent.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.JobProgressSnapshot;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
class JobHistoryViewTest {

    @Mock
    private CommandExecutor commandExecutor;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new TaskExecutionService(commandExecutor);
    }

    @Test
    void historyShowsCompleteJob_whenAllTasksSucceed() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("h1", 0, "step-1", 30),
                new TaskDefinition("h2", 1, "step-2", 30));

        when(commandExecutor.execute(anyString(), eq(30L)))
                .thenReturn(TaskResult.success("task", "ok"));

        AgentJob job = service.executeJob("history-complete", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(snapshot.getStartedAt()).isNotNull();
        assertThat(snapshot.getCompletedAt()).isNotNull();
        assertThat(snapshot.getFailureReason()).isNull();
    }

    @Test
    void historyShowsFailureReason_whenJobFails() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("h1", 0, "step-1", 30),
                new TaskDefinition("h2", 1, "bad-step", 30));

        when(commandExecutor.execute("step-1", 30))
                .thenReturn(TaskResult.success("t1", "ok"));
        when(commandExecutor.execute("bad-step", 30))
                .thenReturn(TaskResult.failure("t2", "failed", "permission denied"));

        AgentJob job = service.executeJob("history-fail", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(snapshot.getFailureReason()).contains("permission denied");
        assertThat(snapshot.getTasks().stream()
                .filter(t -> t.status() == TaskStatus.FAILED)
                .count()).isEqualTo(1);
    }

    @Test
    void historyShowsAllTaskStates_inExecutionOrder() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("h1", 0, "ok-1", 30),
                new TaskDefinition("h2", 1, "fail-2", 30),
                new TaskDefinition("h3", 2, "ok-3", 30));

        when(commandExecutor.execute("ok-1", 30))
                .thenReturn(TaskResult.success("t1", "ok"));
        when(commandExecutor.execute("fail-2", 30))
                .thenReturn(TaskResult.failure("t2", "error", "timeout"));

        AgentJob job = service.executeJob("history-states", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getTasks()).hasSize(3);
        assertThat(snapshot.getTasks().get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(snapshot.getTasks().get(1).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(snapshot.getTasks().get(2).status()).isEqualTo(TaskStatus.SKIPPED);
    }
}
