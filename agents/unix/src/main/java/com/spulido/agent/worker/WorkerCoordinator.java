package com.spulido.agent.worker;

import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.spulido.agent.domain.task.AgentJob;
import com.spulido.agent.domain.task.JobStatus;
import com.spulido.agent.domain.task.TaskDefinition;
import com.spulido.agent.utils.AgentLifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkerCoordinator {

    private final ThreadPoolTaskExecutor executor;
    private final TaskExecutionService taskExecutionService;
    private final AgentLifecycle agentLifecycle;
    private RestTemplate restTemplate = new RestTemplate();

    int counter = 0;

    @Scheduled(fixedDelay = 10000)
    public void pollCentralPlatform() {
        log.info("Polling central platform for jobs");
        executor.submit(this::runJob);
        counter++;
    }

    private void runJob() {
        List<TaskDefinition> steps = buildJobSteps();
        if (steps.isEmpty()) {
            log.info("No jobs to execute");
            return;
        }

        String jobId = "job-" + counter;
        log.info("Executing job: {}", jobId);

        AgentJob job = taskExecutionService.executeJob(jobId, steps);

        if (job.getStatus() == JobStatus.FAILED) {
            log.warn("Job {} failed: {}", job.getJobId(), job.getFailureReason());
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("Job {} completed successfully", job.getJobId());
        }
    }

    private List<TaskDefinition> buildJobSteps() {
        return Arrays.asList(
                new TaskDefinition("step-1", 0, "echo hello", 30),
                new TaskDefinition("step-2", 1, "echo world", 30));
    }
}
