package com.spulido.tfg.domain.replication.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.replication.db.AgentDownloadRecordRepository;
import com.spulido.tfg.domain.replication.model.AgentDownloadRecord;
import com.spulido.tfg.domain.replication.model.dto.AgentDownloadInfo;
import com.spulido.tfg.domain.replication.model.dto.AgentPlatformInfo;
import com.spulido.tfg.domain.replication.services.AgentBinaryService;
import com.spulido.tfg.domain.replication.services.AgentDownloadService;
import com.spulido.tfg.domain.user.db.UserRepository;
import com.spulido.tfg.domain.user.model.User;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDownloadServiceImpl implements AgentDownloadService {

    private final AgentBinaryService binaryService;
    private final AgentDownloadRecordRepository downloadRecordRepository;
    private final UserRepository userRepository;

    @Override
    public List<AgentPlatformInfo> listPlatforms() {
        return binaryService.getAvailablePlatforms();
    }

    @Override
    public byte[] getBinaryForPlatform(String platform, HttpServletRequest request) {
        byte[] binary = binaryService.getBinaryForPlatform(platform);
        String manifest = binaryService.getSignedManifestForPlatform(platform);

        // Append manifest to binary (same format as replication endpoint)
        byte[] manifestBytes = manifest.getBytes();
        byte[] response = new byte[binary.length + manifestBytes.length + 1];
        System.arraycopy(binary, 0, response, 0, binary.length);
        response[binary.length] = '\n';
        System.arraycopy(manifestBytes, 0, response, binary.length + 1, manifestBytes.length);

        // Create audit record
        try {
            AgentDownloadRecord record = new AgentDownloadRecord();
            record.setPlatform(platform);
            record.setAgentVersion("0.0.1-SNAPSHOT");
            record.setFileSizeBytes(binary.length);
            record.setBlake3Hash(binaryService.getSignedManifestForPlatform(platform));
            record.setDownloadedAt(LocalDateTime.now());
            record.setClientIp(getClientIp(request));
            record.setUserAgent(request.getHeader("User-Agent"));

            // Get current user info from security context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                record.setUserEmail(email);
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    record.setUserId(user.getId());
                }
            }

            // Get organization from project context
            String orgId = ProjectContext.getOrganizationId();
            String projectId = ProjectContext.getProjectId();
            record.setOrganizationId(orgId);
            record.setProjectId(projectId);

            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            downloadRecordRepository.save(record);
            log.info("Agent download recorded: platform={}, user={}, org={}", platform, record.getUserEmail(), orgId);
        } catch (Exception e) {
            log.warn("Failed to record agent download: {}", e.getMessage());
        }

        return response;
    }

    @Override
    public String getManifestForPlatform(String platform) {
        return binaryService.getSignedManifestForPlatform(platform);
    }

    @Override
    public Page<AgentDownloadRecord> listDownloadRecords(Pageable pageable, String platform, String userId) {
        if (platform != null && !platform.isBlank()) {
            return downloadRecordRepository.findAllScopedByPlatform(platform, pageable);
        }
        if (userId != null && !userId.isBlank()) {
            return downloadRecordRepository.findAllScopedByUserId(userId, pageable);
        }
        return downloadRecordRepository.findAllScoped(pageable);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
