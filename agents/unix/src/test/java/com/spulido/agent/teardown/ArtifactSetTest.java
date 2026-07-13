package com.spulido.agent.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.worker.tools.BundledToolProvisioner;

@ExtendWith(MockitoExtension.class)
class ArtifactSetTest {

    @Mock
    private BundledToolProvisioner toolProvisioner;

    @Test
    void resolve_includesBinaryConfigLogAndToolsDir(@TempDir Path toolsDir) {
        when(toolProvisioner.getExtractionDirectory()).thenReturn(toolsDir);
        ArtifactSet artifactSet = new ArtifactSet(toolProvisioner);

        var paths = artifactSet.resolve();

        assertThat(paths).containsKey(ArtifactType.AGENT_BINARY);
        assertThat(paths.get(ArtifactType.AGENT_CONFIG))
                .isEqualTo(Path.of(ArtifactSet.INSTALL_DIR, ArtifactSet.CONFIG_NAME));
        assertThat(paths.get(ArtifactType.AGENT_LOG))
                .isEqualTo(Path.of(ArtifactSet.INSTALL_DIR, ArtifactSet.LOG_NAME));
        assertThat(paths.get(ArtifactType.DOWNLOADED_TOOLS)).isEqualTo(toolsDir);
    }

    @Test
    void remove_missingPath_isNotPresent() {
        ArtifactSet artifactSet = new ArtifactSet(toolProvisioner);
        assertThat(artifactSet.remove(Path.of("/nonexistent/agent-xyz-123")))
                .isEqualTo(RemovalStatus.NOT_PRESENT);
        assertThat(artifactSet.remove(null)).isEqualTo(RemovalStatus.NOT_PRESENT);
    }

    @Test
    void remove_existingFile_isRemoved(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("agent.properties");
        Files.writeString(file, "agent.api-key=secret");
        ArtifactSet artifactSet = new ArtifactSet(toolProvisioner);

        assertThat(artifactSet.remove(file)).isEqualTo(RemovalStatus.REMOVED);
        assertThat(Files.exists(file)).isFalse();
        // Idempotent: a second removal is a no-op.
        assertThat(artifactSet.remove(file)).isEqualTo(RemovalStatus.NOT_PRESENT);
    }

    @Test
    void remove_directoryTree_isRemovedRecursively(@TempDir Path dir) throws Exception {
        Path toolsDir = dir.resolve("agent-tools");
        Files.createDirectories(toolsDir);
        Files.writeString(toolsDir.resolve("nmap"), "binary");
        Files.writeString(toolsDir.resolve("rustscan"), "binary");
        ArtifactSet artifactSet = new ArtifactSet(toolProvisioner);

        assertThat(artifactSet.remove(toolsDir)).isEqualTo(RemovalStatus.REMOVED);
        assertThat(Files.exists(toolsDir)).isFalse();
    }
}
