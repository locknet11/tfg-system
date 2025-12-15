package com.spulido.tfg.domain.script.services;

import com.spulido.tfg.domain.target.model.OperatingSystem;

public interface ScriptService {
    
    /**
     * Generates an installation script for the given target parameters.
     * 
     * @param os The target operating system
     * @param apiUrl The API base URL
     * @param organizationIdentifier The organization identifier
     * @param projectIdentifier The project identifier
     * @param targetUniqueId The unique ID of the target
     * @return The generated script content
     */
    String generateInstallScript(
            OperatingSystem os,
            String apiUrl,
            String organizationIdentifier,
            String projectIdentifier,
            String targetUniqueId,
            String preauthCode);
}
