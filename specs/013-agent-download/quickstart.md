# Quickstart: Agent Download Portal

**Feature**: 013-agent-download
**Last Updated**: 2026-07-07

## Prerequisites

- Java 17+ for API
- Maven for both `api/` and `agents/unix/`
- GraalVM 21+ with native-image for building the agent binary
- Node.js 18+ for UI
- Docker + Docker Compose for lab testing
- MongoDB running locally (or via Docker)

## Building the Agent Binary

### 1. Build the native image

```bash
cd agents/unix

# Linux (on Linux host with GraalVM)
./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication

# macOS (on macOS host with GraalVM)
sh package-macos.sh
```

The output binary is at `agents/unix/target/agent`.

### 2. Package binary into API classpath

```bash
# From repo root
mkdir -p api/src/main/resources/agents/linux-x86_64
mkdir -p api/src/main/resources/agents/macos-aarch64

# Copy the built binary
cp agents/unix/target/agent api/src/main/resources/agents/linux-x86_64/agent
# Or for macOS:
cp agents/unix/target/agent api/src/main/resources/agents/macos-aarch64/agent
```

### 3. Start the API

```bash
cd api
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. The agent binary is loaded from classpath at startup.

### 4. Start the UI

```bash
cd ui
npm ci
npm start
```

The UI starts on `http://localhost:4200`.

## Testing Downloads

### Option A: Browser (UI)

1. Log in to the dashboard at `http://localhost:4200`
2. Navigate to **Agents** page
3. Click **Download Agent** button
4. Select platform from dropdown
5. Click **Download** — browser saves the file

### Option B: curl (API direct)

```bash
# Login first to get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | jq -r '.token')

# List available platforms
curl -s http://localhost:8080/api/agent/download/platforms \
  -H "Authorization: Bearer $TOKEN" | jq

# Download agent binary for Linux
curl -o agent-downloaded http://localhost:8080/api/agent/download/linux-x86_64 \
  -H "Authorization: Bearer $TOKEN" -v

# Get manifest only
curl -s http://localhost:8080/api/agent/download/linux-x86_64/manifest \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Option C: Docker lab integration test

1. Deploy the lab:
   ```bash
   cd lab
   docker compose up -d
   ```

2. Build and start API + UI as above.

3. Download the agent binary via UI or curl.

4. Copy the downloaded binary to a lab target and test installation:
   ```bash
   # Copy to a target container
   docker cp agent-downloaded lab-tomcat-1:/tmp/agent

   # Extract manifest from binary (manifest is appended after \n at end of binary)
   # The manifest is the JSON after the last newline in the file

   # Run install script manually (simulating what install-agent-http.sh.tmpl does)
   docker exec lab-tomcat-1 sh -c '
     chmod +x /tmp/agent
     # Verify binary integrity using embedded manifest
     # ... (see install-agent-http.sh.tmpl for full verification steps)
   '
   ```

## Verifying Binary Integrity

The agent binary is signed (Blake3 hash + RSA signature). To verify manually:

```bash
# Extract the binary part (everything before the manifest JSON)
# The manifest is appended as: \n{"blake3Hash":"...","signature":"...","algorithm":"SHA256withRSA"}

# Compute Blake3 hash (requires openssl 3.0+)
BINARY_PART=$(head -c -$(tail -1 agent | wc -c) agent)  # rough extraction
HASH=$(openssl dgst -blake3 -hex "$BINARY_PART" | cut -d' ' -f2)

# Compare with manifest hash
MANIFEST_HASH=$(tail -1 agent | jq -r '.blake3Hash')
[ "$HASH" = "$MANIFEST_HASH" ] && echo "Hash OK" || echo "Hash MISMATCH"

# Verify RSA signature
SIGNATURE=$(tail -1 agent | jq -r '.signature')
echo -n "$HASH" > /tmp/hash.txt
echo "$SIGNATURE" | base64 -d > /tmp/sig.bin
openssl pkeyutl -verify -pubin -inkey /path/to/public.pem \
  -in /tmp/hash.txt -sigfile /tmp/sig.bin
```

## File Structure After Changes

```
api/src/main/resources/agents/
├── linux-x86_64/
│   └── agent           # GraalVM native image (~50 MB)
└── macos-aarch64/
    └── agent           # GraalVM native image (~48 MB)
```

The `agent` binary files are **not committed** to git (they are build artifacts). They are added to `.gitignore`. The `agents/` directory contains only binaries, no source code.
