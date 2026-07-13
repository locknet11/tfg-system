package com.spulido.agent.teardown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes the rendered self-destruct shell to a temporary file and launches it as
 * a detached process that outlives the agent JVM. The script itself waits for the
 * agent process to exit, unlinks the agent binary, sweeps residual artifacts and
 * OS registration, then removes itself.
 */
@Component
public class DetachedCleanupLauncher {

    private static final Logger log = LoggerFactory.getLogger(DetachedCleanupLauncher.class);

    /**
     * Materializes and starts the detached cleanup. Best-effort — a launch
     * failure is logged and swallowed so teardown still proceeds to process exit.
     */
    public void launch(String renderedScript) {
        try {
            Path scriptPath = Files.createTempFile("agent-teardown", ".sh");
            Files.write(scriptPath, renderedScript.getBytes(StandardCharsets.UTF_8));
            scriptPath.toFile().setExecutable(true, true);

            new ProcessBuilder("nohup", "sh", scriptPath.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            log.info("Detached cleanup launched from {}", scriptPath);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to launch detached cleanup: {}", e.getMessage());
        }
    }
}
