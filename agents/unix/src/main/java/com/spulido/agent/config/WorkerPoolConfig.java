package com.spulido.agent.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import com.spulido.agent.remote.RemoteCommandExecutor;
import com.spulido.agent.remote.SshRemoteCommandExecutor;
import com.spulido.agent.remote.SshSessionProvisioner;
import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.ScriptTemplateService;
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
    public RestTemplate restTemplate(AgentConfig agentConfig) {
        RestTemplate restTemplate = new RestTemplate();

        // Add interceptor to inject agent authentication headers
        ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
            String apiKey = agentConfig.getApiKey();
            String agentId = agentConfig.getAgentId();

            if (apiKey != null && !apiKey.isEmpty() && agentId != null && !agentId.isEmpty()) {
                HttpHeaders headers = request.getHeaders();
                headers.set("X-Agent-Api-Key", apiKey);
                headers.set("X-Agent-Id", agentId);
            }

            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(Collections.singletonList(authInterceptor));
        return restTemplate;
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
    public RemoteCommandExecutor remoteCommandExecutor(AgentConfig agentConfig) {
        return new SshRemoteCommandExecutor(agentConfig);
    }

    @Bean
    public SshSessionProvisioner sshSessionProvisioner(AgentConfig agentConfig) {
        return new SshSessionProvisioner(agentConfig);
    }

    @Bean
    public TaskExecutionService taskExecutionService(CommandExecutor commandExecutor,
                                                      AgentHttpClient agentHttpClient,
                                                      AgentConfig agentConfig,
                                                      ScriptTemplateService scriptTemplateService,
                                                      RemoteCommandExecutor remoteCommandExecutor,
                                                      SshSessionProvisioner sshSessionProvisioner) {
        return new TaskExecutionService(commandExecutor,
                WorkerCoordinator.createDefaultStepHandlers(agentHttpClient, commandExecutor, agentConfig,
                        scriptTemplateService, remoteCommandExecutor, sshSessionProvisioner));
    }
}
