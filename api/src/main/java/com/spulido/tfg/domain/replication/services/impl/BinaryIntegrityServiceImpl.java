package com.spulido.tfg.domain.replication.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.replication.exception.ReplicationException;
import com.spulido.tfg.domain.replication.services.BinaryIntegrityService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BinaryIntegrityServiceImpl implements BinaryIntegrityService {

    private final PrivateKey privateKey;
    private final String algorithm;

    public BinaryIntegrityServiceImpl(
            @Value("${replication.private-key:}") String privateKeyPem) {
        this.algorithm = "SHA256withRSA";
        this.privateKey = loadPrivateKey(privateKeyPem);
    }

    private PrivateKey loadPrivateKey(String privateKeyPem) {
        try {
            if (privateKeyPem == null || privateKeyPem.isBlank()) {
                throw new IllegalStateException("REPLICATION_PRIVATE_KEY is not configured");
            }
            String key = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            log.error("Failed to load replication private key", e);
            throw new RuntimeException("Failed to load replication private key", e);
        }
    }

    @Override
    public String computeBlake3Hash(byte[] binary) {
        try {
            org.bouncycastle.crypto.digests.Blake3Digest blake3 = new org.bouncycastle.crypto.digests.Blake3Digest();
            blake3.update(binary, 0, binary.length);
            byte[] hash = new byte[32];
            blake3.doFinal(hash, 0);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to compute Blake3 hash", e);
            throw new ReplicationException(ErrorCode.BINARY_INTEGRITY_CHECK_FAILED, "Failed to compute Blake3 hash: " + e.getMessage());
        }
    }

    @Override
    public String signHash(String hash) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(hash.getBytes(StandardCharsets.UTF_8));
            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            log.error("Failed to sign hash", e);
            throw new ReplicationException(ErrorCode.BINARY_INTEGRITY_CHECK_FAILED, "Failed to sign hash: " + e.getMessage());
        }
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }
}
