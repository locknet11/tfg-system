# Quickstart: Real Metrics and Statistics

**Feature**: `018-real-metrics-stats`  
**Branch**: `018-real-metrics-stats`

## Prerequisites

- Java 21 + Maven (API)
- Node.js 22 + npm (UI)
- MongoDB running (local or configured)
- Authenticated user with at least one organization/project

## Build & Verify

### API

```bash
cd api
./mvnw clean package
./mvnw test -pl . -Dtest="*Dashboard*,*Statistics*,*Metrics*"
```

### UI

```bash
cd ui
npm ci
npm run build
npx prettier --check .
```

## Manual Verification Checklist

### 1. Dashboard KPIs and Charts

1. Log in and select an organization/project
2. Navigate to Dashboard
3. **Verify**: "Target Systems" card shows the same count as the Targets page
4. **Verify**: "Active Agents" card shows the same count as active agents on the Agents page
5. **Verify**: "Fixed Vulnerabilities" card shows the same count as SUCCESS remediations
6. **Verify**: "Latest Critical Vulnerabilities" table shows real CVE data (not CVE-2023-12345)
7. **Verify**: "Vulnerabilities Detected per Month" bar chart shows real months and counts
8. **Verify**: Switch to a different project — all values update within 3 seconds
9. **Verify**: Select a project with no data — zero values shown, no stale mock data

### 2. Remediation Statistics Widget

1. Navigate to Dashboard (widget is at bottom)
2. **Verify**: Pie chart segments are proportional to actual status distribution
3. **Verify**: Status cards (Success, Failed, Pending, Pending Reboot, Skipped) match the pie chart
4. **Verify**: Success Rate percentage is correctly computed
5. **Verify**: MTTR is displayed in human-readable format (e.g., "1h 10m")

### 3. Agent Metrics

1. Navigate to Agents → Key Metrics tab
2. **Verify**: "Active Agents" matches the agent list filter
3. **Verify**: "Detected Vulnerabilities" matches total in Vulnerabilities page
4. **Verify**: "Applied Remediations" matches SUCCESS count in Remediations
5. **Verify**: "Average Uptime" shows a valid percentage (0–100)
6. **Verify**: Line chart shows real weekly data, not hardcoded sample points

### 4. Vulnerability Statistics

1. Navigate to Vulnerabilities
2. **Verify**: Severity distribution summary shows counts for all severity levels
3. **Verify**: Apply a severity filter — summary updates to reflect the filter
4. **Verify**: Search for a service name — summary updates to match filtered results
5. **Verify**: Switch to a project with no vulnerabilities — all counts show zero

### 5. Empty & Error States

1. **Empty state**: Create a fresh project with no agents/targets/vulnerabilities
   - Dashboard shows "No targets configured", "No active agents", etc.
2. **Error state**: Stop the API server
   - Navigate to Dashboard
   - Error message appears with "Retry" button
   - Start API server, click Retry — data loads successfully
3. **Loading state**: Use browser DevTools to throttle network
   - Navigate between views
   - Skeleton loaders or spinners appear while data loads

## Key Architectural Notes

- **No new collections**: All data is read from existing `agents`, `targets`, `remediation_records`, and `service_vulnerabilities`
- **New `dashboard` domain**: `api/src/main/java/com/spulido/tfg/domain/dashboard/` with controller, DTOs, and service
- **Extended controllers**: `AgentController` gains `GET /api/agent/metrics`; `VulnerabilityController` gains `GET /api/vulnerabilities/statistics`
- **Frontend model fixes**: `RemediationStatistics` TS interface aligned with backend DTO
- **All mock data removed**: `DashboardService` methods now make real HTTP calls; `AgentsMetricsComponent` calls `AgentsService.getMetrics()`

## Testing Tips

- **API unit tests**: Mock repositories; verify DTO fields are populated from repository results
- **API integration test**: `DashboardControllerIntegrationTest` — verify endpoint returns correct counts with seeded data
- **UI unit tests**: Spy on `DashboardService`, `AgentsService`, `VulnerabilitiesService`; assert component renders values from service responses
- **E2E smoke test**: Seed some agents/targets/vulnerabilities, log in, verify all views show matching numbers
