# Quickstart: Autonomous Remediation Flow

**Date**: 2026-06-26  
**Feature**: 010-remediation-flow

---

## Implementation Order

This guide describes the recommended order for implementing the remediation flow feature. Each step builds on the previous one and produces a testable increment.

### Step 1: API — Enums and Data Model

**Files to create:**
- `api/.../domain/remediation/model/RemediationType.java`
- `api/.../domain/remediation/model/RemediationStatus.java`
- `api/.../domain/remediation/model/RemediationAction.java`
- `api/.../domain/remediation/model/RemediationRecord.java`
- `api/.../domain/remediation/model/RemediationStrategy.java`
- `api/.../domain/remediation/db/RemediationRecordRepository.java`
- `api/.../domain/remediation/db/RemediationStrategyRepository.java`

**Files to modify:**
- `api/.../domain/ScopedEntity.java` — add RemediationRecord instanceof
- `api/.../domain/plan/model/StepAction.java` — add REMEDIATE

**Verification:** Unit tests for entity creation and repository queries.

---

### Step 2: API — Remediation Strategy Knowledge Base

**Files to create:**
- `api/.../domain/remediation/services/RemediationStrategyService.java`
- `api/.../domain/remediation/services/impl/RemediationStrategyServiceImpl.java`
- `api/.../domain/remediation/config/RemediationStrategyLoader.java`
- `api/src/main/resources/remediation/strategies.json`

**Verification:** Unit test that loads strategies and resolves a known CVE.

---

### Step 3: API — Remediation Service and Controller

**Files to create:**
- `api/.../domain/remediation/services/RemediationService.java`
- `api/.../domain/remediation/services/impl/RemediationServiceImpl.java`
- `api/.../domain/remediation/services/RemediationMapper.java`
- `api/.../domain/remediation/controller/RemediationController.java`
- `api/.../domain/remediation/model/dto/RemediationInfo.java`
- `api/.../domain/remediation/model/dto/RemediationStatistics.java`
- `api/.../domain/remediation/exception/RemediationException.java`

**Verification:** Integration test with MockMvc for list, detail, and statistics endpoints.

---

### Step 4: API — Agent Communication Endpoints

**Files to modify:**
- `api/.../domain/agent/controller/AgentCommunicationController.java` — add remediation endpoints
- `api/.../domain/agent/services/AgentCommunicationService.java` — add remediation methods

**Files to create:**
- `api/.../domain/remediation/model/dto/RemediationStrategyRequest.java`
- `api/.../domain/remediation/model/dto/RemediationStrategyResponse.java`
- `api/.../domain/remediation/model/dto/RemediationReportRequest.java`
- `api/.../domain/remediation/model/dto/RemediationReportResponse.java`

**Verification:** Integration test for agent-facing endpoints.

---

### Step 5: API — Alert Integration

**Files to modify:**
- `api/.../domain/remediation/services/impl/RemediationServiceImpl.java` — fire REMEDIATION_COMPLETED events

**Verification:** Unit test that verifies alert event is created on remediation completion.

---

### Step 6: Agent — Step Action and Handler

**Files to modify:**
- `agents/unix/.../domain/task/StepAction.java` — add REMEDIATE
- `agents/unix/.../worker/WorkerCoordinator.java` — register handler and command mapping

**Files to create:**
- `agents/unix/.../worker/step/RemediationStepHandler.java`
- `agents/unix/.../worker/http/dto/RemediationStrategyRequest.java`
- `agents/unix/.../worker/http/dto/RemediationStrategyResponse.java`
- `agents/unix/.../worker/http/dto/RemediationReportRequest.java`
- `agents/unix/.../worker/http/dto/RemediationReportResponse.java`
- `agents/unix/src/main/resources/scripts/remediate.sh.tmpl`

**Files to modify:**
- `agents/unix/.../worker/http/AgentHttpClient.java` — add remediation API methods

**Verification:** Unit test for handler with mocked HTTP client.

---

### Step 7: UI — Models and Service

**Files to create:**
- `ui/src/app/pages/remediations/data-access/remediations.model.ts`
- `ui/src/app/pages/remediations/data-access/remediations.service.ts`

**Verification:** Unit test for service with HttpClientTestingModule.

---

### Step 8: UI — Remediation History Page

**Files to create:**
- `ui/src/app/pages/remediations/feature/remediations-list/remediations-list.component.ts`
- `ui/src/app/pages/remediations/feature/remediations-list/remediations-list.component.html`
- `ui/src/app/pages/remediations/feature/remediations-list/remediations-list.component.spec.ts`
- `ui/src/app/pages/remediations/remediations.routes.ts`

**Files to modify:**
- App routing — add `/remediations` route
- Sidebar menu — add "Remediation History" menu item

**Verification:** Component test with TestBed.

---

### Step 9: UI — Remediation Detail Page

**Files to create:**
- `ui/src/app/pages/remediations/feature/remediation-detail/remediation-detail.component.ts`
- `ui/src/app/pages/remediations/feature/remediation-detail/remediation-detail.component.html`
- `ui/src/app/pages/remediations/feature/remediation-detail/remediation-detail.component.spec.ts`

**Files to modify:**
- Remediations routing — add `/:id` route

**Verification:** Component test with TestBed.

---

### Step 10: UI — Dashboard Widget

**Files to create:**
- `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.ts`
- `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.html`
- `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.spec.ts`

**Files to modify:**
- Dashboard page — add remediation widget

**Verification:** Component test with TestBed.

---

### Step 11: Integration Testing

**Scenarios to test:**
1. Agent executes plan with REMEDIATE step → vulnerability fixed → record created
2. Agent encounters kernel CVE → SKIPPED with safe version
3. Remediation fails → FAILED record with error details → alert fired
4. Remediation succeeds → SUCCESS record → alert fired → email sent
5. Remediation history page shows all records with filters
6. Dashboard widget shows accurate statistics

---

## Prerequisites

Before starting implementation:

1. ✅ MongoDB running and accessible
2. ✅ API project builds successfully (`cd api && ./mvnw clean package`)
3. ✅ Agent project builds successfully (`cd agents/unix && ./mvnw clean package`)
4. ✅ UI project builds successfully (`cd ui && npm ci && npm run build`)
5. ✅ At least one agent deployed on a target with known vulnerabilities

---

## Key Dependencies

### API Module
- `spring-boot-starter-data-mongodb` (existing)
- `spring-boot-starter-web` (existing)
- `spring-boot-starter-validation` (existing)
- `lombok` (existing)

### Agent Module
- `spring-boot-starter-web` (existing)
- `jsch` (existing for SSH)

### UI Module
- `primeng` (existing for table, dialog, tag components)
- `@angular/router` (existing)

No new external dependencies are required for this feature.
