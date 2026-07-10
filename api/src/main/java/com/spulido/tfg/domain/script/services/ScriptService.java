package com.spulido.tfg.domain.script.services;

import com.spulido.tfg.domain.target.model.OperatingSystem;

public interface ScriptService {

    /**
     * Generates an installation script for the given target parameters.
     * 
     * @param os                     The target operating system
     * @param apiUrl                 The API base URL
     * @param organizationIdentifier The organization identifier
     * @param projectIdentifier      The project identifier
     * @param targetUniqueId         The unique ID of the target
     * @param preauthCode            The preauth code for the target
     * @param downloadUrl            The URL to download the agent binary
     * @param agentId                The registered agent's ID
     * @param apiKey                 The agent's API key for authentication
     * @param centralPublicKey       The central platform's RSA public key for signature verification
     * @return The generated script content
     */
    String generateInstallScript(
            OperatingSystem os,
            String apiUrl,
            String organizationIdentifier,
            String projectIdentifier,
            String targetUniqueId,
            String preauthCode,
            String downloadUrl,
            String agentId,
            String apiKey,
            String centralPublicKey);

    String generateInstallErrorScript(
            OperatingSystem os,
            String targetUniqueId,
            String errorMessage);

    String generateExploitScript(
            String description,
            String targetIp,
            int targetPort,
            String serviceName,
            String serviceVersion,
            String cveId,
            String exploitUrl);
}
