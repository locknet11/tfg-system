package com.spulido.tfg.domain.agent.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterAgentRequest {
    private String organizationIdentifier;
    private String projectIdentifier;
    private String targetUniqueId;
    private String clientIp;
}
