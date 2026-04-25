package com.spulido.agent.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.domain.task.TaskStatus;

public class TaskStateLogger {

    private static final Logger log = LoggerFactory.getLogger(TaskStateLogger.class);

    public void logTaskStarted(AgentTask task) {
        log.info("Task started: taskId={}, jobId={}, sequence={}, command={}",
                task.getTaskId(), task.getJobId(), task.getSequence(), task.getCommand());
    }

    public void logTaskCompleted(AgentTask task) {
        TaskResult result = task.getResult();
        if (result != null && result.isSuccess()) {
            log.info("Task completed: taskId={}, jobId={}, sequence={}, status=COMPLETED",
                    task.getTaskId(), task.getJobId(), task.getSequence());
        } else {
            log.warn("Task failed: taskId={}, jobId={}, sequence={}, status=FAILED, reason={}",
                    task.getTaskId(), task.getJobId(), task.getSequence(),
                    result != null ? result.getFailureReason() : "unknown");
        }
    }

    public void logTaskSkipped(AgentTask task) {
        log.info("Task skipped: taskId={}, jobId={}, sequence={}",
                task.getTaskId(), task.getJobId(), task.getSequence());
    }

    public void logJobStarted(AgentJob job) {
        log.info("Job started: jobId={}, taskCount={}", job.getJobId(), job.getTasks().size());
    }

    public void logJobCompleted(AgentJob job) {
        log.info("Job completed: jobId={}, status={}", job.getJobId(), job.getStatus());
    }

    public void logJobFailed(AgentJob job) {
        log.error("Job failed: jobId={}, reason={}", job.getJobId(), job.getFailureReason());
    }
}
