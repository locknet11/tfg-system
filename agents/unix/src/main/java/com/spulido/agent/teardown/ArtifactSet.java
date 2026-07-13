package com.spulido.agent.teardown;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.spulido.agent.worker.tools.BundledToolProvisioner;

/**
 * Resolves the concrete host paths of the artifacts the agent (and its
 * installer) placed on the target, and removes them idempotently and
 * best-effort. Only artifacts the agent created are enumerated — no unrelated
 * host data is ever touched.
 *
 * <p>The install layout matches {@code install-agent-http.sh.tmpl}: the binary,
 * config, and log live under {@link #INSTALL_DIR}. Downloaded tool binaries live
 * in the {@link BundledToolProvisioner} extraction directory. The running binary
 * path is discovered from the current process, falling back to the install path.
 */
@Component
public class ArtifactSet {

    private static final Logger log = LoggerFactory.getLogger(ArtifactSet.class);

    static final String INSTALL_DIR = "/tmp";
    static final String BINARY_NAME = "agent";
    static final String CONFIG_NAME = "agent.properties";
    static final String LOG_NAME = "agent.log";
    static final String RAW_DOWNLOAD_NAME = "agent_raw";

    private final BundledToolProvisioner toolProvisioner;

    public ArtifactSet(BundledToolProvisioner toolProvisioner) {
        this.toolProvisioner = toolProvisioner;
    }

    /**
     * Resolves the candidate paths for each artifact type. Types with no
     * applicable path on this host (e.g. no OS registration) are omitted and are
     * reported as {@link RemovalStatus#NOT_PRESENT} by the caller.
     */
    public Map<ArtifactType, Path> resolve() {
        Map<ArtifactType, Path> paths = new LinkedHashMap<>();
        paths.put(ArtifactType.AGENT_BINARY, resolveBinaryPath());
        paths.put(ArtifactType.AGENT_CONFIG, Path.of(INSTALL_DIR, CONFIG_NAME));
        paths.put(ArtifactType.AGENT_LOG, Path.of(INSTALL_DIR, LOG_NAME));
        paths.put(ArtifactType.RAW_DOWNLOAD, Path.of(INSTALL_DIR, RAW_DOWNLOAD_NAME));
        Path toolsDir = toolProvisioner != null ? toolProvisioner.getExtractionDirectory() : null;
        if (toolsDir != null) {
            paths.put(ArtifactType.DOWNLOADED_TOOLS, toolsDir);
        }
        return paths;
    }

    /**
     * The running binary path, discovered from the current process, falling back
     * to the known install location.
     */
    public Path resolveBinaryPath() {
        return ProcessHandle.current().info().command()
                .map(Path::of)
                .orElseGet(() -> Path.of(INSTALL_DIR, BINARY_NAME));
    }

    /**
     * Removes a single path (file or directory tree), best-effort and idempotent.
     * A missing path yields {@link RemovalStatus#NOT_PRESENT}; a removal error
     * yields {@link RemovalStatus#FAILED} without throwing.
     */
    public RemovalStatus remove(Path path) {
        if (path == null || !Files.exists(path)) {
            return RemovalStatus.NOT_PRESENT;
        }
        try {
            if (Files.isDirectory(path)) {
                deleteRecursively(path);
            } else {
                Files.delete(path);
            }
            return RemovalStatus.REMOVED;
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to remove artifact {}: {}", path, e.getMessage());
            return RemovalStatus.FAILED;
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
