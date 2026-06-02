package com.spulido.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.TaskExecutionService;
import com.spulido.agent.worker.WorkerCoordinator;
import com.spulido.agent.worker.http.AgentHttpClient;

@Configuration
@EnableAsync
public class WorkerPoolConfig {

    @Bean("workerExecutor")
    public ThreadPoolTaskExecutor workerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CommandExecutor commandExecutor() {
        return (command, timeoutSeconds) -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("sh", "-c", command);
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return com.spulido.agent.domain.task.TaskResult.failure(
                            "cmd-" + System.currentTimeMillis(),
                            "Command timed out",
                            "Timed out after " + timeoutSeconds + " seconds");
                }

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.exitValue();

                if (exitCode == 0) {
                    return com.spulido.agent.domain.task.TaskResult.success(
                            "cmd-" + System.currentTimeMillis(),
                            "Command completed. Output: " + output);
                } else {
                    return com.spulido.agent.domain.task.TaskResult.failure(
                            "cmd-" + System.currentTimeMillis(),
                            "Command failed with exit code " + exitCode,
                            "Exit code: " + exitCode + ". Output: " + output);
                }
            } catch (Exception e) {
                return com.spulido.agent.domain.task.TaskResult.failure(
                        "cmd-" + System.currentTimeMillis(),
                        "Command execution error",
                        e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        };
    }

    @Bean
    public TaskExecutionService taskExecutionService(CommandExecutor commandExecutor,
                                                      AgentHttpClient agentHttpClient,
                                                      AgentConfig agentConfig) {
        return new TaskExecutionService(commandExecutor,
                WorkerCoordinator.createDefaultStepHandlers(agentHttpClient, commandExecutor, agentConfig));
    }
}
