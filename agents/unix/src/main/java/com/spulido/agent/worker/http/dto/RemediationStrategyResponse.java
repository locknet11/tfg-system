package com.spulido.agent.worker.http.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving a remediation strategy response from the central platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationStrategyResponse {
    private boolean found;
    private String remediationType;
    private String action;
    private String targetVersion;
    private String serviceName;
    private boolean requiresReboot;
    private List<String> preCheckCommands;
    private List<String> fixCommands;
    private List<String> postCheckCommands;
    private String notes;
}
