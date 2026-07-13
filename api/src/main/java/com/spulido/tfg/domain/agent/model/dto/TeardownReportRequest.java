package com.spulido.tfg.domain.agent.model.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Inbound body of an agent's teardown-outcome report ({@code POST /api/agent/comm/teardown}).
 */
@Getter
@Setter
@NoArgsConstructor
public class TeardownReportRequest {

    @NotBlank
    private String agentId;

    @NotBlank
    private String trigger;

    private String timestamp;

    @NotNull
    private List<ArtifactResult> results;

    private String binaryRemoval;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ArtifactResult {

        @NotBlank
        private String type;

        private String path;

        @NotBlank
        private String status;

        private String detail;
    }
}
