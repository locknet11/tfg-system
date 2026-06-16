# Data Model: Resend Email Integration

**Feature**: 008-resend-email-integration
**Date**: 2026-06-15

## Overview

This feature replaces the email delivery mechanism without changing the domain data model. The existing entities (`AlertConfiguration`, `AlertEvent`, `WhenCondition`) remain unchanged. The only data changes are in configuration properties and the internal service implementation.

## Entities

### Existing Entities (Unchanged)

- **AlertConfiguration**: Defines an alert rule with conditions, recipient email (`sendTo`), and tracking fields (`lastTriggeredAt`, `active`). No changes required.
- **AlertEvent**: Represents a security event with type, severity, timestamp, and payload. No changes required.
- **WhenCondition**: Enum of trigger conditions (ON_VULNERABILITY_DETECTED, ON_HIGH_SEVERITY_VULNERABILITY, etc.). No changes required.

### New Configuration Properties

No new domain entities are created. The following configuration properties replace the existing SMTP properties:

| Property               | Type   | Source                    | Description                              |
|------------------------|--------|---------------------------|------------------------------------------|
| `resend.api-key`       | String | `${RESEND_API_KEY:}`      | Resend API authentication key (secret)   |
| `resend.from-address`  | String | `${RESEND_FROM_ADDRESS:}` | Default sender email address for alerts  |

### Removed Configuration Properties

| Property                                          | Reason                                      |
|--------------------------------------------------|----------------------------------------------|
| `spring.mail.host`                               | Replaced by Resend API                      |
| `spring.mail.port`                               | Replaced by Resend API                      |
| `spring.mail.username`                           | Replaced by Resend API key                  |
| `spring.mail.password`                           | Replaced by Resend API key                  |
| `spring.mail.properties.mail.smtp.auth`          | Replaced by Resend API                      |
| `spring.mail.properties.mail.smtp.starttls.enable` | Replaced by Resend API                    |
| `spring.mail.properties.mail.smtp.starttls.required` | Replaced by Resend API                  |

### Internal Value Objects (Not Persisted)

These are internal to the email service implementation and not stored in the database:

- **CreateEmailOptions**: SDK builder object for configuring outbound emails (from, to, subject, html, text). Constructed per-email in the service layer.
- **CreateEmailResponse**: SDK response object containing the email ID returned by Resend on successful send. Used for logging only.

## State Transitions

No state transitions change. The `AlertConfiguration.lastTriggeredAt` field continues to be updated after successful alert sends, regardless of the delivery mechanism.

## Validation Rules

No new validation rules are introduced at the domain level. The Resend SDK validates email parameters (from, to, subject) at the API level and returns `ResendException` with status code `400` for validation failures.
