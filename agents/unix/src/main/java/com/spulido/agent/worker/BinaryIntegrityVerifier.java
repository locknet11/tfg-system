package com.spulido.agent.worker;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.crypto.digests.Blake3Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spulido.agent.config.AgentConfig;

public class BinaryIntegrityVerifier {

    private static final Logger log = LoggerFactory.getLogger(BinaryIntegrityVerifier.class);

    private final PublicKey publicKey;
    private final ObjectMapper objectMapper;

    public BinaryIntegrityVerifier(AgentConfig config) {
        this.objectMapper = new ObjectMapper();
        this.publicKey = loadPublicKey(config.getCentralPublicKey());
    }

    private PublicKey loadPublicKey(String publicKeyPem) {
        try {
            if (publicKeyPem == null || publicKeyPem.isBlank()) {
                log.warn("CENTRAL_PUBLIC_KEY not configured — integrity verification disabled");
                return null;
            }
            String key = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            log.error("Failed to load central public key", e);
            return null;
        }
    }

    public boolean verify(byte[] binary) {
        if (publicKey == null) {
            log.warn("Public key not available — skipping integrity verification");
            return true;
        }

        // The response is [binary bytes][manifest JSON]. Locate the manifest by
        // scanning the RAW bytes — never via new String(binary): the agent binary
        // is arbitrary (non-UTF-8) bytes, and a String round-trip replaces invalid
        // sequences with U+FFFD, so the reconstructed binary would not match the
        // Blake3 hash Central computed over the true bytes.
        // Search from the END: the marker also occurs as a compiled-in string
        // constant inside the agent ELF itself, so a forward scan would split on
        // that copy. The real manifest is the trailing occurrence.
        byte[] marker = "{\"blake3Hash\"".getBytes(StandardCharsets.UTF_8);
        int manifestIdx = lastIndexOf(binary, marker);
        if (manifestIdx < 0) {
            log.error("No manifest found in binary response");
            return false;
        }

        byte[] actualBinary = new byte[manifestIdx];
        System.arraycopy(binary, 0, actualBinary, 0, manifestIdx);
        String manifestStr = new String(binary, manifestIdx, binary.length - manifestIdx,
                StandardCharsets.UTF_8).trim();

        try {
            JsonNode manifest = objectMapper.readTree(manifestStr);
            String expectedHash = manifest.get("blake3Hash").asText();
            String signature = manifest.get("signature").asText();
            String algorithm = manifest.has("algorithm") ? manifest.get("algorithm").asText() : "SHA256withRSA";

            String computedHash = computeBlake3Hash(actualBinary);

            if (!computedHash.equals(expectedHash)) {
                log.error("Hash mismatch: expected={}, computed={}", expectedHash, computedHash);
                return false;
            }

            Signature sig = Signature.getInstance(algorithm);
            sig.initVerify(publicKey);
            sig.update(expectedHash.getBytes(StandardCharsets.UTF_8));
            boolean valid = sig.verify(Base64.getDecoder().decode(signature));

            if (!valid) {
                log.error("Signature verification failed");
                return false;
            }

            log.info("Binary integrity verified: Blake3 hash matches and signature is valid");
            return true;

        } catch (Exception e) {
            log.error("Failed to verify binary integrity", e);
            return false;
        }
    }

    private static int lastIndexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = haystack.length - needle.length; i >= 0; i--) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private String computeBlake3Hash(byte[] binary) {
        Blake3Digest blake3 = new Blake3Digest();
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
    }
}
