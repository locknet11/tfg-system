package com.spulido.agent.worker.http.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving remediation report response from the central platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationReportResponse {
    private String remediationId;
    private String status;
}
