package com.spulido.tfg.domain.replication.model.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spulido.tfg.domain.replication.model.ReplicationApprovalMode;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplicationRequestInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("parentAgentId")
    private String parentAgentId;

    @JsonProperty("parentAgentName")
    private String parentAgentName;

    @JsonProperty("targetIp")
    private String targetIp;

    @JsonProperty("targetPort")
    private Integer targetPort;

    @JsonProperty("exploitId")
    private String exploitId;

    @JsonProperty("cveId")
    private String cveId;

    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("serviceVersion")
    private String serviceVersion;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("status")
    private ReplicationRequestStatus status;

    @JsonProperty("policy")
    private ReplicationApprovalMode policy;

    @JsonProperty("approvedBy")
    private String approvedBy;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    @JsonProperty("resolvedAt")
    private LocalDateTime resolvedAt;
}
