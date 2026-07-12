# Research: Expand Remediation Strategies Knowledge Base

**Feature**: 019-expand-remediation-strategies  
**Date**: 2026-07-12

## Research Tasks

### 1. CVE Selection for Strategy Expansion

**Decision**: Select 24+ new CVEs from 2023-2026 across 7 service categories, prioritizing vulnerabilities with stable APT fixes available for Ubuntu 20.04/22.04 and Debian 11/12.

**Rationale**:
- The system already uses APT-based package management — new strategies must work with the same mechanism.
- Ubuntu LTS (20.04, 22.04) and Debian stable (11, 12) are the most common server OS distributions in cloud environments.
- CVEs from 2023-2026 ensure the vulnerabilities are recent enough to be relevant for demonstrations but old enough to have upstream fixes available.
- NVD (nvd.nist.gov) and vendor security trackers (ubuntu.com/security, debian.org/security) provide authoritative CVE and fix version data.

**Alternatives considered**:
- Pulling from OSV.dev or GitHub Advisory Database: more automated but less curated; risk of including CVEs without available APT fixes.
- Including older CVEs (pre-2023): less relevant for demonstrations of current security posture.
- Including CVEs requiring vendor-specific installers: out of scope (APT-only).

**Target CVE categories and count**:

| Category | Packages | Target CVEs | Example CVEs |
|----------|----------|-------------|--------------|
| Web servers | nginx, apache2 | 4-5 | CVE-2024-40725 (Apache), CVE-2024-7342 (nginx) |
| Databases | postgresql, mysql-server, mariadb-server | 5-6 | CVE-2024-4317 (PostgreSQL), CVE-2024-20963 (MySQL) |
| SSH | openssh-server | 2-3 | Already have 2; add CVE-2024-6409 |
| Mail servers | postfix, dovecot, exim4 | 3-4 | CVE-2023-51764 (Postfix SMTP smuggling) |
| DNS servers | bind9 | 2-3 | CVE-2024-0760 (BIND9) |
| Container runtimes | docker.io, containerd, runc | 3-4 | CVE-2024-21626 (runc), CVE-2024-23652 (containerd) |
| Language runtimes | php, python3, nodejs | 4-5 | CVE-2024-4577 (PHP CGI), CVE-2024-4032 (Python) |

### 2. Incremental Seeding Strategy

**Decision**: Modify `RemediationStrategyLoader` to use `save()` instead of `saveAll()`, iterating through strategies and inserting individually, skipping duplicates by catching `DuplicateKeyException` or using `findByCveIdAndOperatingSystem` pre-check.

**Rationale**:
- The current implementation only seeds when the collection is empty (`existingCount > 0 → skip`). This prevents adding new strategies without manual database intervention.
- Spring Data MongoDB's `save()` performs an upsert when `@Id` is set, but our unique constraint is a compound index on `(cveId, operatingSystem)`, not on `@Id`. Using individual saves with a pre-existence check is the safest approach.
- `MongoTemplate.upsert()` with `FindAndReplaceOptions.upsert()` is an alternative but more complex for a simple use case.

**Alternatives considered**:
- Drop and re-seed: would lose runtime-created strategies and is destructive. Rejected.
- Use `@Id` as composite of cveId+os: would change the domain model. Rejected — `@Id` should remain a MongoDB ObjectId.
- Bulk insert with `ordered: false`: MongoDB would silently skip duplicates but wouldn't update existing entries. Rejected.
- Version field + compare: over-engineered for this feature. Rejected.

### 3. Lab Container Design

**Decision**: Create new lab containers using a mix of pre-built vulhub images and custom Dockerfiles under `lab/targets/<service>/`. Each container must:
1. Use a specific OS base image matching strategy entries (ubuntu:20.04, ubuntu:22.04, debian:11, debian:12).
2. Install a verifiably vulnerable version of the target package.
3. Expose the service on a unique port with a static IP.

**Rationale**:
- Existing lab follows the pattern of `lab/targets/<service>/` for custom builds (tomcat, flask, docker) and vulhub images for pre-built vulnerable services (drupal, thinkphp).
- Custom Dockerfiles give us control over the exact OS version and package version to match strategy entries.
- Pre-built vulhub images save time for complex setups but may not match the exact OS version needed for multi-OS strategies.
- Static IPs on the `172.20.0.0/24` subnet ensure consistent target addressing during agent scans.

**Alternatives considered**:
- Using only vulhub images: insufficient OS variety (most vulhub images are Debian-based).
- Using full VMs (Vagrant): heavier resource usage, slower startup. Rejected for lab simplicity.
- Using Kubernetes pod definitions: overkill for a local lab. Docker Compose is sufficient.

### 4. New API Endpoint for Strategy Catalog

**Decision**: Add `GET /api/remediation-strategies` to the existing `RemediationController` (or a new `RemediationStrategyController`) with query parameters for filtering by `cveId`, `operatingSystem`, `packageName`, and `remediationType`. Paginated response with `Page<RemediationStrategyResponse>`.

**Rationale**:
- The UI needs to fetch the full strategy catalog for the dashboard view.
- Filtering server-side avoids sending the entire catalog to the browser (important as the catalog grows).
- Pagination with `PageRequest` follows existing patterns in `RemediationController.listRemediations()`.
- A separate controller (`RemediationStrategyController`) is cleaner than mixing strategy CRUD with remediation records, but we can use the existing controller to minimize changes (following the Constitution's minimal-change principle).

**Alternatives considered**:
- No new endpoint, serve `strategies.json` as a static resource: bypasses DB, no filtering, not RESTful. Rejected.
- GraphQL: adds unnecessary complexity. REST with query params is sufficient.

### 5. Dashboard Strategy Catalog View

**Decision**: Create a new Angular standalone component `StrategiesListComponent` under `ui/src/app/pages/remediations/feature/strategies-list/` following the unified table pattern. Uses PrimeNG `p-table` with `p-paginator`, filter inputs for each column, and a detail expansion panel.

**Rationale**:
- The existing UI at `ui/src/app/pages/remediations/` already has list/detail components (`remediations-list`, `remediation-detail`). The strategy catalog follows the same layout pattern.
- PrimeNG `p-table` with `p-paginator` is already used in the targets list and remediations list — reusing the same component library ensures visual consistency.
- The component is standalone (Angular 17+ convention) with OnPush change detection and signal-based inputs, following the Angular skill guidance.

**Alternatives considered**:
- Adding strategy data to the existing remediations-list component: mixes concepts (remediation records vs. strategy catalog). Rejected.
- Server-side rendering: overkill for an admin dashboard view. CSR with pagination is sufficient.
