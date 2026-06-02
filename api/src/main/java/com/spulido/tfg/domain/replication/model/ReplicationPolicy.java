package com.spulido.tfg.domain.replication.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReplicationPolicy {

    private ReplicationApprovalMode mode = ReplicationApprovalMode.MANUAL_APPROVE;

    private String minSeverity;

    private Boolean notifyAdmin = true;
}
