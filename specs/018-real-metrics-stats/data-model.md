# Data Model: Real Metrics and Statistics

**Feature**: `018-real-metrics-stats`  
**Date**: 2026-07-12

## Overview

This feature is **read-only** — no new MongoDB collections, no new persisted entities, no write operations. All data is derived on-the-fly from four existing collections: `agents`, `targets`, `remediation_records`, and `service_vulnerabilities`.

## New DTOs (API Response Models)

### DashboardKpis

Aggregated key performance indicators for the dashboard overview.

| Field | Type | Source |
|-------|------|--------|
| `targetsCount` | `long` | `targets` collection count scoped to org/project |
| `activeAgentsCount` | `long` | `agents` collection count where `status = ACTIVE` |
| `fixedVulnerabilitiesCount` | `long` | `remediation_records` count where `status = SUCCESS` |

### CriticalVulnerabilityInfo

Lightweight summary of a Critical-severity vulnerability for the dashboard table.

| Field | Type | Source |
|-------|------|--------|
| `serviceKey` | `String` | `service_vulnerabilities._id` or `serviceKey` |
| `cveId` | `String` | From `cves[].cveId` where severity = CRITICAL |
| `description` | `String` | From `cves[].description` |
| `serviceName` | `String` | `service_vulnerabilities.serviceName` |
| `cvssScore` | `Double` (nullable) | From `cves[].cvssScore` |
| `reportedDate` | `Instant` | `service_vulnerabilities.fetchedAt` |

### VulnerabilityTrendPoint

Single data point for a bar/line chart showing vulnerability counts over time.

| Field | Type | Description |
|-------|------|-------------|
| `period` | `String` | Month label e.g. `"2026-07"` or week label e.g. `"2026-W28"` |
| `count` | `long` | Number of vulnerability records with `fetchedAt` in that period |

### VulnerabilityStatistics

Severity distribution for the vulnerabilities page.

| Field | Type | Description |
|-------|------|-------------|
| `totalCount` | `long` | Total vulnerability records matching current filters |
| `bySeverity` | `Map<String, Long>` | Counts keyed by severity: `{ "CRITICAL": 2, "HIGH": 5, "MEDIUM": 3, "LOW": 1, "UNKNOWN": 0 }` |

Supports optional query parameters: `query` (service name search), `severity` (filter to specific severity).

### AgentMetricsResponse

Aggregated KPIs and trend data for the agent metrics view.

| Field | Type | Source |
|-------|------|--------|
| `activeAgents` | `long` | `agents` count where `status = ACTIVE` |
| `totalAgents` | `long` | `agents` count in scope |
| `detectedVulnerabilities` | `long` | `service_vulnerabilities` count in scope |
| `appliedRemediations` | `long` | `remediation_records` count where `status = SUCCESS` |
| `uptimePercentage` | `double` | `activeAgents / totalAgents * 100`, or 0 if no agents |
| `vulnerabilityTrend` | `List<VulnerabilityTrendPoint>` | Weekly aggregation for last 4 weeks |

## Existing Entities (Read-Only)

These are document structures in existing collections. No schema changes.

### Agent (`agents` collection)

Used fields: `_id`, `status` (enum: IN_CREATION, CREATED, ACTIVE, UNRESPONSIVE, KILLED), `lastConnection` (LocalDateTime), `organizationId`, `projectId`, `name`, `version`.

### Target (`targets` collection)

Used fields: `_id`, `organizationId`, `projectId`, `systemName`, `status`.

### RemediationRecord (`remediation_records` collection)

Used fields: `_id`, `cveId`, `targetId`, `status` (enum: PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED), `startedAt`, `completedAt`, `organizationId`, `projectId`.

### ServiceVulnerabilityRecord (`service_vulnerabilities` collection)

Used fields: `_id`, `serviceKey`, `serviceName`, `serviceVersion`, `cves` (array of `CveEntry`), `fetchedAt` (Instant, indexed), `totalCves`.

CveEntry embedded fields: `cveId`, `description`, `cvssScore`, `severity`.

## State Transitions

None. This feature is purely read-only — it queries existing data and returns aggregated views. No new entities are created, updated, or deleted.

## Validation Rules

| Rule | Applies To | Error Response |
|------|------------|---------------|
| Organization/project context must be set | All endpoints | 422 with code `NO_PROJECT_CONTEXT` |
| `months` query param must be 1–24 | `/api/dashboard/vulnerability-trend` | 400 validation error |
| `weeks` query param must be 1–12 | `/api/agent/metrics` trend | 400 validation error |
| Authentication required | All endpoints | 401 |

## Relationships

```
DashboardKpis
  ├── targetsCount → count from targets collection
  ├── activeAgentsCount → count from agents collection
  └── fixedVulnerabilitiesCount → count from remediation_records collection

AgentMetricsResponse
  ├── activeAgents → agents (ACTIVE)
  ├── totalAgents → agents (all)
  ├── detectedVulnerabilities → service_vulnerabilities (all)
  ├── appliedRemediations → remediation_records (SUCCESS)
  ├── uptimePercentage → computed from activeAgents / totalAgents
  └── vulnerabilityTrend → service_vulnerabilities (weekly aggregation)

VulnerabilityStatistics
  └── bySeverity → service_vulnerabilities.cves[].severity (aggregation)

RemediationStatistics (existing, frontend model fix only)
  ├── totalCount → remediation_records (all in scope)
  ├── byStatus → remediation_records (grouped by status)
  ├── meanTimeToRemediateSeconds → computed via RemediationMetrics util
  └── recentActivity → remediation_records (top 5 by completedAt desc)
```
