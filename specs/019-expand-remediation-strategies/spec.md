# Feature Specification: Expand Remediation Strategies Knowledge Base

**Feature Branch**: `019-expand-remediation-strategies`  
**Created**: 2026-07-12  
**Status**: Draft  
**Input**: User description: "Me gustaría que como usuario poder tener mayor cantidad de vulnerabilidades y su remediacion en el archivo strategies.json." Additional clarification: "Esto también significa agregar más contenedores o máquinas vulnerables al archivo del Docker Compose del laboratorio."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Expanded Vulnerability Detection Coverage (Priority: P1)

As a security operator, I want the autonomous agent to recognize and remediate a broader range of vulnerabilities across common services (web servers, databases, mail servers, DNS, container runtimes, programming language runtimes), so that more infrastructure weaknesses are automatically addressed without manual intervention.

**Why this priority**: The current 6 strategies only cover OpenSSH, nginx, Apache, glibc, and the Linux kernel. Expanding coverage directly increases the system's security value and reduces operator workload across a wider surface area.

**Independent Test**: Can be fully tested by deploying an agent against a target with a newly defined vulnerability (e.g., a vulnerable PostgreSQL or MySQL version) and verifying the agent detects, reports, and remediates it using the corresponding strategy entry.

**Acceptance Scenarios**:

1. **Given** a target host runs a vulnerable version of PostgreSQL, **When** the agent scans the host, **Then** the vulnerability is identified and a matching remediation strategy is applied.
2. **Given** a target host runs a vulnerable version of Docker/runc, **When** the agent scans the host, **Then** the vulnerability is detected and the appropriate fix is executed.
3. **Given** a target host runs a vulnerable version of PHP-FPM, **When** the agent scans the host, **Then** the PHP vulnerability is identified and remediated.

---

### User Story 2 - Multi-OS Strategy Support (Priority: P2)

As a security operator managing heterogeneous cloud environments, I want remediation strategies to cover multiple operating systems (Ubuntu 20.04, Ubuntu 22.04, Debian 11, Debian 12), so that agents can remediate vulnerabilities regardless of the underlying OS flavor.

**Why this priority**: Organizations rarely run a single OS version. Multi-OS coverage ensures the system works in real-world diverse environments.

**Independent Test**: Can be fully tested by deploying agents on different OS versions and verifying each OS receives the correct, version-appropriate remediation commands for the same CVE.

**Acceptance Scenarios**:

1. **Given** a CVE affects a package on both Ubuntu 22.04 and Debian 12, **When** an agent on each OS detects the vulnerability, **Then** each agent applies the correct package version and commands for its specific OS.
2. **Given** a target runs Ubuntu 20.04 with a vulnerable library, **When** the agent scans the host, **Then** the remediation uses the Ubuntu 20.04-specific fix commands and target version.

---

### User Story 3 - Diverse Remediation Actions (Priority: P3)

As a security operator, I want the system to support diverse remediation actions beyond simple `apt upgrade` — including configuration file updates, service restarts, and package installations — so that more complex vulnerability fixes can be automated.

**Why this priority**: Not all vulnerabilities are fixed by package upgrades alone. Some require configuration changes, new package installations, or service restarts. Supporting these expands what can be fully automated.

**Independent Test**: Can be fully tested by creating a strategy with `CONFIG_UPDATE` or `APT_INSTALL` actions, deploying an agent against an affected host, and verifying the correct non-upgrade action executes successfully.

**Acceptance Scenarios**:

1. **Given** a vulnerability requires a configuration file change (e.g., disabling TLS 1.0 in Apache), **When** the agent applies the fix, **Then** the configuration is updated and the service is restarted to pick up the change.
2. **Given** a vulnerability is fixed by installing a new package not previously present, **When** the agent executes the strategy, **Then** the package is installed and verified.

---

### User Story 4 - Strategy Browsing in Dashboard (Priority: P4)

As a platform administrator, I want to view and filter the full remediation strategy catalog via the web dashboard, so I can audit what vulnerabilities the system can automatically handle and verify strategy correctness.

**Why this priority**: Visibility into the strategy database supports auditing, compliance, and operator confidence. Without it, operators cannot verify what the system covers.

**Independent Test**: Can be fully tested by logging into the dashboard, navigating to the remediation strategies view, and verifying all strategies are listed with filtering by CVE, OS, package, or remediation type.

**Acceptance Scenarios**:

1. **Given** the strategies catalog contains 30+ entries, **When** the administrator opens the strategies dashboard view, **Then** all strategies are displayed in a searchable, filterable table.
2. **Given** the administrator filters by package "mysql-server", **When** the filter is applied, **Then** only strategies related to MySQL are shown.
3. **Given** the administrator selects a specific strategy, **When** the details panel opens, **Then** the full strategy details (CVE, commands, target version, notes) are displayed.

---

### User Story 5 - Vulnerable Lab Targets for New Strategies (Priority: P2)

As a security operator testing the system, I want the Docker Compose lab environment to include vulnerable containers that correspond to the newly added remediation strategies, so I can validate end-to-end that each new strategy correctly detects and remediates its target vulnerability.

**Why this priority**: Without matching lab targets, the new strategies cannot be verified in a controlled environment. Testing new remediation strategies requires actual vulnerable services to scan and fix.

**Independent Test**: Can be fully tested by running `docker compose up` in the lab directory, verifying new vulnerable services start successfully, deploying an agent, and confirming the agent detects vulnerabilities on the new containers and applies the corresponding strategies.

**Acceptance Scenarios**:

1. **Given** a new remediation strategy exists for a PostgreSQL vulnerability, **When** the lab is started with `docker compose up`, **Then** a vulnerable PostgreSQL container is running and reachable on its assigned port.
2. **Given** the lab is running with new vulnerable containers, **When** an agent is deployed to the lab network, **Then** the agent discovers the new targets and reports vulnerabilities matching the expanded strategies catalog.
3. **Given** a remediation strategy targets a service on a specific OS (e.g., Debian 12 with MySQL), **When** the lab container for that service is built, **Then** it uses the correct base OS image matching the strategy's operating system field.

---

### Edge Cases

- What happens when a CVE has different fix strategies depending on the minor OS version (e.g., Ubuntu 22.04.1 vs 22.04.3)?
- How does the system handle a CVE that affects multiple packages simultaneously (e.g., a shared library used by several services)?
- What happens when a strategy entry has empty fix commands (action = MANUAL) — how is this presented to the operator?
- How does the system behave when a `strategies.json` entry contains invalid JSON or references a non-existent `remediationType` or `action` enum value?
- What happens when a strategy's `preCheckCommands` fail — does the system still attempt the fix?
- How does the seed process work when the MongoDB collection already has strategies (from a previous seed) — are new entries added or is the collection fully replaced?
- What happens if two strategy entries share the same cveId + operatingSystem combination (duplicate unique index)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST include remediation strategy entries for a minimum of 30 distinct CVEs (up from the current 6), covering at least 15 unique software packages/services.
- **FR-002**: System MUST include remediation strategies covering at minimum the following service categories: web servers (nginx, Apache), databases (PostgreSQL, MySQL/MariaDB), SSH servers (OpenSSH), mail servers, DNS servers, container runtimes, and programming language runtimes (PHP, Python, Node.js).
- **FR-003**: Each strategy entry MUST include a valid CVE identifier, operating system, package name, remediation type, action, target version, pre-check commands, fix commands, post-check commands, and descriptive notes.
- **FR-004**: Remediation strategies MUST cover at least 3 distinct operating systems (e.g., Ubuntu 20.04, Ubuntu 22.04, Debian 11, Debian 12).
- **FR-005**: Strategies MUST utilize at least 3 different remediation action types (e.g., package upgrades, package installations, configuration changes, service restarts, and manual interventions).
- **FR-006**: Each strategy MUST specify whether a reboot is required after remediation (`requiresReboot` field).
- **FR-007**: The `preCheckCommands` MUST verify the current installed package version before attempting any fix.
- **FR-008**: The `postCheckCommands` MUST verify the successful application of the fix (updated version, active service, or configuration change).
- **FR-009**: The dashboard MUST display the full remediation strategy catalog with search and filter capabilities (by CVE ID, operating system, package name, remediation type).
- **FR-010**: The seed process MUST be idempotent — if strategies already exist in the database, new strategies from an updated `strategies.json` MUST be added without duplicating existing entries.
- **FR-011**: Invalid or malformed strategy entries in `strategies.json` MUST be rejected at load time with a clear error message logged, without preventing valid entries from loading.
- **FR-012**: Each strategy entry MUST include human-readable `notes` explaining the nature of the vulnerability and the remediation approach in English.
- **FR-013**: The Docker Compose lab file MUST include at least 10 vulnerable service containers (up from the current 5), with at least one container per service category covered by the strategies (web server, database, mail server, DNS, container runtime, language runtime).
- **FR-014**: Each new lab container MUST be reachable on a unique, documented port and MUST be assigned a static IP within the lab network subnet.
- **FR-015**: Lab containers for multi-OS strategies MUST use the correct base OS image (e.g., `ubuntu:20.04`, `ubuntu:22.04`, `debian:11`, `debian:12`) matching at least one strategy entry per OS.
- **FR-016**: Each lab container MUST expose a service that is verifiably vulnerable to at least one CVE present in the strategies catalog, enabling end-to-end detection and remediation testing.

### Cross-Cutting Requirements

- **Internationalization**: All strategy `notes` fields and dashboard labels must be authored in English. No user-facing translation required for strategy data itself — CVEs and technical commands are international by nature.
- **Accessibility**: The strategies dashboard view must follow existing dashboard accessibility patterns (keyboard navigation, ARIA labels on filter controls and table rows).
- **Validation and Error Handling**: Malformed JSON in `strategies.json` must produce a logged error with the specific entry index and reason, allowing operators to fix the file. The system must continue serving previously loaded strategies from the database.
- **Security Constraints**: Strategy entries must not contain hardcoded credentials. Commands in `fixCommands` must be auditable — administrators must be able to review exact commands the agent will execute before deployment.

### Key Entities

- **Remediation Strategy**: Represents a known vulnerability (CVE) and its fix procedure for a specific operating system. Key attributes: CVE ID, OS, package name, remediation type, action, target version, command lists, reboot requirement, notes. Unique per CVE + OS combination. Used by agents to detect and fix vulnerabilities autonomously.
- **Strategy Catalog**: The complete collection of all remediation strategies available to all agents and organizations. A shared knowledge base, not scoped to individual tenants.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The strategies catalog contains at least 30 distinct CVE entries covering at least 15 different software packages.
- **SC-002**: At least 3 different operating system versions have dedicated strategy entries.
- **SC-003**: At least 3 different remediation action approaches are represented across the catalog (e.g., automated upgrades, configuration changes, manual interventions).
- **SC-004**: At least 5 different service categories (web servers, databases, SSH, mail, DNS, containers, language runtimes) are covered by the strategies.
- **SC-005**: 100% of strategy entries have all mandatory fields populated (CVE ID, OS, package name, remediation type, action, pre-checks, fix commands, post-checks, notes).
- **SC-006**: Administrators can filter and search the strategy catalog in the dashboard and find any specific entry within 5 seconds.
- **SC-007**: The system correctly seeds new strategies into an existing database without data loss or duplicates.
- **SC-008**: Invalid strategy entries in `strategies.json` are rejected with specific error messages, and valid entries continue to load normally.
- **SC-009**: The Docker Compose lab contains at least 10 vulnerable service containers (up from 5).
- **SC-010**: At least 80% of the new lab containers have a corresponding strategy entry in the catalog, enabling end-to-end verification.
- **SC-011**: All new lab containers start successfully with a single `docker compose up` command and are reachable on their documented ports within 60 seconds.

## Assumptions

- The existing strategy seeding mechanism will be extended to support incremental seeding (adding new entries without replacing existing ones), as currently it only seeds when the collection is empty.
- The Dashboard already has a table-based layout pattern that can be reused for the strategies view. No new UI framework or design system is needed.
- CVE and target version information is sourced from publicly available NVD (National Vulnerability Database) and vendor security advisories. Accuracy of version numbers is the responsibility of the content author, not the system.
- Strategies are Linux-focused (APT-based package management) as per the existing architecture. Windows or macOS strategies are out of scope.
- The dashboard strategies view follows the existing table-based layout patterns already established in the platform dashboard.
- New strategies will include CVEs from 2023-2026 that have known, stable fixes available in standard APT repositories.
- The `strategies.json` file will remain the single source of truth for seed data; runtime CRUD operations on strategies via API are out of scope for this feature.
- The existing Docker Compose lab at `lab/docker-compose.yml` will be extended with new service definitions. Existing services (drupal, tomcat, flask, thinkphp, docker-api) will remain unchanged.
- Custom lab container builds will use Dockerfiles under `lab/targets/<service-name>/` following the existing convention.
- Pre-built vulnerable images from vulhub or similar trusted sources may be used where suitable to reduce maintenance burden.

## Constitution Notes

- Repository guidance from `AGENTS.md` applies: Java Spring Boot backend (api/), Angular 17 frontend (ui/), MongoDB for data storage.
- The `.agents/skills/angular-component` and `.agents/skills/java-springboot` skill files provide stack-specific patterns for UI and backend work respectively.
- Principle: security-first — all remediation commands must be auditable, no hardcoded secrets.
- Principle: i18n — user-facing text in English (dashboard labels), strategy notes in English, but CVE data and commands are technical/universal.
