# Feature Specification: OCI Image Deployment

**Feature Branch**: `023-oci-image-deployment`  
**Created**: 2026-07-14  
**Status**: Draft  
**Input**: User description: "As a user I want to be able to deploy both API and UI as OCI images"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build API Container Image (Priority: P1)

A platform operator wants to package the backend API service into a self-contained OCI-compliant container image that can be run on any container runtime (Docker, Podman, containerd). The operator builds the image from source, pushes it to a container registry, and runs it in a staging environment to verify it starts and serves requests correctly.

**Why this priority**: The API is the core backend service. Without a containerized API, there is no deployable backend for cloud or orchestrated environments. This is the foundational deliverable.

**Independent Test**: Can be fully tested by building the API image, running a container from it, and verifying the API responds to health-check and application requests on the configured port.

**Acceptance Scenarios**:

1. **Given** the API source code and build configuration are available, **When** the operator executes the image build command, **Then** an OCI-compliant container image is produced without errors.
2. **Given** the API image has been built, **When** the operator starts a container from the image, **Then** the API service starts and responds to HTTP requests on its configured port within 60 seconds.
3. **Given** the API container is running, **When** the operator queries the health endpoint, **Then** the service reports a healthy status.
4. **Given** the API image has been built locally, **When** the operator pushes it to a container registry, **Then** the image is stored and pullable from the registry.

---

### User Story 2 - Build UI Container Image (Priority: P2)

A platform operator wants to package the web dashboard into a self-contained OCI-compliant container image. The built image serves the compiled static frontend assets via a lightweight HTTP server inside the container. The operator can run the UI container and access the dashboard from a browser.

**Why this priority**: The UI provides the user-facing management interface. It's independently deployable from the API and delivers value as the visual control plane. It depends on P1 only to the extent that it needs the API to function - but the image itself can be built and verified independently.

**Independent Test**: Can be fully tested by building the UI image, running a container from it, and verifying the dashboard loads in a browser and displays the login page.

**Acceptance Scenarios**:

1. **Given** the UI source code is available, **When** the operator executes the image build command, **Then** an OCI-compliant container image is produced without errors.
2. **Given** the UI image has been built, **When** the operator starts a container from the image, **Then** the dashboard is accessible via HTTP on the configured port.
3. **Given** the UI container is running, **When** the operator navigates to the root URL in a browser, **Then** the application shell loads and displays the expected landing page.
4. **Given** the UI image has been built, **When** the operator pushes it to a container registry, **Then** the image is stored and pullable from the registry.

---

### User Story 3 - External Configuration via Environment (Priority: P2)

An operator deploying to different environments (development, staging, production) needs to configure both the API and UI containers at runtime without rebuilding images. Configuration values such as database connection strings, API endpoints, and feature flags are injected as environment variables or mounted configuration files.

**Why this priority**: Hardcoded configuration prevents the same image from being promoted across environments. External configuration is essential for any real-world deployment pipeline and must work for both images.

**Independent Test**: Can be tested by running the API and UI containers with different environment variable values and verifying each service uses the injected configuration.

**Acceptance Scenarios**:

1. **Given** the API image, **When** the operator starts a container with environment variables for database connection and service port, **Then** the API connects to the specified database and listens on the specified port.
2. **Given** the UI image, **When** the operator starts a container with an environment variable pointing to the API backend URL, **Then** the dashboard proxies or targets the correct API endpoint.
3. **Given** either image running with configuration injected at startup, **When** configuration values are invalid or missing, **Then** the service fails to start with a clear error message indicating which configuration is missing or invalid.

---

### User Story 4 - Health Checks and Observability (Priority: P3)

Container orchestration platforms (Kubernetes, Docker Swarm, Nomad) need health check endpoints and proper process signal handling to manage container lifecycle. Both images expose health endpoints and gracefully handle termination signals.

**Why this priority**: Health checks are important for production readiness but don't block basic deployment. Without them, orchestration platforms can't automatically detect or recover from failures.

**Independent Test**: Can be tested by running each container and verifying its health endpoint returns the expected response, and by sending a SIGTERM and verifying graceful shutdown.

**Acceptance Scenarios**:

1. **Given** the API container is running, **When** the orchestration platform queries the health check endpoint, **Then** it receives a 200 response when healthy and a non-200 response when unhealthy.
2. **Given** the UI container is running, **When** the orchestration platform queries the health check endpoint, **Then** it receives a 200 response indicating the web server is serving content.
3. **Given** either container is running, **When** a SIGTERM signal is sent, **Then** the service stops accepting new requests, completes in-flight requests, and exits cleanly within 30 seconds.

---

### User Story 5 - Multi-Environment Deployment Guide (Priority: P3)

An operator new to the system needs clear documentation on how to build, configure, and run both images across different environments. The documentation covers local development with a container orchestration tool and production deployment on container orchestration platforms.

**Why this priority**: Documentation enables adoption but is not a blocker for image creation. The images themselves are the primary deliverable.

**Independent Test**: Can be tested by having an operator unfamiliar with the project follow the documentation to build and deploy both services from scratch.

**Acceptance Scenarios**:

1. **Given** the deployment documentation, **When** a new operator follows the instructions for local deployment, **Then** both API and UI containers start and communicate correctly using a local container orchestration tool.
2. **Given** the deployment documentation, **When** an operator follows the production deployment instructions, **Then** they can deploy both services to a container orchestration platform with the documented configuration steps.

---

### Edge Cases

- What happens when the container image is built on an ARM-based machine (e.g., Apple Silicon) but needs to run on x86 infrastructure?
- How does the UI container handle the case where the API backend is unreachable at startup?
- What happens when the API container starts but the database is not yet available?
- How are container image tags versioned to track which source commit produced the image?
- What happens when a container runs out of memory or disk space assigned to it?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST produce an OCI-compliant container image for the API backend service that includes the compiled application and all runtime dependencies.
- **FR-002**: System MUST produce an OCI-compliant container image for the UI frontend that serves the compiled static assets via an embedded HTTP server.
- **FR-003**: Both images MUST accept runtime configuration through environment variables for all deployment-specific settings (port, database URL, external service endpoints).
- **FR-004**: Both images MUST expose a health check endpoint that reports service availability status.
- **FR-005**: Both images MUST handle SIGTERM signals gracefully, stopping new request acceptance and completing in-flight work before exiting.
- **FR-006**: Both images MUST be buildable from source without requiring pre-compiled artifacts outside the build context.
- **FR-007**: Image build configuration MUST be committed alongside source code in the repository.
- **FR-008**: Both images MUST run as a non-root user inside the container for security best practices.
- **FR-009**: The UI image MUST support configuring the API backend URL at runtime so the same image works across environments.

### Cross-Cutting Requirements

- **Internationalization**: The UI already supports i18n; the container image must preserve all existing locale and translation assets. Container log messages and error output must be in English.
- **Accessibility**: The containerized UI must preserve all existing accessibility features of the dashboard. No regressions from the non-containerized deployment.
- **Validation and Error Handling**: Both containers must validate required configuration at startup and fail fast with descriptive error messages when mandatory configuration is missing. Runtime errors must be logged to standard output/error streams for collection by orchestration platforms.
- **Security Constraints**: Images must not contain secrets, credentials, or sensitive configuration in any image layer. Base images must be sourced from trusted registries. Images must be scanned for known vulnerabilities before production use. The final images must run with the least privilege necessary (non-root user, minimal filesystem permissions).

### Key Entities

- **API Container Image**: An OCI-compliant image containing the backend API service, its language runtime, and all application dependencies. Key attributes: image name, version tag, exposed port, environment variable schema, health check endpoint.
- **UI Container Image**: An OCI-compliant image containing the compiled web dashboard frontend assets served by an embedded HTTP server. Key attributes: image name, version tag, exposed port, API backend URL configuration, health check endpoint.
- **Image Build Configuration**: A declarative file defining how each image is assembled from base image, dependencies, and application code. Stored as part of the source repository for each module.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An operator can build both container images from a clean source checkout in under 10 minutes on standard development hardware.
- **SC-002**: Both container images start and become healthy in under 60 seconds from container launch on typical cloud instance types.
- **SC-003**: The same API image can be promoted from development to staging to production by changing only environment variables, with zero image rebuilds required.
- **SC-004**: The same UI image can be pointed at different API backends (development, staging, production) at runtime without rebuilding.
- **SC-005**: Both images together add less than 500 MB of storage overhead compared to the base images they extend.
- **SC-006**: A new team member can build and run both services locally using only the repository and a single documented command.

## Assumptions

- The target container runtime is Docker or any OCI-compliant runtime (Podman, containerd). The images follow OCI standards and are not tied to Docker-specific features.
- Multi-architecture builds (linux/amd64 and linux/arm64) are optional for the initial release but the build configuration should not preclude adding them later.
- The API connects to an external database, which is assumed to be available as a separate service (not bundled in the API image).
- The existing build tools for each module are available in the build environment and do not need to be replaced.
- Image tagging follows a scheme that includes the application version and optionally the git commit hash for traceability.
- Container orchestration specifics (Kubernetes manifests, Helm charts) are out of scope for this feature. This feature covers image creation, not orchestration configuration.
- The UI must be built for production (optimized, minified) when included in the image; development-mode serving is out of scope.
- The autonomous agents component is packaged separately via its own build process and is out of scope for these images.

## Constitution Notes

- Repository guidance from `AGENTS.md`: The project uses Angular 17 for UI and Spring Boot 3 for API. Build commands follow the documented patterns (`cd ui && npm ci && npm run build`, `cd api && ./mvnw clean package`). Security constraints require no secrets in images and least-privilege execution.
- The `.agents/skills/` directory contains backend and frontend skill definitions that inform best practices for each module. Both skills emphasize modern patterns already in use.
- The API currently expects an external database. The container image should not bundle or manage the database; it should only configure the connection.
