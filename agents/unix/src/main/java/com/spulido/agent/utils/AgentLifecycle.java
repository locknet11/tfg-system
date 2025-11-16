package com.spulido.agent.utils;

import org.springframework.stereotype.Component;

import com.spulido.agent.AgentApplication;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AgentLifecycle {

    public void stop() {
        log.info("Shutting down agent");
        AgentApplication.shutdown();
    }
}