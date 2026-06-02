package com.spulido.tfg.domain.replication.controller;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.replication.exception.ReplicationException;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.replication.services.AgentBinaryService;
import com.spulido.tfg.domain.replication.services.ReplicationRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AgentBinaryController {

    private final ReplicationRequestService requestService;
    private final AgentBinaryService binaryService;

    @GetMapping("/api/agent/binary/{replicationToken}")
    public ResponseEntity<byte[]> downloadBinary(@PathVariable String replicationToken) {
        ReplicationRequest request = requestService.findByToken(replicationToken);

        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(LocalDateTime.now())) {
            request.setStatus(ReplicationRequestStatus.EXPIRED);
            throw new ReplicationException(ErrorCode.REPLICATION_TOKEN_EXPIRED, "Token has expired");
        }

        byte[] binaryBytes = binaryService.getBinaryBytes();
        String manifest = binaryService.getSignedManifest();

        byte[] response = new byte[binaryBytes.length + manifest.length() + 2];
        System.arraycopy(binaryBytes, 0, response, 0, binaryBytes.length);
        response[binaryBytes.length] = '\n';
        System.arraycopy(manifest.getBytes(), 0, response, binaryBytes.length + 1, manifest.length());

        request.setStatus(ReplicationRequestStatus.DENIED);
        request.setResolvedAt(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "agent");
        headers.set("X-Blake3-Manifest", manifest);

        return ResponseEntity.ok().headers(headers).body(response);
    }
}
