# Contracts: Resend Email Integration

**Feature**: 008-resend-email-integration

## Summary

This feature does not introduce any new external contracts. It is an internal implementation change that replaces the email delivery mechanism (SMTP → Resend API) without modifying:

- REST API endpoints
- Request/response DTOs
- WebSocket messages
- Database schemas
- Agent communication protocols

## Internal Service Contract (Unchanged)

The `AlertTriggerService` interface remains unchanged:

```java
public interface AlertTriggerService {
    @Async
    void checkAndTrigger(AlertEvent event);
}
```

The method signature, async behavior, and error handling contract are preserved. Only the internal implementation of email sending changes.

## External Dependencies

### Added
- **Resend API** (`https://api.resend.com`): Outbound HTTPS calls for sending emails. Requires valid API key and verified sender domain.

### Removed
- **SMTP server** (previously `smtp.gmail.com:587`): No longer required.
