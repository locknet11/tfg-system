# Tasks: Resend Email Integration

**Feature**: 008-resend-email-integration
**Branch**: `008-resend-email-integration`
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

## Phase 1: Setup

**Goal**: Add Resend SDK dependency and configuration infrastructure

- [x] T001 Add `com.resend:resend-java` dependency to `api/pom.xml` (check Maven Central for latest version)
- [x] T002 Remove `spring-boot-starter-mail` dependency from `api/pom.xml`
- [x] T003 Create `ResendConfig` class in `api/src/main/java/com/spulido/tfg/config/ResendConfig.java` with `@Bean` method returning `Resend` client (inject API key via `@Value("${resend.api-key}")`)
- [x] T004 Add Resend configuration properties to `api/src/main/resources/application.properties`:
  - `resend.api-key=${RESEND_API_KEY:}`
  - `resend.from-address=${RESEND_FROM_ADDRESS:}`

---

## Phase 2: User Story 1 - System Sends Security Alert Emails via Resend (P1)

**Story Goal**: Replace SMTP-based email sending with Resend API in `AlertTriggerServiceImpl`

**Independent Test**: Trigger a security event matching an active alert configuration and verify the recipient receives an email via Resend

- [x] T005 [US1] Update `AlertTriggerServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/AlertTriggerServiceImpl.java`:
  - Replace `JavaMailSender mailSender` field with `Resend resendClient` and `@Value("${resend.from-address}") String fromAddress`
  - Remove `import org.springframework.mail.SimpleMailMessage` and `import org.springframework.mail.javamail.JavaMailSender`
  - Add imports for `com.resend.Resend`, `com.resend.services.emails.model.CreateEmailOptions`, `com.resend.services.emails.model.CreateEmailResponse`, `com.resend.core.exception.ResendException`
- [x] T006 [US1] Rewrite `sendAlert()` method in `AlertTriggerServiceImpl.java`:
  - Build `CreateEmailOptions` using builder pattern with `from`, `to`, `subject`, and `text` fields
  - Call `resendClient.emails().send(options)`
  - Catch `ResendException` and log error with status code, message, recipient, and event type
  - Preserve existing success logging

---

## Phase 3: User Story 2 - Email Configuration Uses Resend API Key (P2)

**Story Goal**: Configuration is complete (covered in Phase 1 setup tasks T003-T004)

**Independent Test**: Verify system starts successfully with Resend configuration and can authenticate with Resend API

- [x] T007 [US2] Remove all `spring.mail.*` properties from `api/src/main/resources/application.properties` (lines 16-23)

---

## Phase 4: User Story 3 - Alert Emails Support HTML Content (P3)

**Story Goal**: Send emails with both HTML and plain-text content for better email client compatibility

**Independent Test**: Send an alert email and verify it contains both HTML and plain-text versions

- [x] T008 [US3] Update `sendAlert()` method in `AlertTriggerServiceImpl.java` to set both `html` and `text` fields in `CreateEmailOptions`:
  - Pass FreeMarker-rendered content to `html` field
  - Pass same content to `text` field as fallback (current template is plain text, which is valid for both)

---

## Phase 5: Polish & Verification

**Goal**: Verify the implementation and clean up

- [x] T009 Build the project with `cd api && ./mvnw clean package` to verify no compilation errors
- [x] T010 Verify no remaining references to `JavaMailSender` or `spring.mail` in the codebase (grep for `JavaMailSender`, `SimpleMailMessage`, `spring.mail`)
- [ ] T011 Manual test: Configure `RESEND_API_KEY` and `RESEND_FROM_ADDRESS` environment variables, start the application, trigger a test alert event, and verify email delivery

---

## Dependencies

```
Phase 1 (Setup)
  ↓
Phase 2 (US1 - Core email sending)
  ↓
Phase 3 (US2 - Config cleanup)
  ↓
Phase 4 (US3 - HTML support)
  ↓
Phase 5 (Polish)
```

**Note**: Phases 2-4 are sequential because they all modify the same file (`AlertTriggerServiceImpl.java`). However, US1 (Phase 2) delivers the MVP — once T006 is complete, the system can send emails via Resend.

---

## Parallel Execution Opportunities

**Limited parallelism**: Most tasks modify the same file (`AlertTriggerServiceImpl.java`), so they must be executed sequentially.

**Potential parallel work**:
- T001-T004 (setup) can be done in parallel if different files are modified
- T009-T010 (verification) can run in parallel after implementation is complete

---

## Implementation Strategy

**MVP Scope**: Phase 1 + Phase 2 (T001-T006)

Once T006 is complete, the system can send alert emails via Resend. The remaining phases are refinements:
- Phase 3: Cleanup of old SMTP config (cosmetic)
- Phase 4: HTML support (enhancement)
- Phase 5: Verification

**Incremental Delivery**:
1. After T006: System sends plain-text emails via Resend (MVP)
2. After T007: Old SMTP config removed (clean)
3. After T008: Emails support both HTML and plain-text (enhanced)

---

## Task Summary

- **Total tasks**: 11
- **Phase 1 (Setup)**: 4 tasks
- **Phase 2 (US1 - Core)**: 2 tasks
- **Phase 3 (US2 - Config)**: 1 task
- **Phase 4 (US3 - HTML)**: 1 task
- **Phase 5 (Polish)**: 3 tasks

**Files modified**:
- `api/pom.xml` (T001, T002)
- `api/src/main/java/com/spulido/tfg/common/config/ResendConfig.java` (T003 - new file)
- `api/src/main/resources/application.properties` (T004, T007)
- `api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/AlertTriggerServiceImpl.java` (T005, T006, T008)
