package com.spulido.agent.worker.step;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RemediationStepHandlerOsDetectionTest {

    @Test
    void parseDistro_ubuntu2204() {
        List<String> lines = List.of(
                "PRETTY_NAME=\"Ubuntu 22.04.5 LTS\"",
                "NAME=\"Ubuntu\"",
                "VERSION_ID=\"22.04\"",
                "ID=ubuntu",
                "ID_LIKE=debian");
        assertThat(RemediationStepHandler.parseDistro(lines)).isEqualTo("ubuntu-22.04");
    }

    @Test
    void parseDistro_debian12() {
        List<String> lines = List.of(
                "PRETTY_NAME=\"Debian GNU/Linux 12 (bookworm)\"",
                "NAME=\"Debian GNU/Linux\"",
                "VERSION_ID=\"12\"",
                "ID=debian");
        assertThat(RemediationStepHandler.parseDistro(lines)).isEqualTo("debian-12");
    }

    @Test
    void parseDistro_lowercasesIdAndStripsQuotes() {
        List<String> lines = List.of("ID=\"Ubuntu\"", "VERSION_ID=\"20.04\"");
        assertThat(RemediationStepHandler.parseDistro(lines)).isEqualTo("ubuntu-20.04");
    }

    @Test
    void parseDistro_returnsNull_whenFieldsMissing() {
        assertThat(RemediationStepHandler.parseDistro(List.of("NAME=\"Something\""))).isNull();
        assertThat(RemediationStepHandler.parseDistro(List.of("ID=ubuntu"))).isNull();
    }
}
