# Project Constitution

## Core Principles

### 1. Separation of Code and Content

All structured text content — shell scripts, email bodies, JSON manifests, configuration
templates, and any multi-line text blocks — must live in dedicated resource template files.
Never embed these as inline strings in Java/TypeScript source code.

**Rule:**

- `api/`: Use FreeMarker templates in `src/main/resources/scripts/*.ftl` and
  `src/main/resources/templates/*.ftl`. Render via Spring's `FreeMarkerConfigurer`
  or `freemarker.template.Configuration`.

- `agents/unix/`: Use `ClassPathResource` to load templates from
  `src/main/resources/scripts/*.sh.tmpl`, and apply simple `String.replace()`
  for placeholder substitution. FreeMarker is avoided in the agent project
  due to GraalVM native-image compatibility constraints.

**Prohibited patterns:**

- `String.format("#!/bin/bash\n%s\n...", ...)`
- `StringBuilder` or `+` concatenation to build shell scripts, email bodies,
  or structured text
- Inline JSON construction with `String.format`

**Rationale:** Templates externalize content from logic, making scripts
testable, versionable, and auditable independently of application code.
