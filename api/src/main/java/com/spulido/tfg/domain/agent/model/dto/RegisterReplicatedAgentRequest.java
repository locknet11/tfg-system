package com.spulido.tfg.domain.agent.model.dto;

import com.spulido.tfg.domain.target.model.OperatingSystem;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Registration payload for an agent that arrived via autoreplication onto a previously
 * unregistered host. Unlike {@link RegisterAgentRequest}, no Target ID is supplied: the
 * central platform creates the target from the reported hostname. Authorization is carried
 * by the replication preauth code (issued when the replication request was approved).
 */
@Getter
@Setter
public class RegisterReplicatedAgentRequest {

    @NotBlank(message = "Preauth code is required")
    private String preauthCode;

    private String hostname;

    private OperatingSystem os;

    private String clientIp;
}
