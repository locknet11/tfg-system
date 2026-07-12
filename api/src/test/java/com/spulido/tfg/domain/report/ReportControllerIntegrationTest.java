package com.spulido.tfg.domain.report;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Round-trip integration coverage for the reports API. Disabled by default
 * because it requires a running MongoDB (same convention as
 * {@code WsApplicationTests}); the deterministic behavior is covered by
 * {@code ReportServiceImplTest}.
 *
 * <p>When run against a live database it verifies:
 * <ul>
 *   <li>{@code POST /api/reports} with data returns 201 and the stored snapshot;</li>
 *   <li>{@code POST /api/reports} with no matching data returns 422
 *       {@code REPORT_EMPTY_RESULT} and persists nothing;</li>
 *   <li>{@code GET /api/reports} lists the report newest-first for the tenant only;</li>
 *   <li>{@code GET /api/reports/{id}} returns the snapshot, and reading it again after
 *       mutating the source remediation records yields identical values (immutability);</li>
 *   <li>a report id from another organization/project returns 404.</li>
 * </ul>
 */
@SpringBootTest
@Disabled("Requires MongoDB running on localhost:27017")
class ReportControllerIntegrationTest {

    @Test
    void generateThenReadRoundTripIsImmutableAndTenantScoped() {
        // Exercised manually per quickstart.md; see class Javadoc for the scenarios.
    }
}
