# Feature Specification: Agent Download Portal

**Feature Branch**: `013-agent-download`  
**Created**: 2026-07-07  
**Status**: Draft  
**Input**: User description: "I want to make the agent available for download from central platform"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrator Downloads Latest Agent Binary (Priority: P1)

An administrator of an organization logs into the central platform dashboard, navigates to the agent management section, and downloads the latest agent binary for their target platform (e.g., Linux, macOS). The downloaded file is a ready-to-run agent package that can be deployed to any machine the administrator manages.

**Why this priority**: This is the core capability — without it, no agent download exists. It enables administrators to manually deploy agents to machines that are not reachable through automated replication, such as air-gapped environments, legacy hosts, or machines requiring manual approval. It is the entry point for all download functionality.

**Independent Test**: Can be fully tested by logging in as an organization administrator, clicking the download button for the Linux agent, and verifying the binary file is correctly downloaded and executable on a Linux target.

**Acceptance Scenarios**:

1. **Given** an authenticated administrator is on the agent management page, **When** they click "Download Agent" for the Linux platform, **Then** the browser initiates a file download of the agent binary with the correct filename, and the file is a valid executable.
2. **Given** an authenticated administrator is on the agent management page, **When** they click "Download Agent" for macOS, **Then** the browser downloads the macOS-native agent binary that runs correctly on macOS.
3. **Given** an unauthenticated user (no valid session), **When** they attempt to access the download URL directly, **Then** the system rejects the request with an authentication error and no file is served.
4. **Given** an authenticated user without agent-download permissions, **When** they attempt to download the agent, **Then** the system rejects the request with an authorization error.
5. **Given** no agent binary exists for the requested platform, **When** a download is requested, **Then** the system returns a clear "not available for this platform" message.

---

### User Story 2 - Administrator Selects Agent Version (Priority: P2)

An administrator needs to download a specific version of the agent binary — not necessarily the latest — because their deployment environment requires a known-good version that has been validated internally. They navigate to the agent downloads section, view the list of available versions with release dates, and select the desired version for download.

**Why this priority**: Version selection is essential for production deployments where stability and compatibility are paramount. Organizations need to control which agent version runs on their infrastructure, and may need to roll back to a previous version if a new one introduces issues.

**Independent Test**: Can be tested by viewing the version list for a platform, selecting a non-latest version, downloading it, and verifying the downloaded binary matches the selected version.

**Acceptance Scenarios**:

1. **Given** multiple agent versions have been published, **When** the administrator opens the agent download section, **Then** they see a list of available versions with release dates, platform availability, and a clear indicator of which version is the latest.
2. **Given** the administrator is viewing the version list, **When** they select version 1.2.0 and click download, **Then** the system serves the exact binary for version 1.2.0 for the selected platform.
3. **Given** a version has been marked as deprecated or withdrawn, **When** the version list is displayed, **Then** deprecated versions are visually flagged and show a warning, but remain downloadable for administrators who need them.
4. **Given** no versions are available for a platform, **When** the administrator views the download section, **Then** a clear message indicates that no agent binaries are currently available for that platform.

---

### User Story 3 - Administrator Views Agent Package Details (Priority: P3)

Before downloading, an administrator wants to verify the integrity and provenance of the agent binary. They view metadata about the agent package: version, release notes, file size, checksum, supported platforms, and publication date. This information helps them make informed decisions about which version to deploy.

**Why this priority**: While not essential for the download itself, package details build trust and enable administrators to verify they are downloading the correct, untampered binary. This is important for security-conscious organizations.

**Independent Test**: Can be tested by viewing the details panel for a specific agent version and verifying all metadata fields are correctly displayed.

**Acceptance Scenarios**:

1. **Given** an agent version exists with full metadata, **When** the administrator views its details, **Then** they see the version number, release date, file size, checksum, supported operating systems, and release notes.
2. **Given** the administrator is viewing package details, **When** they copy the checksum, **Then** they can independently verify the downloaded binary matches the published checksum.

---

### Edge Cases

- What happens when the administrator requests a download while a new version is being published? The system serves the previously published version until publication is complete.
- What happens when the agent binary file on the server is corrupted or missing? The system detects this and returns an error with a clear message instead of serving a broken file.
- What happens when multiple administrators from different organizations download simultaneously? Each download is independent and authenticated; no cross-organization data leaks.
- What happens when a very large binary download is interrupted? The system supports resumable downloads so administrators don't need to restart from scratch.
- What happens when an administrator downloads an agent for a platform that differs from the server's platform? The system serves cross-platform binaries correctly — a Linux server can serve macOS and Windows agent binaries.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The central platform MUST serve agent binary files as downloadable artifacts through authenticated endpoints.
- **FR-002**: The system MUST require valid authentication before serving any agent download.
- **FR-003**: The system MUST require the user to have the agent-download permission within their organization to access downloads.
- **FR-004**: The dashboard MUST display available agent versions grouped by target platform (Linux, macOS, etc.).
- **FR-005**: The dashboard MUST show, for each agent version: version number, release date, file size, checksum, supported platforms, and release notes.
- **FR-006**: Administrators MUST be able to download any available (non-withdrawn) version of the agent for any supported platform.
- **FR-007**: The system MUST track download events (who downloaded which version, when, from which organization) for audit purposes.
- **FR-008**: The system MUST serve the correct platform-specific binary based on the requested platform, regardless of the server's native platform.
- **FR-009**: The dashboard MUST visually distinguish the latest/recommended version from older and deprecated versions.
- **FR-010**: Deprecated agent versions MUST remain downloadable but show a clear deprecation warning to the administrator.
- **FR-011**: The download response MUST include proper content-type and content-disposition headers so browsers handle the file correctly.
- **FR-012**: The system MUST support resumable downloads (HTTP Range requests) for large agent binaries.

### Cross-Cutting Requirements

- **Internationalization**: All user-facing text in the download interface must be authored in English. Future localization support should be considered for version labels and status messages.
- **Accessibility**: The download interface must be keyboard-navigable. Download buttons and version selectors must have accessible labels. Status indicators (latest, deprecated) must not rely solely on color.
- **Validation and Error Handling**: Invalid version identifiers must return clear error messages. Requests for unsupported platforms must return descriptive errors. Missing or corrupted binaries must be detected server-side before serving. All error responses must be in a consistent format.
- **Security Constraints**: Agent binaries must be served over HTTPS only. Download endpoints must enforce authentication and authorization. Checksums must be served alongside binaries so administrators can verify integrity. The system must not expose internal file paths in error messages.

### Key Entities

- **Agent Package**: Represents a published agent binary for a specific platform and version. Key attributes: version identifier, target platform, file path, file size, checksum, publication timestamp, deprecation status, release notes.
- **Agent Download Record**: Represents a single download event. Key attributes: downloading user, organization, agent package reference, download timestamp, client IP address. Used for audit trail.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An authenticated administrator can locate and initiate an agent download from the dashboard in under 30 seconds.
- **SC-002**: Agent binary download completes successfully for files up to 100 MB on a standard broadband connection without corruption.
- **SC-003**: The checksum published alongside the binary matches the downloaded file with 100% accuracy.
- **SC-004**: All download events are recorded in the audit log within 5 seconds of completion.
- **SC-005**: Administrators can identify the latest version from the version list at a glance (in under 5 seconds).
- **SC-006**: 100% of unauthorized download attempts are rejected without serving any file content.

## Assumptions

- The agent binary is already built and packaged as part of the existing build pipeline (agents/unix). This feature only concerns serving and downloading the pre-built artifact.
- The central platform uses role-based access control with an existing permission for agent management; a specific "agent-download" permission or equivalent will be added.
- Administrators have modern web browsers capable of handling file downloads.
- The agent binary files are stored on the central platform's server filesystem or accessible object storage — this feature does not include the storage infrastructure itself.
- Platform identifiers follow a standard convention (e.g., `linux-x86_64`, `macos-aarch64`, `macos-x86_64`).
- The existing auto-replication HTTP download endpoint (spec 005) may be reused or extended; this feature focuses on the user-facing download experience.
- Agent versions follow semantic versioning (MAJOR.MINOR.PATCH).

## Constitution Notes

- Repository guidance from `AGENTS.md` applies: Angular 17 standalone components for UI, Spring Boot 3 for API, GraalVM native agent in `agents/unix/`.
- The `angular-component` skill provides best practices for standalone component structure with signal-based inputs, which should be followed for new download UI components.
- The `java-springboot` skill provides Spring Boot conventions for new API endpoints.
- All user-facing text must be in English with proper i18n annotations in Angular templates.
