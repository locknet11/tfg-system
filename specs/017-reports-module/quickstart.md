# Quickstart: Reports Module (017-reports-module)

How to build, run, and manually verify the feature. Assumes MongoDB is available per the
existing project setup.

## Build & test

```bash
# API (Spring Boot)
cd api && ./mvnw clean package          # compile + run unit/integration tests
./mvnw test                             # tests only

# UI (Angular)
cd ui && npm ci && npm run build        # production build
npx prettier --check .                  # formatting gate (no ESLint configured)
```

## Run locally

1. Start the API (`cd api && ./mvnw spring-boot:run`) and the UI (`cd ui && npm start`).
2. Log in as the administrator and select an organization/project that has remediation
   activity (seed some `remediation_records` if empty).

## Manual verification (maps to spec success criteria)

1. **US-1 / SC-1 — Generate & view (≤3 clicks, ~3s)**
   - Click **Reports** in the main menu → the Reports page loads (previously a dead link).
   - Click **Generate report** (optionally set target / date range / severity / status).
   - The report detail opens: severity bar/doughnut chart, status pie chart, KPI cards
     (MTTR, totals, targets covered), and a detail table. Confirm it renders within ~3s.
2. **SC-4 — MTTR non-zero**
   - With at least one `SUCCESS` remediation having both `startedAt` and `completedAt`,
     confirm the MTTR KPI is greater than zero. Cross-check the dashboard statistics widget
     now also shows a real MTTR (the previously hardcoded `0` is fixed).
3. **SC-5 / FR-008 — Empty result stores nothing**
   - Generate with filters that match no data → a "no data for the selected filters" message
     appears; the history count does **not** increase.
4. **US-2 / SC-2 — History & immutability**
   - Open **Reports** history: previously generated reports appear newest first.
   - Reopen a report → identical values to generation time.
   - Change/delete an underlying `remediation_records` document, reopen the same report →
     values are unchanged (immutable snapshot).
5. **SC-6 — Tenancy**
   - Switch to a different organization/project → its history shows only its own reports.
   - `GET /api/reports/{id}` for a report id from another tenant returns 404.
6. **US-3 / FR-009 — Scheduled generation** (optional; disabled by default in tests)
   - In a non-test profile set `reports.scheduler.enabled=true` and a short `reports.scheduler.cron`.
   - After the interval, a new report tagged `SCHEDULED` appears in the history of each
     project that has data; projects with no data get no report.

## API smoke (curl)

```bash
# Generate (auth header/cookie omitted for brevity)
curl -X POST http://localhost:8080/api/reports -H 'Content-Type: application/json' -d '{}'
# History
curl http://localhost:8080/api/reports?page=0&size=20
# Detail
curl http://localhost:8080/api/reports/<id>
```

Expected: `POST {}` on a project with data → 201 with summary+items; on a project with no
matching data → 422 `REPORT_EMPTY_RESULT` and no new history row.

## Key files (for reviewers)

- API: `api/src/main/java/com/spulido/tfg/domain/report/**`, plus the MTTR fix in
  `domain/remediation/services/impl/RemediationServiceImpl.java` and `application.yml`
  scheduler flags.
- UI: `ui/src/app/pages/reports/**`, the lazy `reports` route in `app-routing.module.ts`,
  and new i18n labels in `ui/src/i18n/messages.json` + `messages.es.json`.
