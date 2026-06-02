package com.spulido.agent.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Path A (HTTP download) end-to-end.
 *
 * Prerequisites:
 * - Central running at configured URL
 * - Target machine with SSH access and curl installed
 * - Project configured with AUTO_APPROVE replication policy
 * - Valid exploit script that prints REVERSE_SHELL_READY
 *
 * Expected flow:
 * EXPLOIT → REQUEST_REPLICATION → EXECUTE_EXPLOIT → TRANSFER_AGENT (Path A)
 * Target downloads binary from Central via curl/wget, launches agent,
 * health check returns UP within 30 seconds.
 */
@Disabled("Requires running Central and target with SSH+curl")
class PathAIntegrationTest {

    @Test
    void endToEndPathA() {
        // 1. Start agent (polls Central for plan)
        // 2. Agent receives plan with EXPLOIT→TRANSFER flow
        // 3. Agent executes exploit against target
        // 4. Agent requests replication from Central
        // 5. Central approves replication (AUTO_APPROVE)
        // 6. Agent probes target: curl=available, centralReachable=true
        // 7. Agent renders install-agent-http.sh.tmpl
        // 8. Agent transfers install script to target
        // 9. Target downloads binary from Central via curl
        // 10. Target launches agent
        // 11. Health check returns UP within 30s
        // 12. Assert StepResult.success() with path=HTTP_DOWNLOAD
    }
}
