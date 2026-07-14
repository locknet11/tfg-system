package com.spulido.tfg.domain.replication.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.replication.services.AgentBinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serves the agent binary for one-time install script downloads.
 * Uses a short-lived install token (generated during agent registration)
 * that is consumed on first successful download.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AgentInstallBinaryController {

    private final AgentRepository agentRepository;
    private final AgentBinaryService binaryService;

    @GetMapping("/api/agent/binary/download/{installToken}")
    public ResponseEntity<?> downloadBinaryForInstall(@PathVariable String installToken) {
        Optional<Agent> agentOpt = agentRepository.findByInstallToken(installToken);

        if (agentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Invalid or expired install token\"}");
        }

        Agent agent = agentOpt.get();

        // Check expiry
        if (agent.getInstallTokenExpiresAt() != null
                && agent.getInstallTokenExpiresAt().isBefore(Instant.now())) {
            // Token expired — clear it
            agent.setInstallToken(null);
            agent.setInstallTokenExpiresAt(null);
            agentRepository.save(agent);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Install token has expired\"}");
        }

        try {
            byte[] binaryBytes = binaryService.getBinaryBytes();
            String manifest = binaryService.getSignedManifest();

            // Build response: [binary bytes]\n[manifest]. Size must be exactly
            // binary + 1 (newline) + manifest — a larger array leaves a trailing
            // 0 byte that shifts the client's split and breaks Blake3 verification.
            byte[] manifestBytes = manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] response = new byte[binaryBytes.length + 1 + manifestBytes.length];
            System.arraycopy(binaryBytes, 0, response, 0, binaryBytes.length);
            response[binaryBytes.length] = '\n';
            System.arraycopy(manifestBytes, 0, response, binaryBytes.length + 1, manifestBytes.length);

            // Consume the token (one-time use)
            agent.setInstallToken(null);
            agent.setInstallTokenExpiresAt(null);
            agentRepository.save(agent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "agent");
            headers.set("X-Blake3-Manifest", manifest);

            log.info("Agent binary served for install token (agent: {})", agent.getId());
            return ResponseEntity.ok().headers(headers).body(response);

        } catch (Exception e) {
            log.error("Failed to serve agent binary for install: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Failed to serve agent binary\"}");
        }
    }
}
