package com.spulido.tfg.domain.agent.monitoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "heartbeat")
@Getter
@Setter
public class HeartbeatConfig {

    private int timeoutSeconds = 120;
    private int schedulerDelayMs = 30000;
}
