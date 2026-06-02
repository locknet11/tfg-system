package com.spulido.tfg.domain.replication.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateReplicationRequest {

    @NotBlank
    private String targetIp;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer targetPort;

    @NotBlank
    private String exploitId;

    @NotBlank
    private String cveId;

    @NotBlank
    private String serviceName;

    @NotBlank
    private String serviceVersion;

    @NotBlank
    private String severity;
}
