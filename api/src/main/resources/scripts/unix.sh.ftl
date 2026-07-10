<#-- Agent Installation Script (FreeMarker template) -->
<#-- Generated for target: ${targetUniqueId} -->
<#-- This script downloads, verifies, configures, and launches the security agent -->

#!/bin/sh
#
# Agent Installation Script
# Generated for target: ${targetUniqueId}
#
# This script downloads the agent binary from the central platform,
# verifies its integrity (Blake3 hash + RSA signature),
# writes configuration, and launches the agent in background.
#

set -e

DOWNLOAD_URL="${downloadUrl}"
CENTRAL_URL="${apiUrl}"
PREAUTH_CODE="${preauthCode}"
API_KEY="${apiKey}"
AGENT_ID="${agentId}"
ORGANIZATION_ID="${organizationIdentifier}"
PROJECT_ID="${projectIdentifier}"
TARGET_ID="${targetUniqueId}"
CENTRAL_PUBLIC_KEY="${centralPublicKey}"

echo "=== Agent Installation ==="
echo "API URL: $CENTRAL_URL"
echo "Organization: $ORGANIZATION_ID"
echo "Project: $PROJECT_ID"
echo "Target: $TARGET_ID"

# --- Pre-flight checks ---
[ -d /tmp ] || mkdir -p /tmp

if [ ! -w /tmp ]; then
    echo "FATAL: /tmp is not writable — cannot install agent"
    exit 1
fi

# --- Download binary ---
echo "=== Downloading agent binary ==="

download_binary() {
    if command -v curl > /dev/null 2>&1; then
        curl -sS --connect-timeout 30 --max-time 300 -o /tmp/agent_raw "$DOWNLOAD_URL"
        return $?
    elif command -v wget > /dev/null 2>&1; then
        wget -q --timeout=30 --tries=1 -O /tmp/agent_raw "$DOWNLOAD_URL"
        return $?
    else
        echo "FATAL: Neither curl nor wget available — cannot download agent"
        exit 1
    fi
}

if ! download_binary; then
    echo "Retrying download..."
    sleep 2
    if ! download_binary; then
        echo "FATAL: Cannot reach central platform after 2 attempts"
        exit 1
    fi
fi

if [ ! -s /tmp/agent_raw ]; then
    echo "FATAL: Downloaded file is empty"
    exit 1
fi

echo "Agent binary downloaded: $(wc -c < /tmp/agent_raw) bytes"

# --- Extract manifest from binary ---
# Binary format: [binary bytes]\n{"blake3Hash":"...","signature":"...","algorithm":"..."}
MANIFEST=$(tail -1 /tmp/agent_raw)
BINARY_SIZE=$(($(wc -c < /tmp/agent_raw) - ${#MANIFEST} - 1))

# Extract just the binary part (everything before the last newline + manifest)
head -c $BINARY_SIZE /tmp/agent_raw > /tmp/agent

# --- Parse manifest ---
parse_manifest_field() {
    field_name="$1"
    # Try jq first, then python3, then grep fallback
    if command -v jq > /dev/null 2>&1; then
        echo "$MANIFEST" | jq -r ".${field_name} // empty"
    elif command -v python3 > /dev/null 2>&1; then
        python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(d.get('$field_name',''))" "$MANIFEST"
    else
        echo "$MANIFEST" | grep -o "\"${field_name}\":\"[^\"]*\"" | sed "s/\"${field_name}\":\"//;s/\"//"
    fi
}

EXPECTED_HASH=$(parse_manifest_field "blake3Hash")
SIGNATURE=$(parse_manifest_field "signature")
ALGORITHM=$(parse_manifest_field "algorithm")

if [ -z "$EXPECTED_HASH" ]; then
    echo "FATAL: Could not parse manifest — missing blake3Hash"
    exit 1
fi

# --- Verify Blake3 hash ---
echo "=== Verifying Blake3 hash ==="

compute_blake3() {
    if command -v openssl > /dev/null 2>&1 && openssl dgst -blake3 /tmp/agent 2>/dev/null | grep -q .; then
        openssl dgst -blake3 -hex /tmp/agent | awk '{print $NF}'
    elif command -v b3sum > /dev/null 2>&1; then
        b3sum /tmp/agent | awk '{print $1}'
    else
        echo ""
    fi
}

COMPUTED_HASH=$(compute_blake3)

if [ -n "$COMPUTED_HASH" ]; then
    if [ "$COMPUTED_HASH" = "$EXPECTED_HASH" ]; then
        echo "Blake3 hash: OK"
    else
        echo "FATAL: Blake3 hash MISMATCH"
        echo "  Expected: $EXPECTED_HASH"
        echo "  Computed: $COMPUTED_HASH"
        exit 1
    fi
else
    echo "WARNING: Cannot compute Blake3 hash — openssl 3.0+ or b3sum not available"
    echo "Skipping hash verification (not recommended for production)"
fi

# --- Verify RSA signature ---
echo "=== Verifying RSA signature ==="

if [ -n "$SIGNATURE" ] && [ -n "$CENTRAL_PUBLIC_KEY" ] && [ "$CENTRAL_PUBLIC_KEY" != " " ]; then
    if command -v openssl > /dev/null 2>&1; then
        # Write public key to temp file
        echo "$CENTRAL_PUBLIC_KEY" > /tmp/central_pubkey.pem

        # Write hash to temp file
        echo -n "$EXPECTED_HASH" > /tmp/hash_to_verify.bin

        # Decode signature from base64
        echo "$SIGNATURE" | base64 -d > /tmp/signature.bin 2>/dev/null || {
            echo "FATAL: Failed to decode signature"
            exit 1
        }

        # Verify signature (PKCS#1 v1.5 padding, SHA256withRSA)
        if openssl pkeyutl -verify -pubin -inkey /tmp/central_pubkey.pem \
                -in /tmp/hash_to_verify.bin -sigfile /tmp/signature.bin 2>/dev/null; then
            echo "RSA signature: VERIFIED"
        else
            echo "FATAL: RSA signature verification FAILED"
            exit 1
        fi

        # Cleanup temp verification files
        rm -f /tmp/central_pubkey.pem /tmp/hash_to_verify.bin /tmp/signature.bin
    else
        echo "WARNING: openssl not available — skipping RSA signature verification"
    fi
else
    echo "RSA signature: SKIPPED (no public key configured)"
fi

# --- Configure agent ---
echo "=== Installing agent ==="

chmod +x /tmp/agent

<#-- Write configuration properties — apiKey goes ONLY to the config file, never echoed -->
echo "agent.central-url=$CENTRAL_URL" > /tmp/agent.properties
echo "agent.api-key=$API_KEY" >> /tmp/agent.properties
echo "agent.agent-id=$AGENT_ID" >> /tmp/agent.properties
echo "agent.central-public-key=$CENTRAL_PUBLIC_KEY" >> /tmp/agent.properties
echo "agent.organization-identifier=$ORGANIZATION_ID" >> /tmp/agent.properties
echo "agent.project-identifier=$PROJECT_ID" >> /tmp/agent.properties
echo "agent.target-unique-id=$TARGET_ID" >> /tmp/agent.properties

# --- Launch agent in background (POSIX-compatible) ---
nohup /tmp/agent > /tmp/agent.log 2>&1 &
AGENT_PID=$!
echo $AGENT_PID > /tmp/agent.pid

# Check agent started successfully
sleep 2
if kill -0 $AGENT_PID 2>/dev/null; then
    echo "Agent started (PID: $AGENT_PID)"
else
    echo "WARNING: Agent may not have started — check /tmp/agent.log"
fi

# --- Cleanup ---
rm -f /tmp/agent_raw

echo "INSTALL_OK"
