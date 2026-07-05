package com.spulido.agent.worker.http.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reporting remediation results to the central platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationReportRequest {
    private String cveId;
    private String targetId;
    private String remediationType;
    private String status;
    private String packageName;
    private String packageVersionBefore;
    private String packageVersionAfter;
    private String actionDescription;
    private List<String> preCheckLogs;
    private List<String> executionLogs;
    private List<String> postCheckLogs;
    private String errorMessage;
    private String rollbackHint;
    private String skipReason;
}
