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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutionService.class);

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
        return executeJob(jobId, steps, actions, targetIp, null);
    }

    /**
     * @param targetId the real Central target document id (distinct from targetIp, which is
     *                 the scannable address). Used to correct SERVICE_SCAN's targetId log entry
     *                 — nmap only knows the address, not Central's identifier — so downstream
     *                 steps (e.g. EXPLOITATION_KNOWLEDGE) report the id Central can look up.
     */
    public AgentJob executeJob(String jobId, List<TaskDefinition> steps, List<StepAction> actions,
                                String targetIp, String targetId) {
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
                    if (stepAction == StepAction.SERVICE_SCAN && targetId != null && !targetId.isBlank()) {
                        stepResult = withRealTargetId(stepResult, targetId);
                    }
                    context.put(stepAction, stepResult);

                    // After a successful NETWORK_SCAN that discovered live sibling hosts,
                    // redirect subsequent steps (SERVICE_SCAN, EXPLOITATION_KNOWLEDGE,
                    // EXECUTE_EXPLOIT, TRANSFER_AGENT) to the first discovered host that is
                    // not a local interface address — the agent should target the sibling it
                    // discovered, not its own assigned target.
                    if (stepAction == StepAction.NETWORK_SCAN && stepResult.isSuccess()) {
                        String sibling = resolveFirstDiscoveredSibling(stepResult, targetIp);
                        if (sibling != null) {
                            log.info("Redirecting subsequent steps to discovered sibling: {}", sibling);
                            targetIp = sibling;
                        }
                    }

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

    /**
     * SERVICE_SCAN only knows the scanned address, so it reports {@code targetId:<address>}.
     * Central's EXPLOITATION_KNOWLEDGE endpoint expects the real target document id there —
     * rewrite the log line with the id sourced from the plan response.
     */
    private StepResult withRealTargetId(StepResult result, String targetId) {
        List<String> logs = new ArrayList<>();
        boolean replaced = false;
        for (String line : result.getLogs()) {
            if (line.startsWith("targetId:")) {
                logs.add("targetId:" + targetId);
                replaced = true;
            } else {
                logs.add(line);
            }
        }
        if (!replaced) {
            return result;
        }
        return new StepResult(result.getAction(), result.getServices(), result.getScripts(), logs,
                result.isSuccess(), result.isSkipped());
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

    /**
     * Scans NETWORK_SCAN result logs for the first discovered host ({@code HOST_FOUND:<addr>})
     * that is not a local interface address and not equal to the original plan targetIp.
     * Returns null when no suitable sibling was found.
     */
    private String resolveFirstDiscoveredSibling(StepResult result, String originalTargetIp) {
        if (result.getLogs() == null) return null;
        java.util.Set<String> local = localInterfaceAddresses();
        if (originalTargetIp != null) {
            local.add(originalTargetIp.trim());
        }
        for (String line : result.getLogs()) {
            if (!line.startsWith("HOST_FOUND:")) continue;
            String addr = line.substring("HOST_FOUND:".length()).trim();
            int paren = addr.indexOf('(');
            if (paren > 0) {
                String inner = addr.substring(paren + 1, addr.indexOf(')', paren));
                if (!inner.isBlank()) addr = inner.trim();
            }
            if (!addr.isBlank() && !local.contains(addr)) {
                return addr;
            }
        }
        return null;
    }

    private static java.util.Set<String> localInterfaceAddresses() {
        java.util.Set<String> addrs = new java.util.HashSet<>();
        try {
            for (java.net.NetworkInterface nic : java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
                java.util.Enumeration<java.net.InetAddress> ia = nic.getInetAddresses();
                while (ia.hasMoreElements()) {
                    addrs.add(ia.nextElement().getHostAddress());
                }
            }
        } catch (Exception ignored) { }
        return addrs;
    }
}
