package com.spulido.tfg.domain.replication.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.replication.model.dto.CreateReplicationRequest;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestResponse;

public interface ReplicationRequestService {

    ReplicationRequestResponse createRequest(CreateReplicationRequest request, String parentAgentId, String organizationId, String projectId);

    ReplicationRequestResponse getRequestStatus(String requestId, String agentId);

    ReplicationRequest getRequest(String requestId);

    List<ReplicationRequest> findDuplicateRequests(String targetIp, String exploitId);

    Page<ReplicationRequest> findAllScoped(Pageable pageable);

    Page<ReplicationRequest> findAllScopedByStatus(ReplicationRequestStatus status, Pageable pageable);

    Page<ReplicationRequest> findAllScopedBySeverity(String severity, Pageable pageable);

    Page<ReplicationRequest> findAllScopedByStatusAndSeverity(ReplicationRequestStatus status, String severity, Pageable pageable);

    void approveRequest(String requestId, String userId);

    void denyRequest(String requestId, String userId);

    void expireStaleRequests();

    ReplicationRequest findByToken(String token);
}
