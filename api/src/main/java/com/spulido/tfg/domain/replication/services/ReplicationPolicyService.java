package com.spulido.tfg.domain.replication.services;

import com.spulido.tfg.domain.replication.model.ReplicationApprovalMode;

public interface ReplicationPolicyService {

    ReplicationApprovalMode evaluatePolicy(String projectId, String severity);

    boolean isSeverityAtOrAbove(String severity, String threshold);
}
