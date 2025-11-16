package com.spulido.agent.worker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.spulido.agent.utils.AgentLifecycle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkerCoordinator {

    private final ThreadPoolTaskExecutor executor;
    private RestTemplate restTemplate = new RestTemplate();
    private final AgentLifecycle agentLifecycle;

    int counter = 0;

    @Scheduled(fixedDelay = 10000)
    public void pollCentralPlatform() {
        log.info("Iniciando poll de trabajos en la plataforma central");
        executor.submit(() -> runJob(null));
        counter++;
    }

    private void runJob(String jobData) {
        // here we will run a massive switch with options
        log.info("Running job");
        if (counter == 3) {
            agentLifecycle.stop();
        }
    }
}
