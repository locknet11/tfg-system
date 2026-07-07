package com.spulido.tfg.domain.replication.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spulido.tfg.domain.replication.model.dto.AgentPlatformInfo;
import com.spulido.tfg.domain.replication.services.AgentBinaryService;
import com.spulido.tfg.domain.replication.services.BinaryIntegrityService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AgentBinaryServiceImpl implements AgentBinaryService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BinaryIntegrityService integrityService;
    private final String binaryPath;
    private final String resourcePath;

    private final Map<String, byte[]> platformBinaries = new LinkedHashMap<>();
    private final Map<String, String> platformHashes = new LinkedHashMap<>();
    private final Map<String, String> platformManifests = new LinkedHashMap<>();
    private final Map<String, LocalDateTime> platformLastBuilt = new LinkedHashMap<>();
    private final Map<String, Long> platformFileSizes = new LinkedHashMap<>();

    // Backward-compatible single-binary fields (first platform loaded)
    private byte[] binaryBytes;
    private String blake3Hash;
    private String signedManifest;

    public AgentBinaryServiceImpl(
            BinaryIntegrityService integrityService,
            @Value("${agent.binary.path:agents/unix/target/agent}") String binaryPath,
            @Value("${agent.binary.resource-path:agents}") String resourcePath) {
        this.integrityService = integrityService;
        this.binaryPath = binaryPath;
        this.resourcePath = resourcePath;
    }

    @PostConstruct
    public void loadBinary() {
        boolean loadedFromClasspath = loadFromClasspath();

        if (!loadedFromClasspath || platformBinaries.isEmpty()) {
            log.info("No classpath binaries found, trying filesystem path: {}", binaryPath);
            loadSingleBinaryFromFilesystem();
        }

        if (!platformBinaries.isEmpty()) {
            // Set backward-compatible single-binary fields from first platform
            String firstPlatform = platformBinaries.keySet().iterator().next();
            this.binaryBytes = platformBinaries.get(firstPlatform);
            this.blake3Hash = platformHashes.get(firstPlatform);
            this.signedManifest = platformManifests.get(firstPlatform);

            log.info("Agent binaries loaded: {} platforms available", platformBinaries.size());
            platformBinaries.forEach((platform, bytes) ->
                    log.info("  {} — {} bytes, Blake3: {}", platform, bytes.length, platformHashes.get(platform)));
        } else {
            log.warn("No agent binaries loaded — download will fail until deployed");
        }
    }

    private boolean loadFromClasspath() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] platformDirs = resolver.getResources("classpath:" + resourcePath + "/*/agent");

            if (platformDirs.length == 0) {
                log.info("No agent binaries found in classpath:{}/", resourcePath);
                return false;
            }

            for (Resource agentResource : platformDirs) {
                try {
                    String path = agentResource.getURL().getPath();
                    // Extract platform from path: .../agents/linux-x86_64/agent → linux-x86_64
                    String[] parts = path.split("/");
                    String platform = null;
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (resourcePath.equals(parts[i]) || parts[i].endsWith(resourcePath)) {
                            platform = parts[i + 1];
                            break;
                        }
                    }
                    if (platform == null) {
                        // Try matching the resourcePath last segment
                        String resourceLastSegment = resourcePath.contains("/")
                                ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                                : resourcePath;
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (resourceLastSegment.equals(parts[i])) {
                                platform = parts[i + 1];
                                break;
                            }
                        }
                    }
                    if (platform == null) {
                        log.warn("Could not determine platform from path: {}", path);
                        continue;
                    }

                    byte[] binary = readResourceBytes(agentResource);
                    if (binary == null || binary.length == 0) continue;

                    String hash = integrityService.computeBlake3Hash(binary);
                    String signature = integrityService.signHash(hash);
                    String manifest = OBJECT_MAPPER.writeValueAsString(
                            new AgentBinaryManifest(hash, signature, integrityService.getAlgorithm()));

                    platformBinaries.put(platform, binary);
                    platformHashes.put(platform, hash);
                    platformManifests.put(platform, manifest);
                    platformFileSizes.put(platform, (long) binary.length);
                    platformLastBuilt.put(platform, LocalDateTime.now());

                    log.info("Loaded agent binary from classpath for platform '{}' — {} bytes", platform, binary.length);
                } catch (Exception e) {
                    log.warn("Failed to load agent binary from resource: {}", agentResource, e);
                }
            }

            return !platformBinaries.isEmpty();
        } catch (Exception e) {
            log.warn("Failed to scan classpath for agent binaries: {}", e.getMessage());
            return false;
        }
    }

    private byte[] readResourceBytes(Resource resource) {
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToByteArray(is);
        } catch (IOException e) {
            log.warn("Failed to read resource: {}", resource, e);
            return null;
        }
    }

    private void loadSingleBinaryFromFilesystem() {
        try {
            Path path = Path.of(binaryPath);
            if (!Files.exists(path)) {
                log.warn("Agent binary not found at {}", binaryPath);
                return;
            }
            byte[] bytes = Files.readAllBytes(path);
            String hash = integrityService.computeBlake3Hash(bytes);
            String signature = integrityService.signHash(hash);
            String manifest = OBJECT_MAPPER.writeValueAsString(
                    new AgentBinaryManifest(hash, signature, integrityService.getAlgorithm()));

            String platform = "linux-x86_64"; // default for filesystem path
            platformBinaries.put(platform, bytes);
            platformHashes.put(platform, hash);
            platformManifests.put(platform, manifest);
            platformFileSizes.put(platform, (long) bytes.length);
            platformLastBuilt.put(platform, LocalDateTime.now());

            log.info("Agent binary loaded from filesystem for platform '{}' — {} bytes", platform, bytes.length);
        } catch (IOException e) {
            log.error("Failed to load agent binary from {}", binaryPath, e);
        }
    }

    @Override
    public List<AgentPlatformInfo> getAvailablePlatforms() {
        if (platformBinaries.isEmpty()) {
            return Collections.emptyList();
        }

        List<AgentPlatformInfo> platforms = new ArrayList<>();
        for (String platform : platformBinaries.keySet()) {
            platforms.add(AgentPlatformInfo.builder()
                    .platform(platform)
                    .label(platformToLabel(platform))
                    .agentVersion("0.0.1-SNAPSHOT")
                    .fileSizeBytes(platformFileSizes.getOrDefault(platform, 0L))
                    .blake3Hash(platformHashes.get(platform))
                    .lastBuilt(platformLastBuilt.get(platform))
                    .build());
        }
        return platforms;
    }

    @Override
    public byte[] getBinaryForPlatform(String platform) {
        byte[] bytes = platformBinaries.get(platform);
        if (bytes == null) {
            throw new IllegalArgumentException("Agent binary not available for platform: " + platform);
        }
        return bytes;
    }

    @Override
    public String getSignedManifestForPlatform(String platform) {
        String manifest = platformManifests.get(platform);
        if (manifest == null) {
            throw new IllegalArgumentException("Agent manifest not available for platform: " + platform);
        }
        return manifest;
    }

    private String platformToLabel(String platform) {
        return switch (platform) {
            case "linux-x86_64" -> "Linux (x86_64)";
            case "linux-aarch64" -> "Linux (ARM64)";
            case "macos-x86_64" -> "macOS (Intel)";
            case "macos-aarch64" -> "macOS (Apple Silicon)";
            default -> platform;
        };
    }

    private record AgentBinaryManifest(String blake3Hash, String signature, String algorithm) {}

    // Backward-compatible methods (used by existing replication endpoint)
    @Override
    public byte[] getBinaryBytes() {
        if (binaryBytes == null) {
            throw new IllegalStateException("Agent binary not available");
        }
        return binaryBytes;
    }

    @Override
    public String getBlake3Hash() {
        return blake3Hash;
    }

    @Override
    public String getSignedManifest() {
        return signedManifest;
    }
}
