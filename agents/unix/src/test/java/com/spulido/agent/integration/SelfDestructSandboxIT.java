package com.spulido.agent.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.spulido.agent.worker.ScriptTemplateService;

/**
 * Renders {@code self-destruct.sh.tmpl} into a sandbox seeded with fake agent
 * artifacts and runs it, verifying complete removal, idempotency, best-effort
 * resilience, and that the script removes itself last.
 */
class SelfDestructSandboxIT {

    private final ScriptTemplateService templateService = new ScriptTemplateService();

    private Path renderScript(Path sandbox, Map<String, String> paths) throws Exception {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("AGENT_BINARY", paths.getOrDefault("AGENT_BINARY", ""));
        // Empty PID so the wait-for-exit loop is skipped in the test.
        replacements.put("AGENT_PID", "");
        replacements.put("CONFIG_FILE", paths.getOrDefault("CONFIG_FILE", ""));
        replacements.put("LOG_FILE", paths.getOrDefault("LOG_FILE", ""));
        replacements.put("RAW_DOWNLOAD", paths.getOrDefault("RAW_DOWNLOAD", ""));
        replacements.put("INSTALL_SCRIPT", paths.getOrDefault("INSTALL_SCRIPT", ""));
        replacements.put("TOOLS_DIR", paths.getOrDefault("TOOLS_DIR", ""));
        replacements.put("WORKING_DIR", paths.getOrDefault("WORKING_DIR", ""));
        replacements.put("OS_REGISTRATION", paths.getOrDefault("OS_REGISTRATION", ""));

        String rendered = templateService.renderTemplate("self-destruct.sh.tmpl", replacements);
        Path script = sandbox.resolve("teardown.sh");
        Files.writeString(script, rendered);
        script.toFile().setExecutable(true);
        return script;
    }

    private int run(Path script) throws Exception {
        Process p = new ProcessBuilder("sh", script.toString())
                .redirectErrorStream(true)
                .start();
        p.waitFor(30, TimeUnit.SECONDS);
        return p.exitValue();
    }

    @Test
    void removesAllSeededArtifacts_andItself(@TempDir Path sandbox) throws Exception {
        Path binary = sandbox.resolve("agent");
        Path config = sandbox.resolve("agent.properties");
        Path log = sandbox.resolve("agent.log");
        Path toolsDir = sandbox.resolve("agent-tools");
        Path osReg = sandbox.resolve("agent.service");
        Files.writeString(binary, "binary");
        Files.writeString(config, "agent.api-key=secret");
        Files.writeString(log, "log");
        Files.createDirectories(toolsDir);
        Files.writeString(toolsDir.resolve("nmap"), "tool");
        Files.writeString(osReg, "[Unit]");

        Map<String, String> paths = new HashMap<>();
        paths.put("AGENT_BINARY", binary.toString());
        paths.put("CONFIG_FILE", config.toString());
        paths.put("LOG_FILE", log.toString());
        paths.put("TOOLS_DIR", toolsDir.toString());
        paths.put("OS_REGISTRATION", osReg.toString());
        Path script = renderScript(sandbox, paths);

        int exit = run(script);

        assertThat(exit).isZero();
        assertThat(Files.exists(binary)).isFalse();
        assertThat(Files.exists(config)).isFalse();
        assertThat(Files.exists(log)).isFalse();
        assertThat(Files.exists(toolsDir)).isFalse();
        assertThat(Files.exists(osReg)).isFalse();
        assertThat(Files.exists(script)).as("script removes itself last").isFalse();
    }

    @Test
    void idempotent_rerunOnCleanSandboxExitsZero(@TempDir Path sandbox) throws Exception {
        Path config = sandbox.resolve("agent.properties");
        Files.writeString(config, "x");
        Map<String, String> paths = new HashMap<>();
        paths.put("CONFIG_FILE", config.toString());

        Path first = renderScript(sandbox, paths);
        assertThat(run(first)).isZero();
        assertThat(Files.exists(config)).isFalse();

        // Second run: nothing left; still exits cleanly.
        Path second = renderScript(sandbox, paths);
        assertThat(run(second)).isZero();
    }

    @Test
    void bestEffort_lockedArtifactDoesNotBlockOthers(@TempDir Path sandbox) throws Exception {
        Path config = sandbox.resolve("agent.properties");
        Path log = sandbox.resolve("agent.log");
        Files.writeString(config, "secret");
        Files.writeString(log, "log");

        // A file inside a read-only directory cannot be unlinked.
        Path lockedDir = sandbox.resolve("locked");
        Files.createDirectories(lockedDir);
        Path lockedFile = lockedDir.resolve("stuck");
        Files.writeString(lockedFile, "stuck");
        lockedDir.toFile().setWritable(false);

        Map<String, String> paths = new HashMap<>();
        paths.put("CONFIG_FILE", config.toString());
        paths.put("LOG_FILE", log.toString());
        paths.put("INSTALL_SCRIPT", lockedFile.toString());
        Path script = renderScript(sandbox, paths);

        int exit = run(script);

        assertThat(exit).isZero();
        assertThat(Files.exists(config)).as("removable artifacts still removed").isFalse();
        assertThat(Files.exists(log)).isFalse();

        lockedDir.toFile().setWritable(true);
    }
}
