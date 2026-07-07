package com.spulido.tfg.domain.replication.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.replication.model.AgentDownloadRecord;
import com.spulido.tfg.domain.replication.model.dto.AgentPlatformInfo;
import com.spulido.tfg.domain.replication.services.AgentDownloadService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AgentDownloadController {

    private final AgentDownloadService downloadService;

    @GetMapping("/api/agent/download/platforms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgentPlatformInfo>> listPlatforms() {
        List<AgentPlatformInfo> platforms = downloadService.listPlatforms();
        if (platforms.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(platforms);
    }

    @GetMapping("/api/agent/download/{platform}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadBinary(
            @PathVariable String platform,
            HttpServletRequest request) {
        byte[] response = downloadService.getBinaryForPlatform(platform, request);

        String manifest = downloadService.getManifestForPlatform(platform);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "agent");
        headers.set("X-Blake3-Manifest", manifest);
        headers.set("X-Agent-Version", "0.0.1-SNAPSHOT");

        return ResponseEntity.ok().headers(headers).body(response);
    }

    @GetMapping("/api/agent/download/{platform}/manifest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getManifest(@PathVariable String platform) {
        String manifest = downloadService.getManifestForPlatform(platform);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(manifest);
    }

    @GetMapping("/api/agent/download/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AgentDownloadRecord>> listRecords(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "platform", required = false) String platform,
            @RequestParam(name = "userId", required = false) String userId) {
        Page<AgentDownloadRecord> records = downloadService.listDownloadRecords(
                PageRequest.of(page, size), platform, userId);
        return ResponseEntity.ok(records);
    }
}
