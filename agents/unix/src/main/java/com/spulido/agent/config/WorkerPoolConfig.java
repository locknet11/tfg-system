package com.spulido.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.TaskExecutionService;

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
    public CommandExecutor commandExecutor() {
        return (command, timeoutSeconds) -> {
            throw new UnsupportedOperationException("Command execution not yet implemented");
        };
    }

    @Bean
    public TaskExecutionService taskExecutionService(CommandExecutor commandExecutor) {
        return new TaskExecutionService(commandExecutor);
    }
}
