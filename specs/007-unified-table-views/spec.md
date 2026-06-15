# Feature Specification: Unified Table Views

**Feature Branch**: `007-unified-table-views`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "I need as a user of the app. to unify the design of the following views : Targets, Agents, Templates, Vulnerabilities, Alerts. The design must be like in the view Targets, but also adding the possibility to Search with a search box (exclude the search feat in the alerts view). The tables must have the same formatting and styling, always using PrimeNG components and all the labels, placeholders and other texts must be internationalized (i18n). Also all the elements of the view must have i18n."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search Across Data Tables (Priority: P1)

As a platform operator, I need to quickly find specific records (targets, agents, templates, or vulnerabilities) by typing a search term into a search box at the top of each table view, so that I can locate items without manually scanning through pages of data.

**Why this priority**: Search is the most impactful user-facing feature being added; it directly improves daily workflow efficiency for all operators managing security infrastructure.

**Independent Test**: Can be fully tested by navigating to any of the four search-enabled views (Targets, Agents, Templates, Vulnerabilities), typing a search term in the search box, and verifying that the table filters results accordingly. Delivers immediate productivity value even if the styling unification is not yet complete.

**Acceptance Scenarios**:

1. **Given** a user is viewing the Targets page with multiple targets loaded, **When** they type a system name into the search box and press Enter or wait for input to settle, **Then** the table refreshes to show only targets whose system name or description matches the search term.
2. **Given** a user is viewing the Agents page, **When** they type an agent name into the search box, **Then** the table filters to show only matching agents.
3. **Given** a user is viewing the Templates page, **When** they type a template name into the search box, **Then** the table filters to show only matching templates.
4. **Given** a user is viewing the Vulnerabilities page, **When** they type a service name into the search box, **Then** the table filters to show only matching vulnerability records.
5. **Given** a user is viewing the Alerts page, **When** they look for a search box, **Then** no search box is present, as search is intentionally excluded from this view.
6. **Given** a user clears the search box, **When** the search input becomes empty, **Then** the table resets to show all records without any search filter applied.
7. **Given** a user searches for a term that matches no records, **When** the search completes, **Then** the table displays an empty state message indicating no results were found.

---

### User Story 2 - Consistent Visual Experience Across Views (Priority: P2)

As a platform operator, I need all data table views (Targets, Agents, Templates, Vulnerabilities, and Alerts) to share the same visual design, layout, and interaction patterns, so that I can navigate between views seamlessly without having to re-learn each page's interface.

**Why this priority**: Visual consistency reduces cognitive load, improves usability, and ensures a professional, cohesive application experience. It builds on the Targets view as the established design reference.

**Independent Test**: Can be fully tested by visiting each of the five views and verifying that they share the same header layout, table styling, paginator configuration, loading indicators, and row-per-page options. Delivers standalone value as a UX improvement.

**Acceptance Scenarios**:

1. **Given** a user visits any of the five data views, **When** the page loads, **Then** the page header displays an `<h1>` title on the left and a primary action button on the right, matching the Targets view layout.
2. **Given** a user views any of the five data tables, **When** the table renders, **Then** all tables use the same PrimeNG `<p-table>` style class, consistent column header styling, and uniform row styling.
3. **Given** a user views any of the five data tables, **When** the paginator is displayed, **Then** it uses the same rows-per-page options, always-show-paginator behavior, and current-page-report format across all views.
4. **Given** a user views any of the five data tables that support lazy loading, **When** data is being fetched from the server, **Then** a loading indicator is shown consistently across all views.
5. **Given** a user views any table with a status-like column, **When** status values are displayed, **Then** they are rendered using `<p-tag>` with dynamic severity coloring, consistent with the Targets view.
6. **Given** a user views any table with action buttons, **When** actions are available, **Then** they are displayed as icon buttons in a flex row within the Actions column, consistent with the Targets view.
7. **Given** a user views a table with no data, **When** the data set is empty, **Then** an empty state message is displayed using the `<ng-template pTemplate="emptymessage">` pattern.

---

### User Story 3 - Fully Internationalized Interface (Priority: P3)

As a platform operator using the application in my preferred language, I need all visible text across these five views — including table headers, button labels, placeholders, tooltips, status labels, empty state messages, paginator text, and confirmation dialogs — to be displayed in my language, so that I can use the application effectively regardless of my language preference.

**Why this priority**: Internationalization is a foundational requirement for the application's target user base; completing it ensures no hardcoded text remains and supports multi-language deployment.

**Independent Test**: Can be fully tested by switching the application locale and verifying that every text element across all five views renders in the selected language. Delivers standalone value for non-English users.

**Acceptance Scenarios**:

1. **Given** the application is loaded in English locale, **When** any of the five views is displayed, **Then** all page titles, table headers, button labels, input placeholders, tooltips, status labels, and paginator text appear in English.
2. **Given** the application is loaded in Spanish locale, **When** any of the five views is displayed, **Then** all visible text appears in Spanish with no English fallback strings.
3. **Given** a user triggers a confirmation dialog (e.g., delete), **When** the dialog appears, **Then** the title, message, and button labels are displayed in the current locale.
4. **Given** a user performs an action that shows a toast notification, **When** the toast appears, **Then** the message text is displayed in the current locale.
5. **Given** a user hovers over an action icon button, **When** the tooltip appears, **Then** the tooltip text is displayed in the current locale.
6. **Given** a table has an empty state, **When** no data is present, **Then** the empty state message is displayed in the current locale.
7. **Given** a user views the paginator, **When** the current-page-report is shown, **Then** it is displayed in the current locale.

---

### Edge Cases

- What happens when a user types special characters or very long strings in the search box? The search input should handle text gracefully; the server-side search should escape or sanitize input appropriately and return an empty result set for queries that match nothing rather than erroring.
- What happens when a user rapidly types and deletes in the search box? The search should debounce input (e.g., 300ms) to avoid excessive API calls.
- How does the table behave when the server returns an error during search or pagination? A user-friendly error toast should be displayed, and the table should retain its last successfully loaded data.
- What happens when a table column has no data for a given row (null/undefined values)? The cell should display a dash or empty indicator rather than showing "null" or "undefined".
- What happens when rows-per-page exceeds the total record count? The paginator should still display correctly, showing the available number of entries without error.
- What happens when a user resizes the browser window? The table should remain readable; actions columns should maintain their fixed width while other columns adapt.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Targets, Agents, Templates, and Vulnerabilities views MUST each include a text search box positioned above the table that allows users to filter records by name, description, or other relevant searchable text fields.
- **FR-002**: The Alerts view MUST NOT include a search box, as explicitly excluded from the search feature scope.
- **FR-003**: The search box MUST trigger a server-side query when the user presses Enter or after a debounce period (300ms) following the last keystroke.
- **FR-004**: All five views (Targets, Agents, Templates, Vulnerabilities, Alerts) MUST use the same page header layout: an `<h1>` title on the left and a primary action button (when applicable) on the right.
- **FR-005**: All five views MUST use the same PrimeNG `<p-table>` configuration for lazy loading (where applicable), paginator behavior, rows-per-page options, and always-show-paginator setting, matching the Targets view as the reference.
- **FR-006**: All five views MUST use a single shared table style class (or consistent styling approach) so that tables appear visually identical in terms of borders, spacing, header background, row hover, and border radius.
- **FR-007**: All five views MUST display a loading indicator (`[loading]` bound to a loading state signal) while data is being fetched from the server.
- **FR-008**: All five views MUST display an empty state message using the `<ng-template pTemplate="emptymessage">` pattern when the data set is empty.
- **FR-009**: Status-like columns across all views MUST use `<p-tag>` with dynamic severity and label computation, consistent with the Targets view pattern.
- **FR-010**: Action buttons in all views MUST be rendered as icon buttons (`icon="pi pi-..."`, `rounded`, `text`) in a flex row within a fixed-width Actions column, consistent with the Targets view.
- **FR-011**: All user-facing text in all five views — including page titles, table column headers, button labels, input placeholders, tooltips, status labels, paginator text, empty state messages, confirmation dialogs, and toast notifications — MUST be internationalized using Angular i18n (`i18n` attribute directives in templates and `$localize` in TypeScript).
- **FR-012**: All i18n template directives MUST use the correct directive variant for the element context (`i18n` for standalone text, `i18n-label` for button labels, `i18n-header` for PrimeNG component headers, `i18n-placeholder` for input placeholders, `i18n-pTooltip` for tooltips, `i18n-currentPageReportTemplate` for paginator report text).
- **FR-013**: The Templates view MUST be refactored from its current manual paginator setup to use lazy loading with built-in paginator, matching the other views.
- **FR-014**: All views MUST handle null or undefined cell values by displaying a dash ("—") or equivalent placeholder rather than raw null/empty strings.

### Cross-Cutting Requirements

- **Internationalization**: All human-visible text in the five views must be authored in English and registered in the i18n message files (`ui/src/i18n/messages.json` and `ui/src/i18n/messages.es.json`). No hardcoded strings must remain in templates or TypeScript files. Status label functions, confirmation dialog text, and toast messages must use `$localize`.
- **Accessibility**: Search inputs must have accessible labels. Table headers must use proper `<th>` elements. Action icon buttons must have tooltips describing the action. Status tags must convey meaning beyond color (via text labels).
- **Validation and Error Handling**: Search input must handle special characters gracefully. Server errors during search or pagination must surface via toast notification. The table must retain previously loaded data on error.
- **Security Constraints**: Search input must be sanitized server-side to prevent injection attacks. No sensitive data may be exposed in client-side logs or error messages.

### Key Entities

- **Table View Page**: Represents each of the five entity listing pages. Key attributes: title, search query (optional), pagination state (page, size, total), loading state, record collection.
- **Search Filter**: Represents a user-initiated text filter applied to a table view. Key attributes: query text, target entity type, debounce timing.
- **Status Display**: Represents how entity status is rendered. Key attributes: status value, severity level (for tag coloring), localized label.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can locate a specific record by name in any of the four search-enabled views within 5 seconds of typing a search term.
- **SC-002**: All five views present a visually consistent interface — a user navigating between any two views cannot identify layout, spacing, or styling differences in the table, paginator, or page header.
- **SC-003**: 100% of user-facing text elements across all five views are rendered in the selected locale with no hardcoded or untranslated strings remaining.
- **SC-004**: All five views display a loading indicator during data fetch and an empty state message when no records exist, with no view diverging from this behavior.
- **SC-005**: Server-side search queries return results within the same performance envelope as the existing non-search lazy-load queries (no perceptible degradation).

## Assumptions

- The existing Targets view design serves as the canonical reference for the unified design across all views.
- The current Angular 17 application with PrimeNG component library will be used for implementation; no migration to a different framework or component library is in scope.
- The existing i18n infrastructure (`ui/src/i18n/messages.json` and `messages.es.json`) will be extended with new translation entries as needed.
- The existing API endpoints for each entity support a `query` or `search` parameter for server-side text filtering. If any endpoint lacks this support, it will need to be added as part of this work.
- The search functionality uses server-side filtering, not client-side filtering, consistent with the lazy-loading data pattern used in the existing views.
- The Agents view's Metrics tab (with charts and metric cards) is out of scope for table unification but should remain fully functional.
- The Vulnerabilities view's detail page (routed sub-page for individual vulnerability details) is out of scope for this feature.
- The Alerts view intentionally excludes the search feature per explicit user requirement.
- The existing row-per-page options of [10, 25, 50] will be standardized across all views, with `alwaysShowPaginator` set to true.

## Constitution Notes

- AGENTS.md (Angular + Spring Boot): UI changes are limited to the Angular frontend (`ui/`). Angular 17 with standalone components, signals, and strict TypeScript mode applies.
- i18n/UI: All text must be in `ui/src/i18n/*.json`; use `i18n` attribute directives in templates and `$localize` in TypeScript. Human-friendly text must be in English.
- TS style: strict mode, avoid `any`, use explicit types/interfaces, kebab-case filenames, PascalCase components, camelCase variables/functions, Observable suffix `$`.
- PrimeNG components must be used for all table, input, button, tag, and dialog elements.
- `.agents/skills/angular-component` provides guidance for Angular component best practices.
- `.agents/skills/java-springboot` applies if server-side search endpoint changes are needed.
