# Implementation Plan: Vulnerable Test Lab

**Branch**: `006-vulnerable-test-lab` | **Date**: 2026-06-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-vulnerable-test-lab/spec.md`

## Summary

Create a local Docker-based test lab with 5 intentionally vulnerable targets (Drupal CVE-2018-7600, Tomcat CVE-2017-12615, Flask SSTI, ThinkPHP 5 RCE, Docker API RCE) for validating autonomous security agents. The lab is deployed with a single command, runs on an isolated network, and includes scripts for stop/reset lifecycle management.

## Technical Context

**Language/Version**: Shell scripts (bash), Docker Compose V2, YAML
**Primary Dependencies**: Docker Desktop/Docker Engine, VulHub vulnerability images
**Storage**: N/A (ephemeral containers; no persistent state)
**Testing**: Manual verification via curl/HTTP requests against each target's exploit endpoint
**Target Platform**: macOS (development), GNU/Linux (EC2 production)
**Project Type**: Test infrastructure / lab environment
**Performance Goals**: Full deploy < 3 minutes, stop/reset < 1 minute (from SC-001, SC-003)
**Constraints**: Not exposed to internet; isolated Docker network; no auth on any target; requires container runtime
**Scale/Scope**: 5 concurrent targets, single host, single Docker Compose project

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: `AGENTS.md` and any relevant `.agents/skills/*/SKILL.md`
  - AGENTS.md rules for Angular/Spring Boot do not apply (this is standalone test infra under `lab/`)
  - Template/script rules (FreeMarker for api/, ClassPathResource for agents/unix/) do not apply (lab uses Docker Compose + bash)
- English-only rule satisfied for code, UI text, docs, and comments: all user-facing output will be in English
- Proposed design is the smallest correct change and avoids unnecessary abstraction: lab is a single docker-compose.yml + bash scripts; no service layers or abstractions
- Stack rules captured for affected modules (`ui/`, `api/`, `agents/unix/`): no existing modules modified
- Verification steps identified for every affected module: each target verified via curl exploit test
- Git actions identified; explicit user approval required before any git command runs: no git actions needed beyond initial feature branch
- Unknown or ambiguous requirements resolved or called out before implementation: all 3 clarify questions resolved in spec session

## Project Structure

### Documentation (this feature)

```text
specs/006-vulnerable-test-lab/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
├── spec.md              # Feature specification
└── checklists/          # Quality checklists
```

### Source Code (repository root)

```text
lab/                              # Lab root directory
├── docker-compose.yml            # Global orchestration
├── .env                          # Port/IP configuration
├── scripts/
│   ├── deploy-all.sh             # Clone VulHub + build + up
│   ├── stop-all.sh               # docker compose down
│   └── reset-all.sh              # down + cleanup + redeploy
└── targets/                      # VulHub vulnerability environments
    ├── drupal/                   # CVE-2018-7600
    ├── tomcat/                   # CVE-2017-12615
    ├── flask/                    # SSTI
    ├── thinkphp/                 # CVE-2018-20062
    └── docker/                   # Unauthorized Docker API RCE
```

**Structure Decision**: Single project directory `lab/` with Docker Compose at root, scripts in `lab/scripts/`, and vulnerability environments in `lab/targets/`. Each target is a subdirectory copied from VulHub with its own Dockerfile or prebuilt image reference.

## Complexity Tracking

No constitution violations to justify. Feature is standalone test infrastructure with minimal abstraction.
