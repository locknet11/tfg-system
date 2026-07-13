package com.spulido.agent.teardown;

/**
 * Categories of on-host artifact that teardown removes. Only artifacts the
 * agent or its installer created are ever enumerated.
 */
public enum ArtifactType {
    AGENT_BINARY,
    AGENT_CONFIG,
    AGENT_LOG,
    DOWNLOADED_TOOLS,
    WORKING_DIR,
    INSTALL_SCRIPT,
    OS_REGISTRATION,
    RAW_DOWNLOAD
}
