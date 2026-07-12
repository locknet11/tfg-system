# Feature Specification: Real Metrics and Statistics in Graph Views

**Feature Branch**: `018-real-metrics-stats`  
**Created**: 2026-07-12  
**Status**: Draft  
**Input**: User description: "As a user I want to see real metrics a stats in all the views that show graphs (Dashboard, Vulnerabilities, etc)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dashboard Displays Real-Time KPIs and Charts (Priority: P1)

As an administrator viewing the Dashboard, I want all KPI cards, charts, and tables to reflect actual system data so that I can make informed security decisions based on the real state of my infrastructure.

**Why this priority**: The Dashboard is the primary landing page and the single most visible view in the application. Currently all KPIs, the vulnerability bar chart, and the critical vulnerabilities table use hardcoded mock data that never changes, making the dashboard useless for monitoring. Fixing this is the highest-impact change.

**Independent Test**: Log in as an administrator, navigate to the Dashboard, and verify that all KPI numbers (target systems, active agents, fixed vulnerabilities) match what is visible in the Targets, Agents, and Remediations pages respectively. Verify the "Vulnerabilities Detected per Month" chart shows bars reflecting actual vulnerability detection data over time.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator with an organization/project selected, **When** the Dashboard loads, **Then** the "Target Systems" KPI card displays the actual count of targets in the current project scope.
2. **Given** an authenticated administrator with an organization/project selected, **When** the Dashboard loads, **Then** the "Active Agents" KPI card displays the actual count of agents with ACTIVE status in the current project scope.
3. **Given** an authenticated administrator with an organization/project selected, **When** the Dashboard loads, **Then** the "Fixed Vulnerabilities" KPI card displays the actual count of remediations with SUCCESS status.
4. **Given** an authenticated administrator with an organization/project selected, **When** the Dashboard loads, **Then** the "Latest Critical Vulnerabilities" table displays the 10 most recent Critical-severity vulnerability records from the backend instead of hardcoded sample data.
5. **Given** an authenticated administrator with an organization/project selected, **When** the Dashboard loads, **Then** the "Vulnerabilities Detected per Month" bar chart displays real monthly aggregation data pulled from the backend, with labels showing actual months and bars reflecting actual vulnerability counts.

---

### User Story 2 - Agent Metrics Show Live Data (Priority: P2)

As an administrator viewing the Agents page, I want the Key Metrics section (active agents count, detected vulnerabilities, applied remediations, average uptime) and the "Vulnerabilities Over Time" line chart to show real data from the live system, not hardcoded placeholder numbers.

**Why this priority**: The Agents metrics section is prominently displayed and currently shows fake data (e.g., "125 active agents", "348 detected vulnerabilities") that never changes regardless of system state, misleading users.

**Independent Test**: Navigate to the Agents page, verify that the four metric cards show values matching the actual agent counts and activity from the backend, and that the vulnerability trend chart updates when new vulnerability data arrives.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator on the Agents page, **When** the Key Metrics section loads, **Then** the "Active Agents" card shows the real count of agents with ACTIVE status in the current scope.
2. **Given** an authenticated administrator on the Agents page, **When** the Key Metrics section loads, **Then** the "Detected Vulnerabilities" card shows the total count of vulnerability records in the current scope.
3. **Given** an authenticated administrator on the Agents page, **When** the Key Metrics section loads, **Then** the "Applied Remediations" card shows the count of remediations with SUCCESS status.
4. **Given** an authenticated administrator on the Agents page, **When** the Key Metrics section loads, **Then** the "Average Uptime" card shows the real average uptime percentage computed from agent heartbeat data.
5. **Given** an authenticated administrator on the Agents page, **When** the "Vulnerabilities Over Time" chart renders, **Then** the line chart uses real weekly vulnerability detection aggregation data instead of hardcoded sample points.

---

### User Story 3 - Remediation Widget Displays Accurate Statistics (Priority: P1)

As an administrator viewing the remediation statistics widget (on Dashboard and elsewhere), I want the pie chart and status breakdown cards to display accurate, real-time statistics that match the backend data structure, including success count, failed count, pending count, pending reboot count, skipped count, and MTTR (mean time to remediate).

**Why this priority**: The remediation widget already calls a real backend endpoint (`/api/remediations/statistics`), but the frontend data model is out of sync with the backend response structure — the frontend expects flat fields while the backend returns a nested `byStatus` map and `recentActivity` list. This means even the one widget that "tries" to use real data is broken. Fixing this is equally critical as the Dashboard fix.

**Independent Test**: Trigger some remediations, then verify the remediation widget pie chart shows the correct status distribution, the numeric cards match the counts, and the success rate percentage is correctly computed.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator with remediations in various states, **When** the remediation statistics widget loads, **Then** the "Total Remediations" count matches the sum of all remediation records in the current scope.
2. **Given** an authenticated administrator with remediations in various states, **When** the remediation statistics widget loads, **Then** the pie chart displays slices for each status (Success, Failed, Pending, Pending Reboot, Skipped) with correct proportions matching the backend `byStatus` map.
3. **Given** an authenticated administrator with completed remediations, **When** the remediation statistics widget loads, **Then** the "Success Rate" percentage is correctly computed as the ratio of SUCCESS remediations to total completed remediations.
4. **Given** an authenticated administrator with timed remediations, **When** the remediation statistics widget loads, **Then** the MTTR value reflects the actual average time between remediation start and completion across all records.

---

### User Story 4 - Vulnerability Views Support Aggregated Statistics (Priority: P2)

As an administrator browsing the Vulnerabilities page, I want to see aggregated statistics (counts by severity, trends over time) so I can assess the security posture at a glance without manually counting records.

**Why this priority**: The Vulnerabilities page currently only offers a flat filtered list. Adding severity distribution and trend data makes it a useful monitoring tool rather than just a data browser.

**Independent Test**: Navigate to the Vulnerabilities page and verify that severity distribution statistics (Critical, High, Medium, Low counts) are displayed and match the observable records in the filtered list.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator on the Vulnerabilities page, **When** the page loads, **Then** a severity distribution summary is displayed showing counts for Critical, High, Medium, and Low severity vulnerabilities.
2. **Given** an authenticated administrator on the Vulnerabilities page with a severity filter applied, **When** the filter changes, **Then** the statistics update to reflect only the filtered subset.
3. **Given** an authenticated administrator on the Vulnerabilities page, **When** new vulnerability data is fetched (via refresh or agent scan), **Then** the statistics reflect the updated data without requiring a manual page reload.

---

### Edge Cases

- **Empty data states**: When no data exists (no agents registered, no vulnerabilities detected, no remediations performed), all metric views must display zero values or appropriate empty-state indicators instead of showing stale mock data or error states.
- **Scope changes**: When the user switches organization or project, all metrics must immediately reflect the new scope — no cross-contamination of data from the previous scope.
- **Loading states**: While metrics data is being fetched from the backend, graph and KPI areas must show loading indicators (skeleton loaders or spinners) rather than flashing mock data or blank areas.
- **API errors**: If a backend statistics endpoint returns an error, the affected widget must gracefully display an error state with a retry option rather than crashing the entire view or silently showing zero values.
- **Large datasets**: When vulnerability or remediation counts are in the thousands, aggregated statistics must still render without performance degradation in the UI.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Dashboard MUST fetch KPI data (target count, active agent count, fixed vulnerability count) from live backend endpoints instead of returning hardcoded mock values.
- **FR-002**: The Dashboard MUST fetch the critical vulnerabilities table from a real backend endpoint, limited to the 10 most recent Critical-severity records scoped to the current organization and project.
- **FR-003**: The Dashboard MUST fetch the "Vulnerabilities Detected per Month" chart data from a backend aggregation endpoint that groups vulnerability records by calendar month.
- **FR-004**: The Agents Metrics component MUST fetch its four KPI values (active agents, detected vulnerabilities, applied remediations, average uptime) from live backend endpoints instead of using hardcoded constants.
- **FR-005**: The Agents Metrics "Vulnerabilities Over Time" line chart MUST use real aggregation data from the backend instead of hardcoded weekly sample points.
- **FR-006**: The Remediation Statistics widget frontend model MUST be aligned with the backend `RemediationStatistics` response structure, correctly consuming the `byStatus` map, `totalCount`, `meanTimeToRemediateSeconds`, and `recentActivity` fields.
- **FR-007**: The Remediation Statistics widget MUST compute and display the success rate as a percentage based on the ratio of SUCCESS remediations to total completed remediations.
- **FR-008**: The Remediation Statistics widget MUST display the MTTR (mean time to remediate) value in a human-readable format (e.g., "2h 15m") based on the `meanTimeToRemediateSeconds` field from the backend.
- **FR-009**: The system MUST provide a backend API endpoint that returns aggregated vulnerability statistics (counts by severity) scoped to the current organization and project, with optional severity and text-search filters.
- **FR-010**: The Vulnerabilities page MUST display a severity distribution summary (Critical, High, Medium, Low counts) using data from the aggregated statistics endpoint.
- **FR-011**: All metric views MUST update their data when the user switches organization or project context, reflecting only records belonging to the newly selected scope.
- **FR-012**: All metric views MUST display appropriate loading indicators while data is being fetched from the backend.
- **FR-013**: All metric views MUST gracefully handle backend errors by showing an error state with a retry mechanism rather than crashing or silently displaying incorrect data.
- **FR-014**: All metric views MUST render an empty/zero state when no data exists for the current scope, clearly indicating that no records are available rather than showing stale mock data.

### Cross-Cutting Requirements

- **Internationalization**: All new user-facing labels (metric titles, chart legends, empty-state messages, error messages) must be authored in English and added to the `ui/src/i18n/messages.json` and `messages.es.json` files.
- **Accessibility**: Chart components must include accessible alternatives (e.g., `aria-label` on canvas elements, or a supplementary data table for screen readers). Color-coded severity indicators must not rely solely on color to convey information.
- **Validation and Error Handling**: Backend statistics endpoints must validate request parameters (organization/project context) and return appropriate HTTP error responses. Frontend components must use the centralized HTTP error interceptor and handle errors with user-friendly messages.
- **Security Constraints**: All statistics endpoints must enforce the same authentication and project-scoping authorization as the rest of the application. Data returned must be strictly limited to the authenticated user's organization and project context.

### Key Entities

- **Dashboard KPIs**: Aggregated counts — total targets in scope, active agents count, fixed (successfully remediated) vulnerability count. All scoped to the current organization/project.
- **Remediation Statistics**: Aggregated remediation data — total count, status distribution map (SUCCESS, FAILED, PENDING, IN_PROGRESS, PENDING_REBOOT, SKIPPED), MTTR in seconds, and the 5 most recent remediation activities.
- **Vulnerability Statistics**: Aggregated vulnerability data — counts grouped by severity level (Critical, High, Medium, Low, Unknown), with optional filtering by search query.
- **Agent Metrics**: Aggregated agent data — active agent count, total vulnerability count, successful remediation count, average agent uptime computed from heartbeat timestamps, and weekly vulnerability detection trend data.
- **Vulnerability Trend Data**: Time-series aggregation of vulnerability detections grouped by time period (month for Dashboard, week for Agent Metrics) for chart rendering.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All Dashboard KPI values match the corresponding counts visible in the Targets, Agents, and Remediations list views within a 5-second refresh window.
- **SC-002**: The Dashboard "Vulnerabilities Detected per Month" chart bars change when new vulnerability records are added in a different month, verifiable by comparing chart render before and after a new agent scan.
- **SC-003**: The Remediation Statistics widget pie chart segments exactly match the counts shown in the status breakdown cards below it on the same widget.
- **SC-004**: Switching between organizations or projects causes all metric views on the current page to reflect only the new scope's data within 3 seconds.
- **SC-005**: When zero records exist for a metric category (e.g., no failed remediations), the corresponding display shows "0" or an empty segment rather than hiding the category or showing stale data.
- **SC-006**: An administrator can verify data accuracy by cross-referencing any metric value with the corresponding filtered list view — the counts must agree within a 5% tolerance for concurrent modifications.

## Assumptions

- The existing backend already has the raw data needed for most metrics (agents, targets, remediations, vulnerabilities stored in MongoDB); the gaps are in aggregation logic and API endpoints, not in data availability.
- The existing `ProjectContext` tenancy mechanism (organization ID + project ID) will be reused for all new statistics endpoints to enforce proper data scoping.
- The Remediation Statistics backend endpoint (`GET /api/remediations/statistics`) already returns correct real data; only the frontend data model and consumption logic need alignment.
- The heartbeat monitor service already tracks agent connection timestamps, making average uptime computable from existing data.
- Monthly/weekly vulnerability aggregation will group records by their `fetchedAt` or `createdAt` timestamp fields, which are already present in the vulnerability records.
- "Average Uptime" for agents is defined as the percentage of time agents have been in ACTIVE status over the most recent 30-day rolling window.
- The existing PrimeNG chart components and chart.js integration are sufficient for all required chart types (bar, line, pie); no new charting library is needed.
- Mobile responsive behavior is out of scope for v1; desktop viewports are the primary target.

## Constitution Notes

- Repository guidance from `AGENTS.md` and the project constitution at `.specify/memory/constitution.md` applies.
- All UI text must follow the i18n pattern: English-authored human-friendly text in `$localize` or `i18n` attributes, with Spanish translations in message catalogs.
- TypeScript strict mode applies; no `any` types; use explicit interfaces from feature-specific model files.
- Java import ordering: `java.*`, `jakarta.*`, `org.*`, project packages, static last.
- No wildcard imports in Java; use Lombok where present (`@Builder`, `@Data`, `@RequiredArgsConstructor`).
- `.agents/skills/angular-component/SKILL.md` and `.agents/skills/java-springboot/SKILL.md` provide stack-specific guidance for UI and API work respectively.
