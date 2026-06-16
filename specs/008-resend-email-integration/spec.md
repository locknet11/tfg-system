# Feature Specification: Resend Email Integration

**Feature Branch**: `008-resend-email-integration`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "I would like to replace the current Java Mail Sender implementation to use Resend service"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - System Sends Security Alert Emails via Resend (Priority: P1)

A system administrator has configured alert rules for security events (vulnerability detected, remediation completed, scan completed, etc.). When a security event matches an active alert rule, the system automatically sends a formatted email notification to the configured recipient address using the Resend email service, replacing the previous SMTP-based email delivery.

**Why this priority**: This is the core functionality being replaced. Without working email delivery through Resend, the entire alert notification system is non-functional. This is the minimum viable change.

**Independent Test**: Can be fully tested by triggering a security event that matches an active alert configuration and verifying the recipient receives the email via Resend. Delivers immediate value by restoring email delivery through a modern, reliable email API.

**Acceptance Scenarios**:

1. **Given** an active alert configuration exists with a valid recipient email address, **When** a matching security event occurs (e.g., vulnerability detected), **Then** the system sends an email to the configured recipient via Resend with the alert details rendered in the email body.
2. **Given** the Resend service is unavailable or returns an error, **When** the system attempts to send an alert email, **Then** the error is logged with sufficient detail for diagnosis and the system continues processing remaining alerts without crashing.
3. **Given** multiple active alert configurations match a single event, **When** the event is processed, **Then** each matching configuration results in a separate email sent via Resend to its respective recipient.

---

### User Story 2 - Email Configuration Uses Resend API Key (Priority: P2)

A system administrator needs to configure the email delivery service. Instead of setting up SMTP server credentials (host, port, username, password), the administrator provides a Resend API key and a default sender email address. The system uses these credentials to authenticate with the Resend service for all outbound emails.

**Why this priority**: Configuration is essential for the feature to function but is secondary to the core sending capability. The administrator must be able to set up Resend credentials before any emails can be sent.

**Independent Test**: Can be tested by configuring the Resend API key and sender address in the application configuration, then verifying that the system authenticates successfully with Resend when sending a test email.

**Acceptance Scenarios**:

1. **Given** a valid Resend API key and sender email are configured, **When** the system initializes the email service, **Then** the service is ready to send emails through Resend without requiring any SMTP configuration.
2. **Given** the Resend API key is missing or invalid, **When** the system attempts to send an email, **Then** a clear error message is logged indicating the authentication failure, and the email is not sent.
3. **Given** the previous SMTP configuration properties exist in the configuration file, **When** the system starts up, **Then** the SMTP properties are no longer used and do not interfere with Resend-based email delivery.

---

### User Story 3 - Alert Emails Support HTML Content (Priority: P3)

A system administrator wants alert emails to be visually formatted with HTML content, leveraging the existing FreeMarker templates that were previously used for plain-text email bodies. The Resend integration supports sending HTML-formatted emails, improving readability of security alert notifications.

**Why this priority**: HTML email support enhances the user experience but is not strictly required for the replacement to be functional. The existing plain-text fallback can serve as a temporary measure.

**Independent Test**: Can be tested by sending an alert email and verifying the recipient receives a properly formatted HTML email with styled content from the FreeMarker template.

**Acceptance Scenarios**:

1. **Given** an alert email is triggered, **When** the system renders the email content using the existing FreeMarker template, **Then** the email is sent via Resend with HTML content type, and the recipient sees a formatted email.
2. **Given** the FreeMarker template fails to render, **When** the system falls back to a plain-text email body, **Then** the email is still sent via Resend with the plain-text content as a fallback.

---

### Edge Cases

- What happens when the Resend API rate limit is reached? The system logs the rate limit error and continues processing; failed emails are not retried automatically (consistent with current behavior).
- What happens when a recipient email address is invalid? The Resend API returns an error which is logged; the system continues processing other alerts.
- What happens when the email body exceeds Resend's maximum size limit? The system truncates or simplifies the content to fit within limits, or logs an error if the content cannot be reduced.
- What happens during application startup if the Resend API key is not configured? The email service fails gracefully with a clear configuration error message; other system functionality remains operational.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST send alert notification emails using the Resend email API service instead of SMTP-based JavaMailSender.
- **FR-002**: System MUST authenticate with Resend using an API key provided through application configuration.
- **FR-003**: System MUST use a configurable sender email address for all outbound alert emails.
- **FR-004**: System MUST support sending HTML-formatted email content via Resend.
- **FR-005**: System MUST support sending plain-text email content as a fallback when HTML rendering fails.
- **FR-006**: System MUST preserve the existing alert triggering logic (condition matching, event processing, last-triggered tracking) without modification.
- **FR-007**: System MUST log email delivery success and failure with sufficient detail for operational monitoring.
- **FR-008**: System MUST handle Resend API errors gracefully without disrupting the alert processing pipeline.
- **FR-009**: System MUST remove all SMTP-related configuration properties (host, port, username, password, SMTP auth settings) from the application configuration.

### Cross-Cutting Requirements

- **Validation and Error Handling**: All Resend API responses must be checked for errors. Failed sends must be logged with the error details including the recipient address, event type, and Resend error message. The system must not throw unhandled exceptions from the email sending path.
- **Security Constraints**: The Resend API key must be treated as a secret. It must be provided via environment variable or externalized configuration, never hardcoded. The API key must not appear in application logs.

### Key Entities

- **Email Message**: Represents an outbound email with sender address, recipient address(es), subject line, and body content (HTML or plain text). Sent via the Resend API.
- **Resend Configuration**: Holds the API key and default sender email address used to authenticate and send emails through the Resend service.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of alert emails that previously would have been sent via SMTP are now delivered through the Resend service without manual intervention.
- **SC-002**: Email delivery latency through Resend is under 5 seconds from trigger to API acceptance confirmation.
- **SC-003**: Zero unhandled exceptions occur in the email sending path during normal operation; all errors are logged with actionable details.
- **SC-004**: System administrators can configure Resend credentials and have the email service operational within 5 minutes, without requiring SMTP server setup.
- **SC-005**: All existing alert rules and conditions continue to function identically after the migration, with no change in triggering behavior.

## Assumptions

- The Resend Java SDK is used as the client library for interacting with the Resend API.
- The existing FreeMarker email templates (`alert-email.ftl`) will continue to be used for rendering email content; only the delivery mechanism changes.
- The Resend API key will be provided as an environment variable (`RESEND_API_KEY`) following the existing pattern for secrets in the application.
- The sender email address will be provided via application configuration (environment variable or properties file).
- The current alert processing logic (async execution, condition matching, last-triggered tracking) remains unchanged; only the email sending implementation is replaced.
- Resend's free or pro tier provides sufficient sending volume for the expected alert load.
- No email attachments are required for alert notifications (current implementation does not use attachments).
- The system does not need to track email delivery status beyond the initial API acceptance response from Resend.

## Constitution Notes

- The existing codebase follows Spring Boot conventions with Lombok, FreeMarker templates, and `@Async` processing. The Resend integration should follow these same patterns.
- Per AGENTS.md, all multi-line structured text must live in resource template files. The FreeMarker template (`alert-email.ftl`) is already in `src/main/resources/templates/` and should remain there.
- The Resend API key is a secret and must not be committed to the repository. It should follow the existing pattern of environment variable injection (e.g., `${RESEND_API_KEY:}`).
- The current `JavaMailSender` dependency and `spring.mail.*` properties should be fully removed as part of this change.
