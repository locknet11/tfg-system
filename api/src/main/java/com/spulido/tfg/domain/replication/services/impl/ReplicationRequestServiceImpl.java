package com.spulido.tfg.domain.replication.services.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.replication.db.ReplicationRequestRepository;
import com.spulido.tfg.domain.replication.exception.ReplicationException;
import com.spulido.tfg.domain.replication.model.ReplicationApprovalMode;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.replication.model.dto.CreateReplicationRequest;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestResponse;
import com.spulido.tfg.domain.replication.services.ReplicationPolicyService;
import com.spulido.tfg.domain.replication.services.ReplicationRequestService;
import com.spulido.tfg.domain.alerts.model.AlertEvent;
import com.spulido.tfg.domain.alerts.services.AlertTriggerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplicationRequestServiceImpl implements ReplicationRequestService {

    private final ReplicationRequestRepository repository;
    private final ReplicationPolicyService policyService;
    private final AgentRepository agentRepository;
    private final AlertTriggerService alertTriggerService;

    @Value("${agent.central.url:http://localhost:8080}")
    private String centralUrl;

    @Override
    public ReplicationRequestResponse createRequest(CreateReplicationRequest request, String parentAgentId,
                                                    String organizationId, String projectId) {
        List<ReplicationRequest> duplicates = repository.findDuplicateRequests(
                request.getTargetIp(), request.getExploitId());
        if (!duplicates.isEmpty()) {
            // A prior request is already in-flight for this target + exploit; return its
            // token set so the requesting agent can carry on with the transfer chain.
            // Without this, TRANSFER_AGENT has no download URL and the auto-replication
            // pipeline stops at REQUEST_REPLICATION.
            ReplicationRequest existing = duplicates.get(0);
            return buildResponse(existing);
        }

        ReplicationApprovalMode policy = policyService.evaluatePolicy(projectId, request.getSeverity());

        ReplicationRequest replicationRequest = new ReplicationRequest();
        replicationRequest.setParentAgentId(parentAgentId);
        replicationRequest.setTargetIp(request.getTargetIp());
        replicationRequest.setTargetPort(request.getTargetPort());
        replicationRequest.setExploitId(request.getExploitId());
        replicationRequest.setCveId(request.getCveId());
        replicationRequest.setServiceName(request.getServiceName());
        replicationRequest.setServiceVersion(request.getServiceVersion());
        replicationRequest.setSeverity(request.getSeverity());
        replicationRequest.setPolicy(policy);
        replicationRequest.setOrganizationId(organizationId);
        replicationRequest.setProjectId(projectId);
        replicationRequest.setCreatedAt(LocalDateTime.now());
        replicationRequest.setUpdatedAt(LocalDateTime.now());

        if (policy == ReplicationApprovalMode.AUTO_APPROVE) {
            approve(replicationRequest);
        } else {
            replicationRequest.setStatus(ReplicationRequestStatus.PENDING);
            replicationRequest.setExpiresAt(LocalDateTime.now().plusHours(1));
            repository.save(replicationRequest);
        }

        try {
            AlertEvent event = AlertEvent.builder()
                    .type(com.spulido.tfg.domain.alerts.model.AlertEvent.AlertEventType.SCAN_COMPLETED)
                    .severity(request.getSeverity())
                    .timestamp(Instant.now())
                    .organizationId(organizationId)
                    .projectId(projectId)
                    .build();
            alertTriggerService.checkAndTrigger(event);
        } catch (Exception e) {
            log.warn("Failed to trigger alert for replication request", e);
        }

        return buildResponse(replicationRequest);
    }

    @Override
    public ReplicationRequestResponse getRequestStatus(String requestId, String agentId) {
        ReplicationRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_FOUND,
                        "Replication request not found: " + requestId));

        if (!agentId.equals(request.getParentAgentId())) {
            throw new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_FOUND,
                    "Replication request not found for this agent: " + requestId);
        }

        return buildResponse(request);
    }

    @Override
    public ReplicationRequest getRequest(String requestId) {
        return repository.findById(requestId)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_FOUND,
                        "Replication request not found: " + requestId));
    }

    @Override
    public List<ReplicationRequest> findDuplicateRequests(String targetIp, String exploitId) {
        return repository.findDuplicateRequests(targetIp, exploitId);
    }

    @Override
    public Page<ReplicationRequest> findAllScoped(Pageable pageable) {
        return repository.findAllScoped(pageable);
    }

    @Override
    public Page<ReplicationRequest> findAllScopedByStatus(ReplicationRequestStatus status, Pageable pageable) {
        return repository.findAllScopedByStatus(status, pageable);
    }

    @Override
    public Page<ReplicationRequest> findAllScopedBySeverity(String severity, Pageable pageable) {
        return repository.findAllScopedBySeverity(severity, pageable);
    }

    @Override
    public Page<ReplicationRequest> findAllScopedByStatusAndSeverity(ReplicationRequestStatus status, String severity,
                                                                     Pageable pageable) {
        return repository.findAllScopedByStatusAndSeverity(status, severity, pageable);
    }

    @Override
    public void approveRequest(String requestId, String userId) {
        ReplicationRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_FOUND,
                        "Replication request not found: " + requestId));

        if (request.getStatus() != ReplicationRequestStatus.PENDING) {
            throw new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_PENDING,
                    "Replication request is not in PENDING status: " + requestId);
        }

        approve(request);
        request.setApprovedBy(userId);
        request.setResolvedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        repository.save(request);

        try {
            Agent agent = agentRepository.findById(request.getParentAgentId()).orElse(null);
            String agentName = agent != null ? agent.getName() : request.getParentAgentId();
            AlertEvent event = AlertEvent.builder()
                    .type(com.spulido.tfg.domain.alerts.model.AlertEvent.AlertEventType.SCAN_COMPLETED)
                    .severity(request.getSeverity())
                    .timestamp(Instant.now())
                    .organizationId(request.getOrganizationId())
                    .projectId(request.getProjectId())
                    .build();
            alertTriggerService.checkAndTrigger(event);
        } catch (Exception e) {
            log.warn("Failed to trigger alert for approval", e);
        }
    }

    @Override
    public void denyRequest(String requestId, String userId) {
        ReplicationRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_FOUND,
                        "Replication request not found: " + requestId));

        if (request.getStatus() != ReplicationRequestStatus.PENDING) {
            throw new ReplicationException(ErrorCode.REPLICATION_REQUEST_NOT_PENDING,
                    "Replication request is not in PENDING status: " + requestId);
        }

        request.setStatus(ReplicationRequestStatus.DENIED);
        request.setApprovedBy(userId);
        request.setResolvedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        repository.save(request);
    }

    @Override
    public void expireStaleRequests() {
        List<ReplicationRequest> staleRequests = repository.findByStatusAndExpiresAtBefore(
                ReplicationRequestStatus.PENDING, LocalDateTime.now());
        for (ReplicationRequest request : staleRequests) {
            request.setStatus(ReplicationRequestStatus.EXPIRED);
            request.setResolvedAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());
            repository.save(request);
        }
        if (!staleRequests.isEmpty()) {
            log.info("Expired {} stale replication requests", staleRequests.size());
        }
    }

    @Override
    public ReplicationRequest findByToken(String token) {
        return repository.findByReplicationToken(token)
                .orElseThrow(() -> new ReplicationException(ErrorCode.REPLICATION_TOKEN_NOT_FOUND,
                        "Token not found: " + token));
    }

    private void approve(ReplicationRequest request) {
        String token = UUID.randomUUID().toString();
        request.setStatus(ReplicationRequestStatus.APPROVED);
        request.setReplicationToken(token);
        request.setDownloadUrl(centralUrl + "/api/agent/binary/" + token);
        request.setPreauthCode("preauth-" + UUID.randomUUID().toString().substring(0, 12));
        request.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        request.setUpdatedAt(LocalDateTime.now());
        repository.save(request);
    }

    private ReplicationRequestResponse buildResponse(ReplicationRequest request) {
        ReplicationRequestResponse response = ReplicationRequestResponse.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .build();

        if (request.getStatus() == ReplicationRequestStatus.APPROVED) {
            response.setReplicationToken(request.getReplicationToken());
            response.setDownloadUrl(request.getDownloadUrl());
            response.setPreauthCode(request.getPreauthCode());
            response.setCentralUrl(centralUrl);
        }

        return response;
    }
}
