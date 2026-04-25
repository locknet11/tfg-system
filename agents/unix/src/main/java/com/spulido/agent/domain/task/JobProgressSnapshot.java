package com.spulido.agent.domain.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobProgressSnapshot {

    private final String jobId;
    private final JobStatus jobStatus;
    private final List<TaskProgressEntry> tasks;
    private final Instant createdAt;
    private final Instant startedAt;
    private final Instant completedAt;
    private final String failureReason;

    private JobProgressSnapshot(String jobId, JobStatus jobStatus, List<TaskProgressEntry> tasks,
            Instant createdAt, Instant startedAt, Instant completedAt, String failureReason) {
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
    }

    public static JobProgressSnapshot from(AgentJob job) {
        List<TaskProgressEntry> entries = new ArrayList<>();
        for (AgentTask task : job.getTasks()) {
            entries.add(TaskProgressEntry.from(task));
        }
        entries.sort((a, b) -> Integer.compare(a.sequence(), b.sequence()));

        return new JobProgressSnapshot(
                job.getJobId(),
                job.getStatus(),
                entries,
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getFailureReason());
    }

    public String getJobId() { return jobId; }
    public JobStatus getJobStatus() { return jobStatus; }
    public List<TaskProgressEntry> getTasks() { return tasks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }

    public record TaskProgressEntry(
            String taskId,
            int sequence,
            String command,
            TaskStatus status,
            Instant startedAt,
            Instant completedAt,
            String failureReason) {

        public static TaskProgressEntry from(AgentTask task) {
            String reason = null;
            if (task.getResult() != null && !task.getResult().isSuccess()) {
                reason = task.getResult().getFailureReason();
            }
            return new TaskProgressEntry(
                    task.getTaskId(),
                    task.getSequence(),
                    task.getCommand(),
                    task.getStatus(),
                    task.getStartedAt(),
                    task.getCompletedAt(),
                    reason);
        }
    }
}
