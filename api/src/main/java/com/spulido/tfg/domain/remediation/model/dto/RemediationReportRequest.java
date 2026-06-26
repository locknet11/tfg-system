package com.spulido.tfg.domain.remediation.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemediationReportRequest {

    @NotBlank(message = "cveId must not be blank")
    @Pattern(regexp = "CVE-\\d{4}-\\d+", message = "Invalid CVE ID format")
    private String cveId;

    @NotBlank(message = "targetId must not be blank")
    private String targetId;

    @NotBlank(message = "remediationType must not be blank")
    private String remediationType;

    @NotNull(message = "status must not be null")
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
}
