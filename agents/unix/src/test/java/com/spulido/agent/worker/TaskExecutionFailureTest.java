package com.spulido.agent.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
class TaskExecutionFailureTest {

    @Mock
    private CommandExecutor commandExecutor;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new TaskExecutionService(commandExecutor);
    }

    @Test
    void stopsRemainingTasks_whenMiddleTaskFails() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "cmd-1", 30),
                new TaskDefinition("s2", 1, "cmd-2", 30),
                new TaskDefinition("s3", 2, "cmd-3", 30));

        when(commandExecutor.execute("cmd-1", 30))
                .thenReturn(TaskResult.success("t1", "ok"));
        when(commandExecutor.execute("cmd-2", 30))
                .thenReturn(TaskResult.failure("t2", "command failed", "exit code 1"));

        AgentJob job = service.executeJob("fail-job", steps);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getFailureReason()).contains("exit code 1");

        assertThat(job.getTasks().get(0).getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(job.getTasks().get(1).getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(job.getTasks().get(2).getStatus()).isEqualTo(TaskStatus.SKIPPED);
    }

    @Test
    void capturesFailureReason_onFailedTask() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "fail-cmd", 30));

        when(commandExecutor.execute("fail-cmd", 30))
                .thenReturn(TaskResult.failure("t1", "execution error", "timeout exceeded"));

        AgentJob job = service.executeJob("reason-job", steps);

        AgentTask task = job.getTasks().get(0);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getResult()).isNotNull();
        assertThat(task.getResult().getFailureReason()).isEqualTo("timeout exceeded");
        assertThat(task.getResult().getMessage()).isEqualTo("execution error");
    }

    @Test
    void marksJobFailed_withFirstFailureReason() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "cmd-1", 30),
                new TaskDefinition("s2", 1, "cmd-2", 30));

        when(commandExecutor.execute("cmd-1", 30))
                .thenReturn(TaskResult.success("t1", "ok"));
        when(commandExecutor.execute("cmd-2", 30))
                .thenReturn(TaskResult.failure("t2", "failed", "disk full"));

        AgentJob job = service.executeJob("first-fail-job", steps);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getFailureReason()).contains("disk full");
    }
}
