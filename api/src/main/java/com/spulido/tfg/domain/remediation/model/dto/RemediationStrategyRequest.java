package com.spulido.tfg.domain.remediation.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemediationStrategyRequest {

    @NotBlank(message = "cveId must not be blank")
    @Pattern(regexp = "CVE-\\d{4}-\\d+", message = "Invalid CVE ID format")
    private String cveId;

    @NotBlank(message = "packageName must not be blank")
    private String packageName;

    @NotBlank(message = "currentVersion must not be blank")
    private String currentVersion;

    @NotBlank(message = "operatingSystem must not be blank")
    private String operatingSystem;
}
