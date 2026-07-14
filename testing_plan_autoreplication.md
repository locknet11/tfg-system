# E2E Testing Plan — Agent Auto-Replication (feature `004-agent-autoreplication`)

**Purpose:** validate the full self-replication chain on the **lab-server (SSH)**,
parent agent → exploit → binary transfer → new agent registers with Central, using
Docker machines as targets.

**Owner:** run by me (Claude) driving the lab-server over SSH; steps are scripted
so they can be re-run and diffed.

---

## 0. Load-bearing facts (read first — these decide what "pass" means)

1. **Step chain the parent executes** (from the code, not the PRD):
   `SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE → REQUEST_REPLICATION →
   EXECUTE_EXPLOIT → TRANSFER_AGENT`.
   - `RequestReplicationStepHandler` builds the request from the **first**
     `SERVICE_SCAN` service + **first** `EXPLOITATION_KNOWLEDGE` script
     (`exploitId|cveId|severity` pipe-split), and reads `targetIp:`/`targetPort:`
     out of the SERVICE_SCAN logs. If either upstream step is empty it skips or fails.
   - On `APPROVED` it emits `replicationToken / downloadUrl / preauthCode / centralUrl`
     in its logs; `PENDING` → polls (10s→300s backoff, 180 attempts).

2. **The replication *push* is over SSH/SCP, not the app exploit.**
   `EXECUTE_EXPLOIT` runs the exploit script, then `SshSessionProvisioner.verify()`
   does `ssh -o StrictHostKeyChecking=no [-i identityFile] user@targetIp 'echo OK'`.
   `TransferAgentStepHandler` only proceeds if the exploit logs carry
   `reverseShellActive:true` + a resolvable `targetIp:` + `targetUser:`.
   **Consequence:** the target must be reachable by SSH with a key/user the agent
   holds. Stock vulhub containers (drupal/tomcat/flask/thinkphp) **do not run sshd**,
   so replication *into* them fails at the SSH-verify gate. This is the single most
   important setup item — see §2.

3. **Two transfer paths** in `TransferAgentStepHandler`, chosen by probing the target:
   - **Path A (HTTP_DOWNLOAD)**: target has `curl`/`wget` AND can reach Central →
     pushes `install-agent-http.sh.tmpl`, target pulls the binary itself.
   - **Path B (AGENT_PUSH)**: parent downloads the binary, verifies Blake3/PKI
     integrity locally, SCPs it to `/tmp/agent`, pushes `install-agent-transfer.sh.tmpl`,
     launches it. Path A falls back to Path B on failure.
   - Both finish with a health check: `curl -s http://localhost:1222/actuator/health`
     on the target expecting `UP` (3 attempts, else `PARTIAL_SUCCESS` — still SUCCESS).

4. **Central side (API):**
   - Project `replicationPolicy` = `AUTO_APPROVE` | `MANUAL_APPROVE` (+ optional min
     severity). AUTO with met threshold → immediate APPROVED.
   - Binary endpoint is token-authenticated, **5-min TTL**, serves binary + signed
     Blake3 manifest. Expired token → must re-request.
   - Duplicate (same targetIp + exploit) → `DUPLICATE`. Target already has an active
     agent → skip. Target outside authorized range → deny.
   - New agent registers via the existing registration endpoint with the preauth
     code; Central records replication metadata (parentAgentId, timestamp, exploit),
     auto-assigns an initial plan, and notifies admin.

5. **Binary served must be the linux-x86_64 native build** (the docker targets are
   linux/amd64). Confirm `api/src/main/resources/agents/linux-x86_64/agent` is the
   freshly-built ELF (per CLAUDE.md Docker build), not a stale/macOS artifact.

---

## 1. Environment & preconditions

**Lab-server (over SSH):** Docker + Docker Compose, the repo checked out, outbound
internet (vulhub pulls), and the ports in `lab/docker-compose.yml` free.

**Central (API):** reachable from the lab-server at a stable URL the agents can hit
(`CENTRAL_URL`). Decide where Central runs — see §Decision below. Needs:
- MongoDB up, `tfg-system` DB.
- `REPLICATION_PRIVATE_KEY` (PEM) set; matching `CENTRAL_PUBLIC_KEY` provisioned to
  agents. Generate once:
  ```bash
  openssl genpkey -algorithm Ed25519 -out private.pem
  openssl pkey -in private.pem -pubout -out public.pem
  ```
- `AGENT_BINARY_PATH` pointing at the linux-x86_64 ELF (or the classpath resource).

**Parent agent:** installed on the lab-server host (or a dedicated "attacker"
container) with `CENTRAL_URL`, `CENTRAL_PUBLIC_KEY`, and after registration
`AGENT_ID`/`AGENT_API_KEY`.

**Decision to confirm before starting** (I'll ask if unset):
- Where Central runs relative to the lab-server (same host `localhost`, or remote URL).
- AUTO vs MANUAL approval for the first pass (recommend AUTO for the smoke, then
  one MANUAL pass).

---

## 2. Replication-capable target (the critical setup)

Because replication pushes over SSH, we need at least one target that is
(a) exploitable and (b) SSH-reachable by the agent afterward. Two viable options:

**Option A — dedicated SSH victim container (recommended, most representative).**
Add an `ssh-victim` service to a lab override compose file: a linux/amd64 image with
`openssh-server`, a known-vulnerable app or a deliberately weak account, and the
**agent's public SSH key** pre-planted in `authorized_keys` for `targetUser`
(simulating what a real exploit would install). This makes `EXPLOITATION_KNOWLEDGE`
emit `targetUser:` + `identityFile:` and lets `EXECUTE_EXPLOIT`'s SSH verify pass.
It must run the linux agent (glibc/musl compatible with the native build) and be able
to reach Central. Give it a static IP on the `targets` network (e.g. `172.20.0.30`).

**Option B — docker-api target (`172.20.0.14`, unauth Docker daemon on 2375).**
The exploit spawns a privileged container with host mount → RCE on the DinD host.
This validates the *exploit* leg richly but does **not** naturally yield an SSH
session for the binary push unless the exploit also starts sshd + plants a key inside
the DinD host. Use this to validate exploit + Central approval + audit, and treat the
push as a known follow-up if SSH isn't wired.

I will start with **Option A** for a clean end-to-end pass, and additionally exercise
**Option B** for the exploit/approval/audit legs.

**Setup steps (Option A):**
1. On the lab-server, generate/collect the agent's SSH keypair; note the public key.
2. Write `lab/docker-compose.replication.yml` (override) defining `ssh-victim` with
   the public key baked into `authorized_keys`, sshd running, the target service, and
   `curl` present (so Path A is exercised).
3. `docker compose -f lab/docker-compose.yml -f lab/docker-compose.replication.yml up -d`.
4. Verify from the parent host: `ssh -i <key> <user>@172.20.0.30 'echo OK'` returns OK,
   and the victim can `curl` Central's `/actuator/health`.

---

## 3. Test scenarios

### TS-1 — AUTO_APPROVE happy path (P1, primary smoke)
**Goal:** full chain completes, new agent registers, parent keeps running.
1. Set the project's `replicationPolicy = AUTO_APPROVE` (UI or API), threshold ≤ HIGH.
2. Confirm the linux-x86_64 binary is the one served (`AGENT_BINARY_PATH` / manifest).
3. Assign the parent a plan ending in `TRANSFER_AGENT` scoped to the SSH victim.
4. Trigger the plan; watch parent logs through each step.

**Pass criteria:**
- `REQUEST_REPLICATION` → `APPROVED`, logs carry token/downloadUrl/preauth/centralUrl.
- `EXECUTE_EXPLOIT` → `exploitExecuted:true`, `reverseShellActive:true`.
- `TRANSFER_AGENT` → `integrityCheck:PASSED` (Path B) or `targetDownload:success`
  (Path A), `healthCheck:PASSED` (accept `PARTIAL_SUCCESS` but note it).
- Central shows the **new agent** with replication metadata (parentAgentId, exploit,
  timestamp) and an auto-assigned initial plan; admin notification fired.
- Parent agent reports `REPLICATE`/`TRANSFER_AGENT` COMPLETED and continues its plan.
- **SC-001 timer:** detection→registration under ~3 min.

### TS-2 — MANUAL_APPROVE flow (P2)
1. Set `replicationPolicy = MANUAL_APPROVE`; trigger a request.
2. Verify request appears `PENDING` in the Replication Requests UI with target IP,
   CVE, severity, requesting agent, age; parent is polling (backoff visible in logs).
3. **Approve** → parent receives token, proceeds, new agent registers.
4. Repeat, **Deny** → parent logs "denied. Skipping", step SUCCESS (abandoned), no new
   agent, parent continues.

### TS-3 — Policy threshold fallback (P3)
- AUTO_APPROVE with min severity `CRITICAL`; submit a `HIGH` request → must fall back
  to MANUAL (PENDING), not auto-approve.

### TS-4 — Duplicate suppression (FR-015 / SC-006)
- Two parents (or two runs) target the same IP + same exploit → second gets
  `DUPLICATE`, skips, no second agent record for that target-exploit pair.

### TS-5 — Already-covered target
- Point replication at an IP that already has an active agent → parent skips, reports
  "already covered", no request created.

### TS-6 — Token expiry (FR-007)
- Approve, then delay the transfer past 5 min (or replay the download URL) → download
  endpoint returns expired-token error; parent must re-request (fresh token), old
  token unusable.

### TS-7 — Integrity failure (FR-017 / SC-008)
- Serve a tampered binary or wrong signature (temporarily point `AGENT_BINARY_PATH` at
  a modified file while keeping the old manifest) → parent's `integrityVerifier.verify`
  fails, `TRANSFER_AGENT` FAILS, binary **not executed** on target, admin notified.

### TS-8 — Exploit failure / clean abort (edge, SC-007)
- Make the exploit fail (wrong port / patched victim) → `EXECUTE_EXPLOIT` FAILS,
  `TRANSFER_AGENT` does not run, parent resumes normal plan, no orphaned request state.

### TS-9 — Audit trail completeness (FR-012 / SC-005)
- After any run, verify the Replication Audit entries: request created → policy
  evaluated → decision → exploit start/complete → binary transferred → new agent
  registered (or failure), each with actor + timestamp, immutable.

---

## 4. Observation points / how I'll verify each run

- **Parent agent logs** (journald/log file on lab-server): step-by-step `logs:` lines
  quoted above are the ground truth.
- **API / Mongo:** `replication_requests`, agent collection (new agent + metadata),
  replication audit collection. Query via API endpoints or `mongosh`.
- **UI:** Replication Requests page (status filters), Agents list (replication badge),
  notifications.
- **Target:** `docker exec` into the victim → agent process running, `/tmp/agent`
  present (Path B), `curl localhost:1222/actuator/health` = UP.
- **Timers:** capture wall-clock for SC-001 (≤3 min) and SC-004 (new agent registers
  ≤30s after launch).

---

## 5. Execution order

1. §1 preconditions + §Decision answered.
2. Build/refresh linux-x86_64 agent binary; confirm it's what Central serves.
3. §2 stand up SSH-capable victim (Option A) + keep docker-api (Option B) available.
4. Register parent agent; confirm heartbeat.
5. Run **TS-1** (must pass before anything else).
6. Run TS-2 → TS-9.
7. `lab/scripts/reset-all.sh` between destructive scenarios; wipe replicated-agent
   records so duplicate/already-covered checks start clean.

---

## 6. Known risks / things likely to bite

- **SSH gate is the #1 failure point.** If TS-1 dies at `EXECUTE_EXPLOIT` with
  `reverseShellActive:false`, the victim isn't SSH-reachable with the agent's key —
  fix §2 before blaming the replication code.
- **Binary/arch mismatch.** A macOS or stale binary served to a linux target = launch
  failure. Verify ELF + arch first.
- **Central reachability from inside the `targets` network.** `CENTRAL_URL` must be an
  address the containers can reach (not `localhost` if Central is on the host — use the
  docker gateway / host IP).
- **Clock skew** breaking the 5-min token TTL between lab-server and Central.
- **Native-image caveats**: some flows behave differently under GraalVM vs JVM; if a
  step misbehaves, cross-check with the JAR fallback before assuming a logic bug.

---

## 7. Deliverable

A short run report per scenario: PASS/FAIL, the decisive log lines, timing vs SC
targets, and any deviation. TS-1 + TS-2 passing is the minimum bar for calling
auto-replication validated on the lab.

---

## 8. lab-server topology (dedicated tester environment)

The **lab-server is a dedicated tester host** for deploying the platform, the lab,
VMs, and agents — so everything can co-locate there and I drive it over SSH. This
resolves the §1 "where does Central run" decision and opens up VM targets:

- **Central (API + Mongo + UI)** runs on the lab-server. Agents/containers reach it
  via the docker gateway / host IP, not `localhost` — set `CENTRAL_URL` accordingly.
- **Parent agent** runs on the lab-server host (or an "attacker" container/VM there).
- **Targets** can be Docker containers (§2 Option A/B) **and/or real VMs**. A VM makes
  the SSH-based replication push more realistic than a container: it naturally runs
  sshd, boots the linux agent as a service, and reaches Central over the lab network —
  removing the §6 "SSH gate" workaround. If VMs are available, prefer a VM victim for
  TS-1 and use containers for breadth (TS-4/TS-5 duplicate/coverage, docker-api exploit).
- Because it's a throwaway tester box, destructive scenarios (TS-7/TS-8) and full
  resets are safe; snapshot/reset VMs between runs where possible.

I'll confirm on connect: is a VM victim available (preferred for TS-1), or do we go
with the SSH-victim container from §2 Option A?
</content>
