# Quickstart: Report Target Resolution

## Build & unit test (local)

```bash
cd api
./mvnw -q test -Dtest=ReportServiceImplTest
# or full: ./mvnw clean package
```

## End-to-end validation against the live lab

The running API on lab-server is `java -jar api.jar` on port 8080. To validate the fix end-to-end you must
rebuild and redeploy that jar (build `api/` → copy `target/*.jar` to lab-server → restart the process).

```bash
# 1. Log in (raw token — do NOT prefix with "Bearer ")
ssh lab-server
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin@lab.local","password":"admin123"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

ORG=6a5541137b29d23e23efdba3
PROJ=6a5541137b29d23e23efdba4

# 2. Find a target id whose ipOrDomain matches the records' host (records store "127.0.0.1")
docker exec tfg-mongo mongosh tfg-system --quiet --eval \
  'db.targets.find({}, {_id:1, systemName:1, ipOrDomain:1}).forEach(d=>printjson(d))'

# 3. BEFORE fix: target-scoped generate returns 422 REPORT_EMPTY_RESULT
# 3. AFTER fix: returns 201 with items and non-null targetName
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/reports \
  -H "Authorization: $TOKEN" -H 'Content-Type: application/json' \
  -H "X-Organization-Id: $ORG" -H "X-Project-Id: $PROJ" \
  -d '{"targetId":"<TARGET_OBJECT_ID_WITH_MATCHING_ipOrDomain>"}'

# 4. Regression: unscoped generate still returns 201 with all records
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/reports \
  -H "Authorization: $TOKEN" -H 'Content-Type: application/json' \
  -H "X-Organization-Id: $ORG" -H "X-Project-Id: $PROJ" -d '{}'
```

## Acceptance mapping

| Spec item | How to verify |
|-----------|---------------|
| US1 / FR-001 / SC-001 | Step 3 after fix returns 201 with items |
| US2 / FR-002 / SC-002 | Report items have non-null `targetName` (e.g. `vm-...`) |
| US3 / FR-003 / SC-003 | Step 4 unscoped still 201 with full record count |
| US3 / FR-004 / SC-004 | Severity/status/date filters and summaries unchanged (unit tests) |
| FR-006 | Only active org/project targets/records matched (unit tests + scoped queries) |
```
