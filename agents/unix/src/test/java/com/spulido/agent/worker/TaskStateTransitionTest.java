package com.spulido.agent.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.domain.task.TaskStatus;

class TaskStateTransitionTest {

    @Test
    void transitionsFromPendingToRunning_onStart() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);

        task.start();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(task.getStartedAt()).isNotNull();
    }

    @Test
    void transitionsFromRunningToCompleted_onSuccess() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        task.start();

        task.complete(TaskResult.success("t1", "done"));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
        assertThat(task.getResult().isSuccess()).isTrue();
    }

    @Test
    void transitionsFromRunningToFailed_onFailure() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        task.start();

        task.complete(TaskResult.failure("t1", "error", "exit code 1"));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getResult().getFailureReason()).isEqualTo("exit code 1");
    }

    @Test
    void transitionsFromPendingToSkipped_whenEarlierTaskFails() {
        AgentTask task = new AgentTask("t2", "j1", 1, "cmd");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);

        task.skip();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SKIPPED);
    }

    @Test
    void transitionsFromRunningToInterrupted_onInterrupt() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        task.start();

        task.interrupt();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INTERRUPTED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    void rejectsStart_fromNonPendingState() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        task.start();

        assertThatThrownBy(task::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUNNING");
    }

    @Test
    void rejectsComplete_fromNonRunningState() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");

        assertThatThrownBy(() -> task.complete(TaskResult.success("t1", "ok")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void rejectsSkip_fromNonPendingState() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        task.start();

        assertThatThrownBy(task::skip)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RUNNING");
    }

    @Test
    void rejectsInterrupt_fromNonRunningState() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");

        assertThatThrownBy(task::interrupt)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void setsTimeoutAt_whenConfigured() {
        AgentTask task = new AgentTask("t1", "j1", 0, "cmd");
        Instant timeout = Instant.now().plusSeconds(30);

        task.setTimeoutAt(timeout);
        assertThat(task.getTimeoutAt()).isEqualTo(timeout);
    }
}
