package com.spulido.tfg.domain.replication.services.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.project.db.ProjectRepository;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.replication.model.ReplicationApprovalMode;
import com.spulido.tfg.domain.replication.model.ReplicationPolicy;
import com.spulido.tfg.domain.replication.services.ReplicationPolicyService;
import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.replication.exception.ReplicationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReplicationPolicyServiceImpl implements ReplicationPolicyService {

    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
            "CRITICAL", 4,
            "HIGH", 3,
            "MEDIUM", 2,
            "LOW", 1
    );

    private final ProjectRepository projectRepository;

    @Override
    public ReplicationApprovalMode evaluatePolicy(String projectId, String severity) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_POLICY_NOT_FOUND,
                        "Project not found: " + projectId));

        ReplicationPolicy policy = project.getReplicationPolicy();
        if (policy == null || policy.getMode() == null) {
            return ReplicationApprovalMode.MANUAL_APPROVE;
        }

        if (policy.getMode() == ReplicationApprovalMode.MANUAL_APPROVE) {
            return ReplicationApprovalMode.MANUAL_APPROVE;
        }

        if (policy.getMinSeverity() != null && !isSeverityAtOrAbove(severity, policy.getMinSeverity())) {
            return ReplicationApprovalMode.MANUAL_APPROVE;
        }

        return ReplicationApprovalMode.AUTO_APPROVE;
    }

    @Override
    public boolean isSeverityAtOrAbove(String severity, String threshold) {
        Integer severityOrder = SEVERITY_ORDER.get(severity);
        Integer thresholdOrder = SEVERITY_ORDER.get(threshold);
        if (severityOrder == null || thresholdOrder == null) {
            return false;
        }
        return severityOrder >= thresholdOrder;
    }
}
