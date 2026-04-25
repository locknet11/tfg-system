package com.spulido.agent.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
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
class TaskExecutionServiceTest {

    @Mock
    private CommandExecutor commandExecutor;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new TaskExecutionService(commandExecutor);
    }

    @Test
    void executesTasksInOrder_whenAllSucceed() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("step-1", 0, "echo one", 30),
                new TaskDefinition("step-2", 1, "echo two", 30),
                new TaskDefinition("step-3", 2, "echo three", 30));

        when(commandExecutor.execute("echo one", 30))
                .thenReturn(TaskResult.success("task-1", "ok"));
        when(commandExecutor.execute("echo two", 30))
                .thenReturn(TaskResult.success("task-2", "ok"));
        when(commandExecutor.execute("echo three", 30))
                .thenReturn(TaskResult.success("task-3", "ok"));

        AgentJob job = service.executeJob("job-1", steps);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getTasks()).hasSize(3);

        AgentTask t1 = job.getTasks().get(0);
        AgentTask t2 = job.getTasks().get(1);
        AgentTask t3 = job.getTasks().get(2);

        assertThat(t1.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(t2.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(t3.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        verify(commandExecutor, times(1)).execute("echo one", 30);
        verify(commandExecutor, times(1)).execute("echo two", 30);
        verify(commandExecutor, times(1)).execute("echo three", 30);
    }

    @Test
    void stopsExecution_whenTaskFails() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("step-1", 0, "echo one", 30),
                new TaskDefinition("step-2", 1, "fail-command", 30),
                new TaskDefinition("step-3", 2, "echo three", 30));

        when(commandExecutor.execute("echo one", 30))
                .thenReturn(TaskResult.success("task-1", "ok"));
        when(commandExecutor.execute("fail-command", 30))
                .thenReturn(TaskResult.failure("task-2", "command failed", "exit code 1"));

        AgentJob job = service.executeJob("job-2", steps);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);

        AgentTask t1 = job.getTasks().get(0);
        AgentTask t2 = job.getTasks().get(1);
        AgentTask t3 = job.getTasks().get(2);

        assertThat(t1.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(t2.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(t3.getStatus()).isEqualTo(TaskStatus.SKIPPED);

        verify(commandExecutor, times(0)).execute("echo three", 30);
    }

    @Test
    void rejectsJob_withNoSteps() {
        assertThatThrownBy(() -> service.executeJob("job-empty", Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one");
    }

    @Test
    void marksJobInvalid_whenStepsHaveDuplicateOrder() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("step-1", 0, "echo one", 30),
                new TaskDefinition("step-2", 0, "echo two", 30));

        assertThatThrownBy(() -> service.executeJob("job-dup", steps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }
}
