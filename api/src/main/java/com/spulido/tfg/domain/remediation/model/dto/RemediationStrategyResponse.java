package com.spulido.tfg.domain.remediation.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public static RemediationStrategyResponse notFound(String notes) {
        return RemediationStrategyResponse.builder()
                .found(false)
                .remediationType("UNKNOWN")
                .preCheckCommands(List.of())
                .fixCommands(List.of())
                .postCheckCommands(List.of())
                .notes(notes)
                .build();
    }
}
