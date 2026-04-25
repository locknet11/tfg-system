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
import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.domain.task.TaskStatus;

@ExtendWith(MockitoExtension.class)
class WorkerCoordinatorTest {

    @Mock
    private CommandExecutor commandExecutor;

    private TaskExecutionService taskExecutionService;

    @BeforeEach
    void setUp() {
        taskExecutionService = new TaskExecutionService(commandExecutor);
    }

    @Test
    void coordinatesSequentialExecution_whenJobHasMultipleSteps() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("step-a", 0, "cmd-a", 10),
                new TaskDefinition("step-b", 1, "cmd-b", 10));

        when(commandExecutor.execute("cmd-a", 10))
                .thenReturn(TaskResult.success("task-a", "ok"));
        when(commandExecutor.execute("cmd-b", 10))
                .thenReturn(TaskResult.success("task-b", "ok"));

        AgentJob job = taskExecutionService.executeJob("coord-job-1", steps);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getTasks()).hasSize(2);

        AgentTask first = job.getTasks().get(0);
        AgentTask second = job.getTasks().get(1);

        assertThat(first.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(first.getSequence()).isLessThan(second.getSequence());
    }

    @Test
    void preservesTaskOrdering_inExecutionResult() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "first", 5),
                new TaskDefinition("s2", 1, "second", 5),
                new TaskDefinition("s3", 2, "third", 5));

        when(commandExecutor.execute(anyString(), eq(5L)))
                .thenReturn(TaskResult.success("task", "ok"));

        AgentJob job = taskExecutionService.executeJob("order-job", steps);

        assertThat(job.getTasks().get(0).getSequence()).isEqualTo(0);
        assertThat(job.getTasks().get(1).getSequence()).isEqualTo(1);
        assertThat(job.getTasks().get(2).getSequence()).isEqualTo(2);
    }
}
