package com.spulido.tfg.domain;

/**
 * Marker interface for entities that belong to an organization and project
 * scope.
 * Entities implementing this interface will have their organizationId and
 * projectId
 * automatically populated from ProjectContext when saved to the database.
 */
public interface ScopedEntity {

    String getOrganizationId();

    String getProjectId();

    default void setOrganizationIdValue(String organizationId) {
        if (this instanceof com.spulido.tfg.domain.target.model.Target) {
            ((com.spulido.tfg.domain.target.model.Target) this).setOrganizationId(organizationId);
        } else if (this instanceof com.spulido.tfg.domain.agent.model.Agent) {
            ((com.spulido.tfg.domain.agent.model.Agent) this).setOrganizationId(organizationId);
        } else if (this instanceof com.spulido.tfg.domain.template.model.Template) {
            ((com.spulido.tfg.domain.template.model.Template) this).setOrganizationId(organizationId);
        }
    }

    default void setProjectIdValue(String projectId) {
        if (this instanceof com.spulido.tfg.domain.target.model.Target) {
            ((com.spulido.tfg.domain.target.model.Target) this).setProjectId(projectId);
        } else if (this instanceof com.spulido.tfg.domain.agent.model.Agent) {
            ((com.spulido.tfg.domain.agent.model.Agent) this).setProjectId(projectId);
        } else if (this instanceof com.spulido.tfg.domain.template.model.Template) {
            ((com.spulido.tfg.domain.template.model.Template) this).setProjectId(projectId);
        }
    }
}
