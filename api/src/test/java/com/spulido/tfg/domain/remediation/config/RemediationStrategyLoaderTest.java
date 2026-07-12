package com.spulido.tfg.domain.remediation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import com.spulido.tfg.domain.remediation.db.RemediationStrategyRepository;
import com.spulido.tfg.domain.remediation.model.RemediationAction;
import com.spulido.tfg.domain.remediation.model.RemediationStrategy;
import com.spulido.tfg.domain.remediation.model.RemediationType;

@ExtendWith(MockitoExtension.class)
class RemediationStrategyLoaderTest {

    @Mock
    private RemediationStrategyRepository repository;

    private RemediationStrategyLoader loader;

    @BeforeEach
    void setUp() {
        loader = new RemediationStrategyLoader(repository, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("Should reject strategy with invalid CVE ID format")
    void shouldRejectInvalidCveId() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("INVALID-FORMAT")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of("cmd"))
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("Invalid or missing cveId");
    }

    @Test
    @DisplayName("Should reject strategy with missing operatingSystem")
    void shouldRejectMissingOs() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of("cmd"))
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("operatingSystem");
    }

    @Test
    @DisplayName("Should reject strategy with missing packageName")
    void shouldRejectMissingPackageName() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName(null)
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of("cmd"))
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("packageName");
    }

    @Test
    @DisplayName("Should reject strategy with missing targetVersion for non-MANUAL action")
    void shouldRejectMissingTargetVersion() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion(null)
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of("cmd"))
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("targetVersion");
    }

    @Test
    @DisplayName("Should accept strategy with missing targetVersion for MANUAL action")
    void shouldAcceptMissingTargetVersionForManual() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.KERNEL_UPDATE)
                .action(RemediationAction.MANUAL)
                .targetVersion(null)
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of())
                .postCheckCommands(List.of("cmd"))
                .notes("Manual intervention required")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNull();
    }

    @Test
    @DisplayName("Should reject strategy with missing notes")
    void shouldRejectMissingNotes() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of("cmd"))
                .postCheckCommands(List.of("cmd"))
                .notes("")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("notes");
    }

    @Test
    @DisplayName("Should accept valid strategy entry")
    void shouldAcceptValidStrategy() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1:9.3p1-1ubuntu3.2")
                .preCheckCommands(List.of("dpkg -l openssh-server"))
                .fixCommands(List.of("apt-get update", "apt-get install -y openssh-server"))
                .postCheckCommands(List.of("dpkg -l openssh-server"))
                .serviceName("ssh")
                .requiresReboot(false)
                .notes("Remote code execution vulnerability in OpenSSH.")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNull();
    }

    @Test
    @DisplayName("Should reject strategy with empty fixCommands for non-MANUAL action")
    void shouldRejectEmptyFixCommands() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(List.of())
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("fixCommands");
    }

    @Test
    @DisplayName("Should reject strategy with null fixCommands for non-MANUAL action")
    void shouldRejectNullFixCommands() {
        RemediationStrategy strategy = RemediationStrategy.builder()
                .cveId("CVE-2023-12345")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1.0")
                .preCheckCommands(List.of("cmd"))
                .fixCommands(null)
                .postCheckCommands(List.of("cmd"))
                .notes("Test notes")
                .build();

        String error = loader.validateStrategy(strategy, 0);
        assertThat(error).isNotNull();
        assertThat(error).contains("fixCommands");
    }

    @Test
    @DisplayName("Every entry in strategies.json is valid and unique, so all of them are seeded")
    void shouldSeedEveryEntryOfTheShippedCatalog() throws Exception {
        when(repository.existsByCveIdAndOperatingSystem(any(), any())).thenReturn(false);

        loader.run(null);

        ArgumentCaptor<RemediationStrategy> captor = ArgumentCaptor.forClass(RemediationStrategy.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        List<RemediationStrategy> seeded = captor.getAllValues();

        // A CVE + OS pair repeated in the file would be dropped by the loader and never reach
        // the database, since cve_os_idx is unique. Nothing may be silently lost.
        int entriesInFile = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(new ClassPathResource("remediation/strategies.json").getInputStream(),
                        RemediationStrategy[].class)
                .length;

        assertThat(seeded).hasSize(entriesInFile);
        assertThat(seeded)
                .extracting(s -> s.getCveId() + "@" + s.getOperatingSystem())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Should skip existing strategies and only add new ones")
    void shouldSkipExistingStrategies() throws Exception {
        RemediationStrategy existing = RemediationStrategy.builder()
                .cveId("CVE-2023-38408")
                .operatingSystem("ubuntu-22.04")
                .packageName("openssh-server")
                .remediationType(RemediationType.SERVICE_UPDATE)
                .action(RemediationAction.APT_UPGRADE)
                .targetVersion("1:9.3p1-1ubuntu3.2")
                .preCheckCommands(List.of("dpkg -l openssh-server"))
                .fixCommands(List.of("apt-get update", "apt-get install -y openssh-server"))
                .postCheckCommands(List.of("dpkg -l openssh-server"))
                .notes("Test notes")
                .build();

        when(repository.existsByCveIdAndOperatingSystem("CVE-2023-38408", "ubuntu-22.04"))
                .thenReturn(true);

        // Verify existing strategy would be recognized as duplicate
        assertThat(repository.existsByCveIdAndOperatingSystem("CVE-2023-38408", "ubuntu-22.04")).isTrue();
        verify(repository, never()).save(existing);
    }
}
