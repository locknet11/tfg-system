package com.spulido.agent.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;

public class TaskExecutionService {

    private final CommandExecutor commandExecutor;
    private final TaskStateLogger taskStateLogger;

    public TaskExecutionService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.taskStateLogger = new TaskStateLogger();
    }

    public AgentJob executeJob(String jobId, List<TaskDefinition> steps) {
        JobValidator.validate(steps);

        List<AgentTask> tasks = new ArrayList<>();
        for (TaskDefinition step : steps) {
            String taskId = UUID.randomUUID().toString();
            tasks.add(new AgentTask(taskId, jobId, step.getOrder(), step.getCommand()));
        }

        AgentJob job = new AgentJob(jobId, tasks);
        taskStateLogger.logJobStarted(job);
        job.start();

        for (AgentTask task : job.getTasks()) {
            task.start();
            taskStateLogger.logTaskStarted(task);

            TaskDefinition matchingStep = findStepForTask(task, steps);
            TaskResult result = commandExecutor.execute(task.getCommand(), matchingStep.getTimeoutSeconds());

            task.complete(result);
            taskStateLogger.logTaskCompleted(task);

            if (!result.isSuccess()) {
                skipRemainingTasks(job, task);
                job.fail("Task " + task.getTaskId() + " failed: " + result.getFailureReason());
                taskStateLogger.logJobFailed(job);
                return job;
            }
        }

        job.complete();
        taskStateLogger.logJobCompleted(job);
        return job;
    }

    private TaskDefinition findStepForTask(AgentTask task, List<TaskDefinition> steps) {
        for (TaskDefinition step : steps) {
            if (step.getOrder() == task.getSequence()) {
                return step;
            }
        }
        throw new IllegalStateException("No step definition found for task sequence " + task.getSequence());
    }

    private void skipRemainingTasks(AgentJob job, AgentTask failedTask) {
        for (AgentTask task : job.getTasks()) {
            if (task.getSequence() > failedTask.getSequence()) {
                task.skip();
                taskStateLogger.logTaskSkipped(task);
            }
        }
    }
}
