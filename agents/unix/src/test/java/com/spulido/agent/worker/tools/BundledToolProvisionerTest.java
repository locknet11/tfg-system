package com.spulido.agent.worker.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BundledToolProvisioner} covering os/arch resolution
 * and the lenient startup behaviour (no crash when no binaries present).
 */
class BundledToolProvisionerTest {

    @Test
    void resolveOsArchDirReturnsExpectedFormat() {
        String dir = BundledToolProvisioner.resolveOsArchDir();
        assertNotNull(dir);
        // Must be in "os-arch" format
        assertTrue(dir.matches("[a-z]+-[a-z0-9]+"), "Expected os-arch format, got: " + dir);
    }

    @Test
    void constructorDoesNotThrowWhenNoBinariesPresent() {
        // In test scope, tools resources are not on the classpath, but the provisioner
        // should still construct successfully (lenient mode — steps using missing tools
        // will fail individually with TOOL_ERROR).
        BundledToolProvisioner provisioner = new BundledToolProvisioner();
        assertNotNull(provisioner.getExtractionDirectory());
        // No tools should be resolved since there are no binaries on the test classpath
        assertNull(provisioner.getResolvedPath(BundledTool.NETWORK_DISCOVERY));
    }
}
