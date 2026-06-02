package com.spulido.tfg.domain.replication.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestInfo;
import com.spulido.tfg.domain.replication.model.dto.ReplicationRequestResponse;
import com.spulido.tfg.domain.replication.services.ReplicationRequestService;
import com.spulido.tfg.domain.replication.services.ReplicationRequestServiceMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ReplicationAdminController {

    private final ReplicationRequestService service;
    private final ReplicationRequestServiceMapper mapper;

    @GetMapping("/api/replication-requests")
    public ResponseEntity<ResponseList<ReplicationRequestInfo>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {

        PageRequest pageable = PageRequest.of(page, size);
        Page<ReplicationRequest> requests;

        if (status != null && severity != null) {
            ReplicationRequestStatus reqStatus = ReplicationRequestStatus.valueOf(status);
            requests = service.findAllScopedByStatusAndSeverity(reqStatus, severity, pageable);
        } else if (status != null) {
            ReplicationRequestStatus reqStatus = ReplicationRequestStatus.valueOf(status);
            requests = service.findAllScopedByStatus(reqStatus, pageable);
        } else if (severity != null) {
            requests = service.findAllScopedBySeverity(severity, pageable);
        } else {
            requests = service.findAllScoped(pageable);
        }

        return ResponseEntity.ok(mapper.toResponseList(requests));
    }

    @PutMapping("/api/replication-requests/{id}/approve")
    public ResponseEntity<ReplicationRequestResponse> approve(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        service.approveRequest(id, userId);

        ReplicationRequest request = service.getRequest(id);
        ReplicationRequestResponse response = ReplicationRequestResponse.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .replicationToken(request.getReplicationToken())
                .downloadUrl(request.getDownloadUrl())
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/replication-requests/{id}/deny")
    public ResponseEntity<ReplicationRequestResponse> deny(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        service.denyRequest(id, userId);

        ReplicationRequest request = service.getRequest(id);
        ReplicationRequestResponse response = ReplicationRequestResponse.builder()
                .id(request.getId())
                .status(request.getStatus().name())
                .build();

        return ResponseEntity.ok(response);
    }
}
