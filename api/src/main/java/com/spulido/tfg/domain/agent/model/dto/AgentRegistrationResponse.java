package com.spulido.tfg.domain.agent.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentRegistrationResponse {
    private String agentId;
    private String targetId;
    private String ipAddress;
    private String status;
}
