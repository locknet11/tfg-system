package com.spulido.tfg.domain.remediation.model.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationInfo {

    private String id;
    private String vulnerabilityRecordId;
    private String cveId;
    private String targetId;
    private String targetName;
    private String agentId;
    private String planId;
    private String remediationType;
    private String status;
    private String packageName;
    private String packageVersionBefore;
    private String packageVersionAfter;
    private String actionDescription;
    private List<String> preCheckLogs;
    private List<String> executionLogs;
    private List<String> postCheckLogs;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String rollbackHint;
    private String skipReason;
    private Instant createdAt;
    private Instant updatedAt;
}
