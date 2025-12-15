package com.spulido.tfg.domain.agent.model.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HeartbeatResponse {
    private String agentId;
    private String status;
    private LocalDateTime lastConnection;
    private boolean hasPlan;
}
