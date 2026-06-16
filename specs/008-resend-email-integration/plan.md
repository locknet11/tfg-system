# Implementation Plan: Resend Email Integration

**Branch**: `008-resend-email-integration` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/008-resend-email-integration/spec.md`

## Summary

Replace the SMTP-based `JavaMailSender` email delivery in `AlertTriggerServiceImpl` with the Resend email API service using the official `resend-java` SDK. The change is scoped to the email sending implementation: configuration moves from SMTP properties to Resend API key + sender address in `application.properties`, and the `sendAlert()` method switches from `SimpleMailMessage` + `JavaMailSender` to `CreateEmailOptions` + `Resend` client. All alert triggering logic, FreeMarker template rendering, and async processing remain unchanged.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.1.3, `com.resend:resend-java` (latest), Lombok 1.18.30, FreeMarker (spring-boot-starter-freemarker)
**Storage**: MongoDB (unchanged — no schema changes)
**Testing**: JUnit 5 + Spring Boot Test (spring-boot-starter-test)
**Target Platform**: Cloud server (Spring Boot web service)
**Project Type**: Web service (REST API)
**Performance Goals**: Email delivery under 5 seconds from trigger to API acceptance
**Constraints**: Resend API rate limit of 5 req/s per team; no client-side rate limiting needed for expected alert volume
**Scale/Scope**: Single service change in `api/` module; no UI or agent changes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` and relevant skills (java-springboot)
- [x] English-only rule satisfied: all code, config keys, and log messages in English
- [x] Proposed design is the smallest correct change: replaces only the email delivery mechanism; no new abstractions, no repository pattern, no new modules
- [x] Stack rules captured: Spring Boot conventions (constructor injection via Lombok `@RequiredArgsConstructor`, `@Value` for config, `@Bean` for client setup), `jakarta.validation` not needed (no new DTOs), centralized exception handling not affected (email errors caught locally)
- [x] Verification steps identified: build with `./mvnw clean package`, manual test by triggering alert event
- [x] Git actions identified: branch already created (`008-resend-email-integration`); commit requires user approval
- [x] Unknown or ambiguous requirements resolved: config via `application.properties` (confirmed by user), no UI form needed

## Project Structure

### Documentation (this feature)

```text
specs/008-resend-email-integration/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
├── contracts/           # Phase 1 contracts
│   └── README.md        # Contract summary (no new external contracts)
├── checklists/          # Quality checklists
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
api/
├── pom.xml                                          # Remove spring-boot-starter-mail, add resend-java
├── src/main/java/com/spulido/tfg/
│   ├── common/config/
│   │   └── ResendConfig.java                        # NEW: @Configuration with Resend @Bean
│   └── domain/alerts/services/impl/
│       └── AlertTriggerServiceImpl.java             # MODIFY: replace JavaMailSender with Resend client
└── src/main/resources/
    ├── application.properties                       # MODIFY: remove spring.mail.*, add resend.*
    └── templates/
        └── alert-email.ftl                          # UNCHANGED: still used for email body rendering
```

**Structure Decision**: Single-project Spring Boot API. Changes are confined to the `api/` module. No new modules, packages, or layers are introduced. A new `ResendConfig` class is added to the existing `common/config/` package (following the project's configuration class pattern).

## Complexity Tracking

> No constitution violations. No justifications needed.

## Implementation Notes

### Files to Modify

1. **`api/pom.xml`**:
   - Remove `spring-boot-starter-mail` dependency
   - Add `com.resend:resend-java` dependency (check Maven Central for latest version)

2. **`api/src/main/resources/application.properties`**:
   - Remove all `spring.mail.*` properties (lines 16-23)
   - Add `resend.api-key=${RESEND_API_KEY:}` and `resend.from-address=${RESEND_FROM_ADDRESS:}`

3. **`api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/AlertTriggerServiceImpl.java`**:
   - Replace `JavaMailSender mailSender` field with `Resend resendClient` and `@Value("${resend.from-address}") String fromAddress`
   - Rewrite `sendAlert()` method to use `CreateEmailOptions.builder()` and `resendClient.emails().send()`
   - Pass FreeMarker-rendered content as `html` field; also set `text` field with the same content as fallback
   - Catch `ResendException` instead of generic `Exception` for email send errors

### Files to Create

4. **`api/src/main/java/com/spulido/tfg/common/config/ResendConfig.java`**:
   - `@Configuration` class with `@Bean` method returning `Resend` client
   - Inject API key via `@Value("${resend.api-key}")`

### Files Unchanged

- `alert-email.ftl` — still used for rendering email body content
- `AlertConfiguration`, `AlertEvent`, `WhenCondition` — domain model unchanged
- `AlertTriggerService` interface — contract unchanged
- All other alert-related classes — unchanged
