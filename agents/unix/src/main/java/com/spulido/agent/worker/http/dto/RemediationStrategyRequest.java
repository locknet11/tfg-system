package com.spulido.agent.worker.http.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for requesting a remediation strategy from the central platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationStrategyRequest {
    private String cveId;
    private String packageName;
    private String currentVersion;
    private String operatingSystem;
}
