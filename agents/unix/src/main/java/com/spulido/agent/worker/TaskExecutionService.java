package com.spulido.agent.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.AgentTask;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.StepResult;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.step.StepHandler;

public class TaskExecutionService {

    private final CommandExecutor commandExecutor;
    private final TaskStateLogger taskStateLogger;
    private final Map<StepAction, StepHandler> stepHandlers;

    public TaskExecutionService(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.taskStateLogger = new TaskStateLogger();
        this.stepHandlers = new HashMap<>();
    }

    public TaskExecutionService(CommandExecutor commandExecutor,
                                Map<StepAction, StepHandler> stepHandlers) {
        this.commandExecutor = commandExecutor;
        this.taskStateLogger = new TaskStateLogger();
        this.stepHandlers = stepHandlers != null ? stepHandlers : new HashMap<>();
    }

    public AgentJob executeJob(String jobId, List<TaskDefinition> steps) {
        return executeJob(jobId, steps, List.of(), null);
    }

    public AgentJob executeJob(String jobId, List<TaskDefinition> steps, List<StepAction> actions) {
        return executeJob(jobId, steps, actions, null);
    }

    public AgentJob executeJob(String jobId, List<TaskDefinition> steps, List<StepAction> actions,
                                String targetIp) {
        JobValidator.validate(steps);

        List<AgentTask> tasks = new ArrayList<>();
        for (TaskDefinition step : steps) {
            String taskId = UUID.randomUUID().toString();
            tasks.add(new AgentTask(taskId, jobId, step.getOrder(), step.getCommand()));
        }

        AgentJob job = new AgentJob(jobId, tasks);
        taskStateLogger.logJobStarted(job);
        job.start();

        Map<StepAction, StepResult> context = new HashMap<>();

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

            StepAction stepAction = getActionForOrder(matchingStep.getOrder(), actions);
            if (stepAction != null) {
                StepHandler handler = stepHandlers.get(stepAction);
                if (handler != null) {
                    StepResult stepResult = handler.handle(stepAction, context, targetIp);
                    context.put(stepAction, stepResult);

                    if (stepResult.isSkipped()) {
                        taskStateLogger.logTaskCompleted(task);
                        continue;
                    }

                    if (!stepResult.isSuccess()) {
                        skipRemainingTasks(job, task);
                        job.fail("Task " + task.getTaskId() + " failed at step handler: "
                                + String.join(", ", stepResult.getLogs()));
                        taskStateLogger.logJobFailed(job);
                        return job;
                    }
                }
            }
        }

        job.complete();
        taskStateLogger.logJobCompleted(job);
        return job;
    }

    private StepAction getActionForOrder(int order, List<StepAction> actions) {
        if (order >= 0 && order < actions.size()) {
            return actions.get(order);
        }
        return null;
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
