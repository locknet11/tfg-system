package com.spulido.tfg.domain.replication.services.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private byte[] binaryBytes;
    private String blake3Hash;
    private String signedManifest;

    public AgentBinaryServiceImpl(
            BinaryIntegrityService integrityService,
            @Value("${agent.binary.path:agents/unix/target/agent}") String binaryPath) {
        this.integrityService = integrityService;
        this.binaryPath = binaryPath;
    }

    @PostConstruct
    public void loadBinary() {
        try {
            Path path = Path.of(binaryPath);
            if (!Files.exists(path)) {
                log.warn("Agent binary not found at {} — binary download will fail until deployed", binaryPath);
                return;
            }
            binaryBytes = Files.readAllBytes(path);
            blake3Hash = integrityService.computeBlake3Hash(binaryBytes);
            String signature = integrityService.signHash(blake3Hash);
            signedManifest = OBJECT_MAPPER.writeValueAsString(
                    new AgentBinaryManifest(blake3Hash, signature, integrityService.getAlgorithm()));
            log.info("Agent binary loaded and signed — {} bytes, Blake3 hash: {}", binaryBytes.length, blake3Hash);
        } catch (IOException e) {
            log.error("Failed to load agent binary from {}", binaryPath, e);
        }
    }

    private record AgentBinaryManifest(String blake3Hash, String signature, String algorithm) {}

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
