package com.spulido.tfg.domain.remediation.services.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.alerts.model.AlertEvent;
import com.spulido.tfg.domain.alerts.services.AlertTriggerService;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.exception.RemediationException;
import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.remediation.model.dto.RemediationInfo;
import com.spulido.tfg.domain.remediation.model.dto.RemediationReportRequest;
import com.spulido.tfg.domain.remediation.model.dto.RemediationStatistics;
import com.spulido.tfg.domain.remediation.services.RemediationService;
import com.spulido.tfg.common.exception.ErrorCode;

@Service
public class RemediationServiceImpl implements RemediationService {

    private static final Logger log = LoggerFactory.getLogger(RemediationServiceImpl.class);

    private final RemediationRecordRepository repository;
    private final AlertTriggerService alertTriggerService;

    public RemediationServiceImpl(RemediationRecordRepository repository, AlertTriggerService alertTriggerService) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.alertTriggerService = Objects.requireNonNull(alertTriggerService, "alertTriggerService must not be null");
    }

    @Override
    public RemediationRecord createRemediation(RemediationReportRequest request, String agentId) {
        if (request == null) {
            throw new RemediationException("Remediation report request cannot be null",
                    ErrorCode.BAD_REQUEST);
        }
        if (agentId == null || agentId.isBlank()) {
            throw new RemediationException("Agent ID cannot be null or blank",
                    ErrorCode.BAD_REQUEST);
        }

        log.info("Creating remediation record for CVE: {}, target: {}, agent: {}",
                request.getCveId(), request.getTargetId(), agentId);

        RemediationStatus status = parseStatus(request.getStatus());

        RemediationRecord record = RemediationRecord.builder()
                .cveId(request.getCveId())
                .targetId(request.getTargetId())
                .agentId(agentId)
                .remediationType(parseType(request.getRemediationType()))
                .status(status)
                .packageName(request.getPackageName())
                .packageVersionBefore(request.getPackageVersionBefore())
                .packageVersionAfter(request.getPackageVersionAfter())
                .actionDescription(request.getActionDescription())
                .preCheckLogs(request.getPreCheckLogs())
                .executionLogs(request.getExecutionLogs())
                .postCheckLogs(request.getPostCheckLogs())
                .errorMessage(request.getErrorMessage())
                .rollbackHint(request.getRollbackHint())
                .skipReason(request.getSkipReason())
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();

        RemediationRecord saved = repository.save(record);

        log.info("Remediation record created: {}, status: {}", saved.getId(), status);

        // Trigger alert event for remediation completion
        triggerRemediationAlert(saved);

        return saved;
    }

    private void triggerRemediationAlert(RemediationRecord record) {
        if (record.getStatus() == null) {
            return;
        }

        // Only trigger alerts for terminal states (SUCCESS, FAILED, PENDING_REBOOT, SKIPPED)
        if (record.getStatus() != RemediationStatus.SUCCESS &&
            record.getStatus() != RemediationStatus.FAILED &&
            record.getStatus() != RemediationStatus.PENDING_REBOOT &&
            record.getStatus() != RemediationStatus.SKIPPED) {
            return;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "remediationId", record.getId() != null ? record.getId() : "",
                    "cveId", record.getCveId() != null ? record.getCveId() : "",
                    "targetId", record.getTargetId() != null ? record.getTargetId() : "",
                    "agentId", record.getAgentId() != null ? record.getAgentId() : "",
                    "packageName", record.getPackageName() != null ? record.getPackageName() : "",
                    "status", record.getStatus().name(),
                    "remediationType", record.getRemediationType() != null ? record.getRemediationType().name() : "UNKNOWN"
            );

            String severity = determineSeverity(record.getStatus());

            AlertEvent event = AlertEvent.builder()
                    .type(AlertEvent.AlertEventType.REMEDIATION_COMPLETED)
                    .severity(severity)
                    .payload(payload)
                    .timestamp(record.getCompletedAt() != null ? record.getCompletedAt() : Instant.now())
                    .organizationId(getContextOrgId())
                    .projectId(getContextProjectId())
                    .build();

            alertTriggerService.checkAndTrigger(event);

            log.info("Triggered remediation alert for record: {}, status: {}", 
                    record.getId(), record.getStatus());
        } catch (Exception e) {
            log.error("Failed to trigger alert for remediation record: {}: {}", 
                    record.getId(), e.getMessage(), e);
            // Don't throw - alert failures shouldn't block remediation
        }
    }

    private String determineSeverity(RemediationStatus status) {
        return switch (status) {
            case FAILED -> "CRITICAL";
            case PENDING_REBOOT, SKIPPED -> "WARNING";
            case SUCCESS -> "INFO";
            default -> "INFO";
        };
    }

    @Override
    public RemediationRecord findById(String id) {
        if (id == null || id.isBlank()) {
            throw new RemediationException("Remediation ID cannot be null or blank",
                    ErrorCode.BAD_REQUEST);
        }

        return repository.findById(id)
                .orElseThrow(() -> new RemediationException(
                        "Remediation record not found: " + id,
                        ErrorCode.REMEDIATION_NOT_FOUND));
    }

    @Override
    public Page<RemediationRecord> findAll(RemediationStatus status, String targetId, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (status != null && targetId != null && !targetId.isBlank()) {
            log.debug("Querying remediations with status: {} and targetId: {}", status, targetId);
            return repository.findByOrganizationIdAndProjectIdAndTargetId(
                    getContextOrgId(), getContextProjectId(), targetId, pageable);
        } else if (status != null) {
            log.debug("Querying remediations with status: {}", status);
            return repository.findByOrganizationIdAndProjectIdAndStatus(
                    getContextOrgId(), getContextProjectId(), status, pageable);
        } else if (targetId != null && !targetId.isBlank()) {
            log.debug("Querying remediations with targetId: {}", targetId);
            return repository.findByOrganizationIdAndProjectIdAndTargetId(
                    getContextOrgId(), getContextProjectId(), targetId, pageable);
        } else {
            log.debug("Querying all remediations");
            return repository.findByOrganizationIdAndProjectId(
                    getContextOrgId(), getContextProjectId(), pageable);
        }
    }

    @Override
    public RemediationStatistics getStatistics() {
        String orgId = getContextOrgId();
        String projectId = getContextProjectId();

        Map<String, Long> byStatus = Map.of(
                "SUCCESS", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.SUCCESS),
                "FAILED", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.FAILED),
                "PENDING", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.PENDING),
                "IN_PROGRESS", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.IN_PROGRESS),
                "PENDING_REBOOT", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.PENDING_REBOOT),
                "SKIPPED", repository.countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.SKIPPED)
        );

        long totalCount = byStatus.values().stream().mapToLong(Long::longValue).sum();

        List<RemediationStatistics.RecentActivity> recentActivity = repository
                .findTop5ByOrganizationIdAndProjectIdOrderByCompletedAtDesc(orgId, projectId)
                .stream()
                .map(record -> RemediationStatistics.RecentActivity.builder()
                        .id(record.getId())
                        .cveId(record.getCveId())
                        .status(record.getStatus().name())
                        .completedAt(record.getCompletedAt() != null ? record.getCompletedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return RemediationStatistics.builder()
                .totalCount(totalCount)
                .byStatus(byStatus)
                .meanTimeToRemediateSeconds(0)
                .recentActivity(recentActivity)
                .build();
    }

    @Override
    public RemediationInfo toInfo(RemediationRecord record) {
        if (record == null) {
            return null;
        }

        return RemediationInfo.builder()
                .id(record.getId())
                .vulnerabilityRecordId(record.getVulnerabilityRecordId())
                .cveId(record.getCveId())
                .targetId(record.getTargetId())
                .agentId(record.getAgentId())
                .planId(record.getPlanId())
                .remediationType(record.getRemediationType() != null ? record.getRemediationType().name() : null)
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .packageName(record.getPackageName())
                .packageVersionBefore(record.getPackageVersionBefore())
                .packageVersionAfter(record.getPackageVersionAfter())
                .actionDescription(record.getActionDescription())
                .preCheckLogs(record.getPreCheckLogs())
                .executionLogs(record.getExecutionLogs())
                .postCheckLogs(record.getPostCheckLogs())
                .startedAt(record.getStartedAt())
                .completedAt(record.getCompletedAt())
                .errorMessage(record.getErrorMessage())
                .rollbackHint(record.getRollbackHint())
                .skipReason(record.getSkipReason())
                .createdAt(record.getCreatedAt() != null ? record.getCreatedAt().toInstant(java.time.ZoneOffset.UTC) : null)
                .updatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().toInstant(java.time.ZoneOffset.UTC) : null)
                .build();
    }

    private RemediationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return RemediationStatus.PENDING;
        }
        try {
            return RemediationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid remediation status: {}, defaulting to PENDING", status);
            return RemediationStatus.PENDING;
        }
    }

    private com.spulido.tfg.domain.remediation.model.RemediationType parseType(String type) {
        if (type == null || type.isBlank()) {
            return com.spulido.tfg.domain.remediation.model.RemediationType.UNKNOWN;
        }
        try {
            return com.spulido.tfg.domain.remediation.model.RemediationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid remediation type: {}, defaulting to UNKNOWN", type);
            return com.spulido.tfg.domain.remediation.model.RemediationType.UNKNOWN;
        }
    }

    private String getContextOrgId() {
        String orgId = com.spulido.tfg.common.context.ProjectContext.getOrganizationId();
        return orgId != null ? orgId : "";
    }

    private String getContextProjectId() {
        String projectId = com.spulido.tfg.common.context.ProjectContext.getProjectId();
        return projectId != null ? projectId : "";
    }
}
