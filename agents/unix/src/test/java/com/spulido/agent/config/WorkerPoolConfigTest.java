package com.spulido.agent.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.spulido.agent.domain.task.TaskResult;
import com.spulido.agent.worker.CommandExecutor;
import com.spulido.agent.worker.tools.BundledToolProvisioner;

/**
 * Verifies that the CommandExecutor bean prepends the bundled tools extraction
 * directory to the PATH environment variable used by process invocations.
 */
class WorkerPoolConfigTest {

    @Test
    void commandExecutorPathIncludesBundledToolsDirectory() {
        BundledToolProvisioner provisioner = new BundledToolProvisioner();
        WorkerPoolConfig config = new WorkerPoolConfig();
        CommandExecutor executor = config.commandExecutor(provisioner);

        assertNotNull(executor, "CommandExecutor bean should not be null");

        // Execute a trivial command that echoes the PATH variable
        TaskResult result = executor.execute("echo \"$PATH\"", 5);

        assertTrue(result.isSuccess(), "echo PATH command should succeed");
        String path = result.getMessage();
        assertNotNull(path);

        String toolsDir = provisioner.getExtractionDirectory().toAbsolutePath().toString();
        assertTrue(path.contains(toolsDir),
                "PATH should contain the bundled tools extraction directory: " + toolsDir);
    }
}
