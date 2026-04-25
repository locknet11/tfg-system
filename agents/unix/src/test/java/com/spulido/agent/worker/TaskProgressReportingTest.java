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
class TaskProgressReportingTest {

    @Mock
    private CommandExecutor commandExecutor;

    private TaskExecutionService service;

    @BeforeEach
    void setUp() {
        service = new TaskExecutionService(commandExecutor);
    }

    @Test
    void snapshotShowsCompletedTasks_whenJobSucceeds() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "cmd-1", 30),
                new TaskDefinition("s2", 1, "cmd-2", 30));

        when(commandExecutor.execute(anyString(), eq(30L)))
                .thenReturn(TaskResult.success("task", "ok"));

        AgentJob job = service.executeJob("progress-job", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(snapshot.getTasks()).hasSize(2);
        assertThat(snapshot.getTasks().get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(snapshot.getTasks().get(1).status()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void snapshotShowsFailedAndSkippedTasks_whenJobFails() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("s1", 0, "cmd-1", 30),
                new TaskDefinition("s2", 1, "fail-cmd", 30),
                new TaskDefinition("s3", 2, "cmd-3", 30));

        when(commandExecutor.execute("cmd-1", 30))
                .thenReturn(TaskResult.success("t1", "ok"));
        when(commandExecutor.execute("fail-cmd", 30))
                .thenReturn(TaskResult.failure("t2", "error", "exit code 1"));

        AgentJob job = service.executeJob("fail-progress-job", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getJobStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(snapshot.getTasks()).hasSize(3);
        assertThat(snapshot.getTasks().get(0).status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(snapshot.getTasks().get(1).status()).isEqualTo(TaskStatus.FAILED);
        assertThat(snapshot.getTasks().get(1).failureReason()).isEqualTo("exit code 1");
        assertThat(snapshot.getTasks().get(2).status()).isEqualTo(TaskStatus.SKIPPED);
    }

    @Test
    void snapshotPreservesTaskOrdering() {
        List<TaskDefinition> steps = Arrays.asList(
                new TaskDefinition("a", 0, "first", 10),
                new TaskDefinition("b", 1, "second", 10),
                new TaskDefinition("c", 2, "third", 10));

        when(commandExecutor.execute(anyString(), eq(10L)))
                .thenReturn(TaskResult.success("task", "ok"));

        AgentJob job = service.executeJob("order-snapshot", steps);
        JobProgressSnapshot snapshot = JobProgressSnapshot.from(job);

        assertThat(snapshot.getTasks().get(0).sequence()).isEqualTo(0);
        assertThat(snapshot.getTasks().get(1).sequence()).isEqualTo(1);
        assertThat(snapshot.getTasks().get(2).sequence()).isEqualTo(2);
    }
}
