package com.spulido.tfg.domain.agent.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spulido.tfg.domain.agent.model.AgentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AgentInfo {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private AgentStatus status;

    @JsonProperty("version")
    private String version;

    @JsonProperty("lastConnection")
    private LocalDateTime lastConnection;

    @JsonProperty("targetSystem")
    private String targetSystem;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("projectId")
    private String projectId;
}
