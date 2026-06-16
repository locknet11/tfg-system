# Research: Resend Email Integration

**Feature**: 008-resend-email-integration
**Date**: 2026-06-15

## Research Topics

### 1. Resend Java SDK Integration

**Decision**: Use the official `resend-java` SDK (Maven: `com.resend:resend-java`) as a Spring Bean, configured via `application.properties`.

**Rationale**: The official SDK provides a typed Java client with builder pattern for email options, built-in exception handling via `ResendException`, and documented Spring Boot integration patterns. This is the most straightforward path to replace `JavaMailSender`.

**Alternatives considered**:
- Direct HTTP calls to Resend REST API using `RestTemplate`/`WebClient` — rejected because the official SDK already handles serialization, error mapping, and authentication.
- Third-party Spring Boot Resend starters — rejected because none are officially maintained; the SDK + manual `@Bean` config is simpler and more reliable.

### 2. SDK Configuration Pattern

**Decision**: Configure the Resend client as a Spring `@Bean` in a `@Configuration` class, reading the API key and sender address from `application.properties` (backed by environment variables).

**Rationale**: Follows existing project patterns (e.g., NVD API config uses the same `${ENV_VAR:default}` approach in `application.properties`). The user confirmed configuration is via `application.properties` only — no UI form needed.

**Pattern**:
```properties
resend.api-key=${RESEND_API_KEY:}
resend.from-address=${RESEND_FROM_ADDRESS:}
```

```java
@Configuration
public class ResendConfig {
    @Bean
    public Resend resendClient(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
```

### 3. Error Handling Strategy

**Decision**: Catch `ResendException` and log with status code, error name, and message. Do not retry automatically (consistent with current `JavaMailSender` behavior which also does not retry).

**Rationale**: The current `AlertTriggerServiceImpl.sendAlert()` already wraps email sending in a try-catch and logs failures. The replacement should maintain the same fire-and-forget pattern. Resend error codes:
- `400` — validation error (invalid email format, missing fields)
- `401` — invalid API key
- `404` — resource not found
- `429` — rate limited (5 req/s per team by default)
- `5xx` — server errors

**Alternatives considered**:
- Retry with exponential backoff — rejected because the current system does not retry; adding retry logic would change system behavior beyond the scope of this replacement.
- Circuit breaker — rejected as over-engineering for the current alert volume.

### 4. HTML Email Support

**Decision**: Send emails with both `html` and `text` fields populated. The `html` field contains the FreeMarker-rendered template output. The `text` field serves as a fallback.

**Rationale**: Resend's API accepts both `html` and `text` in `CreateEmailOptions`. The existing `alert-email.ftl` template is plain text; it can be enhanced to HTML or kept as-is and passed to the `html` field (plain text is valid HTML). The `text` field provides a fallback for email clients that prefer plain text.

**Current template**: The existing `alert-email.ftl` is plain text. For Phase 1, it will be rendered and passed as the `html` content. A future enhancement could convert it to a styled HTML template.

### 5. Dependency Changes

**Decision**: Remove `spring-boot-starter-mail` dependency and all `spring.mail.*` properties. Add `com.resend:resend-java` dependency.

**Rationale**: The `spring-boot-starter-mail` provides `JavaMailSender` which is being fully replaced. Keeping it would add unused transitive dependencies (Jakarta Mail). The `spring-boot-starter-freemarker` dependency is retained since FreeMarker templates are still used for rendering email content.

### 6. Rate Limiting Considerations

**Decision**: No client-side rate limiting needed for current alert volume. Log `429` errors clearly for operational awareness.

**Rationale**: Resend's default rate limit is 5 requests/second per team. The alert system processes events asynchronously (`@Async`) and the expected volume of security alerts is well below this threshold. If rate limiting becomes an issue, it would be addressed in a future enhancement.

### 7. Maven Dependency Version

**Decision**: Use the latest stable version of `resend-java` from Maven Central.

**Rationale**: The SDK is actively maintained. Using `LATEST` in documentation but the actual pom.xml should pin a specific version for reproducible builds. The implementer should check Maven Central for the latest stable version at implementation time.

## Summary of Key Decisions

| Topic                    | Decision                                                    |
|--------------------------|-------------------------------------------------------------|
| SDK                      | `com.resend:resend-java` official SDK                       |
| Config                   | `application.properties` with env var backing               |
| Error handling           | Catch `ResendException`, log, no retry                      |
| HTML support             | Both `html` and `text` fields in `CreateEmailOptions`       |
| Dependencies             | Remove `spring-boot-starter-mail`, add `resend-java`        |
| Rate limiting            | No client-side limiting; log 429s                           |
| UI configuration         | Not needed; config via `application.properties` only        |
