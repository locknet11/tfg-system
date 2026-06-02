package com.spulido.tfg.domain.project.model.dto;

import com.spulido.tfg.domain.replication.model.ReplicationApprovalMode;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateReplicationPolicyRequest {

    @NotNull
    private ReplicationApprovalMode mode;

    private String minSeverity;

    @NotNull
    private Boolean notifyAdmin = true;
}
