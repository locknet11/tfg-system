# Implementation Plan: Expand Remediation Strategies Knowledge Base

**Branch**: `019-expand-remediation-strategies` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/019-expand-remediation-strategies/spec.md`

## Summary

Expand the remediation strategies knowledge base from 6 to 30+ CVE entries covering 15+ software packages across 4 OS variants. Extend the Docker Compose lab from 5 to 10+ vulnerable containers matching the new strategies. Add a dashboard view for browsing the strategy catalog. Update the seed loader to support incremental seeding (add new entries without replacing existing ones).

## Technical Context

**Language/Version**: Java 17+ (api/), TypeScript 5+ (ui/), YAML/Dockerfile (lab)  
**Primary Dependencies**: Spring Boot 3, Spring Data MongoDB, Angular 17, PrimeNG, Docker Engine  
**Storage**: MongoDB (`remediation_strategies` collection)  
**Testing**: JUnit 5 + Mockito (api/), Jasmine + Karma (ui/), Docker Compose (lab integration)  
**Target Platform**: Linux amd64 (containers + agent), macOS (agent native), Docker (lab)  
**Project Type**: Web application (api/ + ui/) + Docker Compose lab  
**Performance Goals**: Seed time <5s for 50 strategies; dashboard load <2s for strategy listing  
**Constraints**: strategies.json under ~500 entries for maintainable seed time; APT-only package management  
**Scale/Scope**: 30+ strategies, 10+ lab containers, 4 OS variants, 7 service categories

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` applies for api/ (Spring Boot conventions, constructor injection, DTO boundaries, centralized error handling) and ui/ (Angular strict TS, kebab-case naming, i18n with `$localize`, Prettier formatting). `.agents/skills/angular-component/SKILL.md` and `.agents/skills/java-springboot/SKILL.md` provide stack-specific patterns.
- [x] English-only rule satisfied: all strategy `notes`, dashboard labels, and documentation authored in English.
- [x] Proposed design is the smallest correct change: expanding existing JSON data file + adding lab containers + minimal new UI view. No new frameworks, no schema changes, no API redesign.
- [x] Stack rules captured: api/ changes follow Spring Boot patterns (constructor injection, `@RestController`, DTO via `RemediationStrategyResponse`); ui/ changes follow Angular patterns (standalone components, OnPush, signal inputs, i18n).
- [x] Verification steps identified: unit tests for RemediationStrategyLoader incremental seeding; manual `docker compose up` for lab verification; UI component tests for strategy catalog view.
- [x] Git actions identified: branch `019-expand-remediation-strategies` already created; no additional git actions needed without explicit user approval.
- [x] Unknown requirements resolved: user clarified lab expansion is in scope.

## Project Structure

### Documentation (this feature)

```text
specs/019-expand-remediation-strategies/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── strategy-api.yaml
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/
├── src/main/resources/remediation/
│   └── strategies.json          # EXPAND: 6 → 30+ entries
├── src/main/java/com/spulido/tfg/domain/remediation/
│   ├── config/
│   │   └── RemediationStrategyLoader.java  # MODIFY: incremental seeding
│   ├── controller/
│   │   └── RemediationStrategyController.java  # NEW: strategy listing endpoint
│   ├── model/dto/
│   │   └── RemediationStrategyResponse.java   # NEW: strategy DTO
│   └── services/impl/
│       └── RemediationStrategyServiceImpl.java # MODIFY: add list/filter
└── src/test/java/com/spulido/tfg/domain/remediation/
    └── config/
        └── RemediationStrategyLoaderTest.java  # NEW: seed tests

lab/
├── docker-compose.yml            # EXPAND: 5 → 10+ services
└── targets/
    ├── postgres/                  # NEW: vulnerable PostgreSQL
    │   └── Dockerfile
    ├── mysql/                     # NEW: vulnerable MySQL/MariaDB
    │   └── Dockerfile
    ├── bind9/                     # NEW: vulnerable DNS server
    │   └── Dockerfile
    ├── postfix/                   # NEW: vulnerable mail server
    │   └── Dockerfile
    ├── php-fpm/                   # NEW: vulnerable PHP runtime
    │   └── Dockerfile
    └── nodejs/                    # NEW: vulnerable Node.js runtime
        └── Dockerfile

ui/src/app/pages/remediations/
├── feature/
│   └── strategies-list/           # NEW: strategy catalog view
│       ├── strategies-list.component.ts
│       ├── strategies-list.component.html
│       └── strategies-list.component.scss
├── data-access/
│   ├── remediations.service.ts    # MODIFY: add strategy listing methods
│   └── remediations.model.ts      # MODIFY: add strategy interfaces
└── remediations.routes.ts         # MODIFY: add strategies route
```

**Structure Decision**: Web application structure (api/ + ui/ + lab/). The feature spans all three modules: api/ for the strategy catalog API and seed logic, ui/ for the dashboard view, and lab/ for the vulnerable containers. No new modules or packages are created — all changes extend existing domains.

## Complexity Tracking

> No constitution violations. All changes follow existing patterns and conventions.
