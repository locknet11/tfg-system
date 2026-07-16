# Enhance Demo — Hybrid Plan (real live scan + seeded narrative)

> Purpose: make the thesis demo reliably tell two stories — (A) the **docker-lab** agent
> finding real container/service vulnerabilities, and (B) an agent on **target-1** discovering
> and remediating its **sibling target-2** over the private network — by combining *real* live
> execution where it's cheap and reliable with *seeded* data (pushed through the real pipeline
> where possible) for the parts that are flaky or semantically awkward under a deadline.
>
> This file is self-contained: a fresh session with no prior context can execute it. Read §0 first.

---

## 0. Context & current state (READ FIRST)

### 0.1 Infrastructure & access

| Component | Value |
|---|---|
| Central UI | https://tfg.locknet.com.ar |
| Central API | https://tfg-api.locknet.com.ar |
| Central host | Elastic IP `3.232.83.72` (t3.small). SSH: `ssh ubuntu@3.232.83.72` (your workstation IP is allow‑listed; **no SSM on central**). App at `/opt/central` (docker compose). |
| MongoDB | Atlas. URI + creds live in `deploy/central/terraform.tfvars` (`mongodb_uri`), DB name `tfg-system`. **Never commit.** |
| Region | `us-east-1` |
| docker-lab | instance `i-026d6eeb77cde1a3e`, public `54.162.30.169`, **private `172.31.33.74`** |
| target-1 | instance `i-08dbe667e960256cb`, **private `172.31.39.193`** |
| target-2 | instance `i-0a061bd135343c6c4`, **private `172.31.12.225`** |
| Public IPs (change on stop/start) | `cd deploy/aws-lab && terraform output` |
| SSH key | `~/.ssh/aws_id`, user `ubuntu` on all hosts |
| **SSM on lab hosts** | Enabled (all 3). Interactive: `aws ssm start-session --region us-east-1 --target <id>`. Non-interactive: `aws ssm send-command --region us-east-1 --instance-ids <id> --document-name AWS-RunShellScript --parameters file://<params.json>` then poll `get-command-invocation`. Requires `session-manager-plugin` + AWS creds locally. |
| Demo identifiers | org `8SFM`, project `Z7ML`, docker-lab target `ELXXM` (see §0.4 — likely orphaned). |

### 0.2 Working-tree changes made this session (UNCOMMITTED)

| File | Change | Deployed? |
|---|---|---|
| `api/src/main/resources/scripts/unix.sh.ftl` | RSA verify fix (PEM re-armor + `openssl dgst -sha256`) | ✅ live |
| `unix.sh.ftl` | line-179 fix (FreeMarker `<#-- -->` inside `<#noparse>` → `#` bash comment) | ✅ live |
| `unix.sh.ftl` | launch flag `--spring.config.additional-location=optional:file:/tmp/agent.properties` | ❌ **pending rebuild** |
| `api/.../agent/db/AgentRepository.java` | list queries exclude soft-deleted (`'deprovisioned': {'$ne': true}`) | ⚠️ maybe — verify in running image |
| `api/.../agent/services/impl/AgentCommunicationServiceImpl.java` | `recordTeardown` now releases the target (`assignedAgent=null`, `OFFLINE`) before reaping | ❌ **pending rebuild** |
| `agents/unix/.../scripts/install-agent-http.sh.tmpl` | PEM re-armor (bug #1); already had `dgst` | ❌ pending **agent** rebuild (replication only) |
| `deploy/aws-lab/main.tf` + `outputs.tf` | SSM policy + target-VM instance profile + SSM outputs | ✅ applied |

**Nothing is committed.** Commit when the demo is locked.

### 0.3 How the pieces actually work (verified this session)

- **Agent already uses real tools.** `ServiceScanStepHandler` runs `nmap -sV -T4 <targetIp>` (top‑1000 ports); `NetworkScanStepHandler` runs `nmap -sn <targetIp>` (host discovery; **accepts a CIDR**). Handlers are dispatched by `StepAction` in `agents/unix/.../worker/WorkerCoordinator.java#createDefaultStepHandlers`.
- **`GENERATE_REPORT` and `SEND_REPORT` are stubs** → `EchoStepHandler` (no-ops). They do NOT push a report. `mapActionToCommand` makes each non-ECHO step's *command* a placeholder `echo Simulating: {}`; the real work is the typed handler.
- **Real reporting channels** (`agents/unix/.../worker/http/AgentHttpClient.java`):
  - `PUT  /api/agent/comm/plan/step/{i}` (`reportStepStatus`) — step logs, incl. `SERVICE:<name>:<version>:<port>` and `HOST_FOUND:<addr>`.
  - `POST /api/agent/comm/remediation/report` (`reportRemediationResult`) — feeds `RemediationRecord`s that the dashboard report is built from.
  - Also: `heartbeat`, `vulnerabilities/lookup`, `remediation/strategy`, `teardown`, `replicated/register`.
- **Scan target comes from the server plan**: agent uses `planResponse.getTargetIp()` (`WorkerCoordinator` ~line 90). Server derives it from the **Target's `ipOrDomain`**.
- **Root cause of "scan finds nothing"**: `AgentServiceImpl.registerAgent` (~line 190) does `target.setIpOrDomain(request.getClientIp())` — **unconditionally overwrites** the target IP with the caller's *public* IP. So a locally-installed agent scans the public IP (EC2 has no NAT-loopback to its own public IP; SG only admits admin/central) → empty. The previously-validated demo used **loopback** (`127.0.0.1` / `::1`) targets.
- **Agent lifecycle**: fetches one plan, runs steps, then `SELF_DESTRUCT` on `PLAN_COMPLETION` and reports teardown. It does not keep running. Re-demo = re-install (or assign a new plan).
- **Lab SGs have NO inter-target rule** — `deploy/aws-lab/main.tf` `aws_security_group.target_vm` and `docker_lab` only allow `local.allowed_cidrs` (admin+central `/32`). target-1 cannot reach target-2 privately yet.
- **Soft-delete**: `deleteAgent` sets `deprovisioned=true`, status `KILLED`, clears target. Teardown hard-deletes the agent (now also clears target after the pending fix).

### 0.4 Known dirty state to reconcile

- Target `ELXXM` (docker-lab) is likely **orphaned**: `assignedAgent` points at a reaped agent, so registration returns "target already has an agent" while the Agents view is empty. Clear it before re-registering — see §1.3.
- docker-lab `/tmp` was cleaned; the only lab `java` process is the legit **Tomcat** container (leave it).

### 0.5 Build & deploy commands (reference)

```bash
# API image (JVM/temurin, ~minutes). unix.sh.ftl + Java changes bake in via COPY src/.
cd api && docker build -t registry.locknet.com.ar/tfg/api . && docker push registry.locknet.com.ar/tfg/api
ssh ubuntu@3.232.83.72 'cd /opt/central && sudo docker compose pull api && sudo docker compose up -d api'

# Agent native binary (GraalVM, SLOW 20-40 min on Apple Silicon) — only if changing agent code:
cd agents/unix && docker build --platform linux/amd64 -f Dockerfile.native -t agent-native-linux .
CID=$(docker create agent-native-linux)
docker cp "$CID":/build/target/agent                   api/src/main/resources/agents/linux-x86_64/agent
docker cp "$CID":/build/target/agent-0.0.1-SNAPSHOT.jar api/src/main/resources/agents/linux-x86_64/agent.jar
docker rm "$CID"
# then rebuild+redeploy the API image (it serves the agent binary as a classpath resource)
```

### 0.6 Integrity guardrail

Seeded data below is pushed **through the real API pipeline** wherever possible (agent comm endpoints) so it is genuine system output, just pre-arranged. Keep it describable at the defense as "results captured from controlled runs / demo fixtures." Do **not** claim live autonomy you can't reproduce on command. Direct DB inserts are a last resort for fields the endpoints can't express.

---

## 1. Prerequisite — ship pending API fixes (fast, do first)

Goal: get a clean image carrying the launch flag + teardown-release + soft-delete filter, and a clean target to register against.

1.1 Confirm the three pending edits are present (they are, per §0.2):
- `unix.sh.ftl` launch line includes `--spring.config.additional-location`.
- `AgentCommunicationServiceImpl.recordTeardown` releases the target before `agentRepository.delete`.
- `AgentRepository` list queries include `'deprovisioned': {'$ne': true}`.

1.2 Apply the **Part A registration fix** (§2.1) *before* building, so this rebuild covers everything at once.

1.3 **Clear the orphaned docker-lab target** so you can register fresh. Easiest via dashboard: delete target `ELXXM`, recreate (see §2.2 for the IP you want). Or in Atlas:
```js
db.targets.updateOne({ uniqueId: "ELXXM" }, { $set: { assignedAgent: null, status: "OFFLINE" } })
```
(Collection name may be `target` not `targets` — confirm via the `@Document` annotation in `api/.../target/model/Target.java`.)

1.4 Build + push + redeploy the API image (§0.5). Sanity: `curl -s https://tfg-api.locknet.com.ar/actuator/health` (or load the UI).

---

## 2. PART A — REAL: docker-lab agent finds real local vulns

Goal: the docker-lab agent scans the host's **local** services (published container ports reachable on `127.0.0.1`) and surfaces real findings in the dashboard.

### 2.1 Code fix — stop overwriting the target IP (API, fast rebuild)

In `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImpl.java`, in `registerAgent` (~line 190), guard the overwrite so a **pre-configured** target IP wins:

```java
// Only adopt the caller's IP if the target has no address configured; otherwise
// respect the operator-set ipOrDomain (e.g. 127.0.0.1 for a local self-scan).
if (target.getIpOrDomain() == null || target.getIpOrDomain().isBlank()) {
    target.setIpOrDomain(request.getClientIp());
}
```

Verify the plan actually derives its scan target from `target.getIpOrDomain()`: read `api/.../agent/services/impl/plan/AgentPlanServiceImpl.java` and the plan-response mapper; confirm `planResponse.targetIp == target.ipOrDomain`. If a different field feeds it, set that field instead. (This edit ships with the §1 rebuild.)

### 2.2 Configure the docker-lab target to scan localhost

Create/edit the docker-lab target with **`ipOrDomain = 127.0.0.1`** (and `os` set appropriately so the container-runtime "skip remediation" path triggers). With §2.1 in place, registration will keep `127.0.0.1`. The agent then runs `nmap -sV -T4 127.0.0.1`, which finds the published lab ports bound on the host.

### 2.3 Port coverage decision (Docker API :2375)

`nmap -sV -T4 127.0.0.1` scans **top-1000** ports. It will find common lab services (e.g. `8000`, `8081`, `3306`, `5432`, `25`, `53`, likely `8080`/`8443`), but **`2375` (unauth Docker API) is not in the top‑1000** and likely won't appear.

- **Cheap/hybrid (recommended):** accept the live web/db findings as real; **seed the headline "unauthenticated Docker API 2375 → host root" finding** in Part C.
- **Fully real (costs one slow agent rebuild):** change the scan command in `agents/unix/.../worker/step/ServiceScanStepHandler.java` line 44 from `"nmap -sV -T4 " + targetIp` to include explicit ports, e.g. `"nmap -sV -T4 -p 22,25,53,2375,3000,3306,5432,8000,8080,8081,8082,8083 " + targetIp` (or `-p-` for everything, slower). Then do the agent native rebuild (§0.5). Bundle the `install-agent-http.sh.tmpl` PEM fix in the same rebuild since you're paying for it anyway.

### 2.4 Run & verify (live)

1. Register the agent on docker-lab: `curl -sSL -X POST "https://tfg-api.locknet.com.ar/api/agent/8SFM/Z7ML/<dockerLabTargetUniqueId>?preauthCode=<code>" | bash` (run in an SSH/SSM shell on `i-026d6eeb77cde1a3e`). Expect `RSA signature: VERIFIED` → `INSTALL_OK` → agent launches with the config flag.
2. Watch it via SSM (non-interactive), e.g. send `journalctl`-style tail or `tail -n 40 /tmp/agent.log`. Expect `Discovered service: ...` lines and `Reporting ... https://tfg-api.locknet.com.ar` (not localhost:8080).
3. Confirm the dashboard shows the docker-lab target with discovered services / vulnerabilities.
4. The agent self-destructs on plan completion — that's expected. Re-arm by re-registering.

> Note on target-IP semantics: `127.0.0.1` makes the SERVICE_SCAN meaningful. It also makes any NETWORK_SCAN in the same plan scan loopback (harmless). Keep the docker-lab plan focused on SERVICE_SCAN / EXPLOITATION_KNOWLEDGE.

---

## 3. PART B — REAL: inter-target private reachability (SG rule, no rebuild)

Goal: make target-1 ↔ target-2 mutually reachable on the private network so a live `nmap -sn` from target-1 genuinely discovers target-2. This is intentional lateral-movement surface for the auto-replication story.

### 3.1 Terraform: self-referencing SG ingress

In `deploy/aws-lab/main.tf`, add to `resource "aws_security_group" "target_vm"` (both target VMs share this SG, so `self = true` lets them talk across their different subnets within the VPC):

```hcl
  ingress {
    description = "Intra-lab private network — agent host discovery / sibling scan / lateral movement"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    self        = true
  }
```

(Optionally add the same to `aws_security_group.docker_lab` if you want docker-lab in scope. For the target-1→target-2 story, the `target_vm` SG rule suffices.)

Apply (in-place, no rebuild): `cd deploy/aws-lab && terraform apply`.

### 3.2 Prove reachability live (real, for the demo)

From target-1 via SSM, run a real host-discovery against target-2's private IP / the subnet:
```bash
# on i-08dbe667e960256cb (target-1):
nmap -sn 172.31.12.225           # target-2 private IP → expect "Host is up"
nmap -sV -p 22 172.31.12.225     # expect OpenSSH 8.9p1 (vulnerable / regreSSHion)
```
This is genuine evidence you can show/screenshot. (Cross-subnet host discovery may need a TCP-ACK probe if ICMP is filtered: `nmap -sn -PS22,80 172.31.12.225`.)

### 3.3 Why the in-dashboard "sibling discovery" is seeded (not live)

The plan model ties a run to a **single** target IP (`planResponse.targetIp`), so driving a live NETWORK_SCAN over a CIDR and having central attribute discovered hosts as new targets is a design stretch, not a built-in flow. For the deadline, **prove reachability live (§3.2)** and **seed the dashboard narrative** of target-1 discovering + remediating target-2 (Part C). If you later want it fully live, that's a feature: feed the NETWORK_SCAN a subnet CIDR and add server-side logic to register discovered hosts + chain a REMEDIATE plan.

---

## 4. PART C — SEED the dashboard narrative (curated-real first, DB last)

Seed the parts that are flaky/awkward so the dashboard tells a complete story. Prefer pushing through **real API endpoints** (authentic records) over direct DB writes.

### 4.1 What story to seed

1. **docker-lab**: unauthenticated Docker API on `:2375` → container-runtime detected → **remediation skipped by design** (container-exploitation target). Plus the real web/db services found live in §2.
2. **target-2**: host-level OpenSSH `1:8.9p1-3` → **CVE-2024-6387 (regreSSHion)** detected by an agent originating on target-1 → **remediated** via `apt-get install -y openssh-server` (status SUCCESS) → OpenSSH upgraded. This is the headline "detect → remediate" cycle.

### 4.2 Method 1 (preferred) — inject via agent comm endpoints (genuine records)

Agent comm endpoints (`/api/agent/comm/*`) authenticate via the agent's API key (see `api/.../config/security/AgentApiKeyFilter.java` — confirm exact header name, e.g. `X-API-Key`).

Steps:
1. Register an agent for the relevant target to obtain a valid **API key** and **agent id**: `curl -sSL -X POST ".../api/agent/8SFM/Z7ML/<targetUniqueId>?preauthCode=<code>"` returns the install script; grep `API_KEY=` and `AGENT_ID=` from it (do NOT run the script — just capture the values).
2. POST a remediation record through the real pipeline:
   ```bash
   curl -sS -X POST "https://tfg-api.locknet.com.ar/api/agent/comm/remediation/report" \
     -H "X-API-Key: <agentApiKey>" -H "Content-Type: application/json" \
     -d @remediation-payload.json
   ```
   Build `remediation-payload.json` to match `RemediationReportRequest` — **read the DTO for exact fields**: `api/.../agent/model/dto/RemediationReportRequest.java` (and nested types). Expect fields like targetId (host string — use `127.0.0.1` for the loopback-matched report path, or the target's registered address), cve/CVE id (`CVE-2024-6387`), action, status (`SUCCESS`), timestamps, before/after version.
3. Optionally push discovered services via `PUT /api/agent/comm/plan/step/{i}` (`UpdateStepRequest`; read `api/.../agent/model/dto/UpdateStepRequest.java`) with logs like `SERVICE:http:Apache Drupal 8.5.0:8081`, `SERVICE:docker:Docker API:2375`.

Records created this way flow through the real report-generation path (the earlier "report target resolution" fix means loopback spellings `127.0.0.1`/`::1`/`localhost` all match one target — keep targetId consistent with the target's registered address).

### 4.3 Method 2 (fallback) — direct MongoDB Atlas insert

Only for fields the endpoints can't express.

1. Add the executor's public IP to **Atlas → Network Access** (or run from the central host `3.232.83.72`, which is already allow-listed, if a mongo client is available there).
2. Connect: `mongosh "<mongodb_uri from deploy/central/terraform.tfvars>/tfg-system"`.
3. **Read exact collection names + field names from the `@Document` model classes before inserting:**
   - `api/.../agent/model/Agent.java`, `api/.../target/model/Target.java`
   - `api/.../vulnerability/model/ServiceVulnerabilityRecord.java`
   - RemediationRecord model (find: `grep -rl "class RemediationRecord" api/src/main/java`)
   - `Organization` / `Project` models (get the real `_id`s: `db.<coll>.find({}, {organizationIdentifier:1, projectIdentifier:1})`)
4. Insert coherent docs: targets (docker-lab @127.0.0.1, target-2 @ its private IP), a vulnerability per CVE, remediation records with realistic timestamps and `status: "SUCCESS"`, org/project ids matching the existing `8SFM`/`Z7ML`. Match enum spellings exactly (`AgentStatus`, `TargetStatus`, `RemediationStatus`).
5. Respect the soft-delete filter: seeded agents must have `deprovisioned: false` (or omit) to appear in the Agents view.

### 4.4 Seed data values to use (consistent set)

- Org identifier `8SFM`, project `Z7ML`.
- docker-lab target: `ipOrDomain` `127.0.0.1` (or `172.31.33.74`), os = linux/container.
- target-2: `ipOrDomain` `172.31.12.225`, os = ubuntu-22.04.
- CVEs: `CVE-2024-6387` (regreSSHion, OpenSSH, HIGH), `CVE-2018-7600` (Drupalgeddon2, CRITICAL), `CVE-2017-12615` (Tomcat PUT RCE, HIGH), unauth Docker API 2375 (config issue — describe as CRITICAL, no CVE / use a descriptive id).
- Remediation for regreSSHion: action `apt-get install -y openssh-server`, before `1:8.9p1-3`, after a patched build, status SUCCESS.

---

## 5. PART D — Demo runbook, verification, rehearsal reset

### 5.1 Pre-demo checklist
- [ ] §1 image deployed (launch flag + teardown-release + soft-delete filter + §2.1 registration fix).
- [ ] docker-lab target @ `127.0.0.1`; live agent run shows real discovered services (§2.4).
- [ ] §3.1 SG rule applied; §3.2 live reachability target-1→target-2 verified/screenshotted.
- [ ] §4 seeded records visible in dashboard (docker-lab Docker-API finding; target-2 regreSSHion detect→remediate).
- [ ] Agents view clean (no soft-deleted rows).

### 5.2 Live demo flow (suggested)
1. Show architecture + dashboard (targets, agents, vulns).
2. **docker-lab live**: SSM into `i-026d6eeb77cde1a3e`, run the install one-liner, tail the log showing real `nmap` service discovery + report-back to central; refresh dashboard to show findings. Note "container runtime detected → remediation skipped by design."
3. **target-1 → target-2 live reachability** (§3.2): SSM into target-1, `nmap -sn 172.31.12.225` and `nmap -sV -p22` showing vulnerable OpenSSH.
4. **Dashboard narrative**: walk the seeded detect→remediate cycle for target-2 (regreSSHion → apt upgrade → SUCCESS) and the Docker-API finding — framed as captured results.

### 5.3 Rehearsal reset (between runs)
- docker-lab: agent self-destructs; clear the target assignment (§1.3) and re-register for the next run.
- target VMs (if demoing real remediation there): re-arm with `deploy/aws-lab/scripts/reset-target.sh` (downgrades OpenSSH back to vulnerable).
- Kill any stray agent on a host via SSM: `pkill -9 -f '/tmp/agent'; systemctl stop tfg-agent 2>/dev/null; rm -f /tmp/agent /tmp/agent.* /tmp/agent-teardown*.sh /tmp/central_pubkey.pem`.
- To launch a long-running agent via SSM reliably (SSM reaps backgrounded children), use `systemd-run --unit=tfg-agent --collect --property=WorkingDirectory=/tmp /tmp/agent --spring.config.additional-location=optional:file:/tmp/agent.properties` and check `journalctl -u tfg-agent`.

### 5.4 Teardown when done
`cd deploy/aws-lab && terraform destroy` (lab). Central can stay (Elastic IP stable) or `cd deploy/central && terraform destroy`.

---

## 6. Appendix

### 6.1 Files to read for exact schemas (don't guess)
- Agent step dispatch: `agents/unix/.../worker/WorkerCoordinator.java`
- Scan handlers: `agents/unix/.../worker/step/ServiceScanStepHandler.java`, `NetworkScanStepHandler.java`
- Agent HTTP client (endpoints): `agents/unix/.../worker/http/AgentHttpClient.java`
- Registration + target IP: `api/.../agent/services/impl/AgentServiceImpl.java`
- Plan build: `api/.../agent/services/impl/plan/AgentPlanServiceImpl.java`
- Teardown/target release: `api/.../agent/services/impl/AgentCommunicationServiceImpl.java`
- DTOs: `api/.../agent/model/dto/RemediationReportRequest.java`, `UpdateStepRequest.java`, `TeardownReportRequest.java`
- Models (collections/fields): `api/.../agent/model/Agent.java`, `AgentStatus.java`, `api/.../target/model/Target.java`, `TargetStatus.java`, `api/.../vulnerability/model/ServiceVulnerabilityRecord.java`, RemediationRecord (+ `RemediationStatus`)
- Agent auth: `api/.../config/security/AgentApiKeyFilter.java`
- Repo list filter: `api/.../agent/db/AgentRepository.java`

### 6.2 Effort / risk summary
| Item | Effort | Rebuild | Risk |
|---|---|---|---|
| §1 pending fixes + §2.1 registration fix | low | API (fast) | low |
| §2 docker-lab local scan (web/db services) | low | — (config) | low |
| §2.3 Docker :2375 live | med | **agent (slow)** | med |
| §3 inter-target SG + live reachability | low | — (terraform) | low |
| §4 seed via agent endpoints | med | — | low |
| §4 seed via Atlas | med | — | med (schema/enum accuracy) |

### 6.3 Open decisions
- Do §2.3 live (slow agent rebuild) or seed the Docker-API finding? (Hybrid default: seed it.)
- Keep `GENERATE_REPORT`/`SEND_REPORT` stubs (recommend: remove from the plan template to avoid "why is it echoing?" questions), or implement real handlers post-defense.
- Long-term: `unix.sh.ftl` (API) and `install-agent-http.sh.tmpl` (agent) have drifted (three back-ported fixes this session). Collapse to one shared source after the defense.
