package com.spulido.tfg.domain;

/**
 * Marker interface for entities that belong to an organization and project scope.
 * Entities implementing this interface will have their organizationId and projectId
 * automatically populated from ProjectContext when saved to the database.
 * 
 * Note: This interface uses default methods to be compatible with Lombok's
 * @Accessors(chain = true) which makes setters return the entity type instead of void.
 */
public interface ScopedEntity {
    
    String getOrganizationId();
    
    String getProjectId();
    
    /**
     * Default implementation that casts to the implementing type.
     * Works with both void setters and fluent setters (returning this).
     */
    default void setOrganizationIdValue(String organizationId) {
        if (this instanceof com.spulido.tfg.domain.target.model.Target) {
            ((com.spulido.tfg.domain.target.model.Target) this).setOrganizationId(organizationId);
        } else if (this instanceof com.spulido.tfg.domain.agent.model.Agent) {
            ((com.spulido.tfg.domain.agent.model.Agent) this).setOrganizationId(organizationId);
        }
    }
    
    /**
     * Default implementation that casts to the implementing type.
     * Works with both void setters and fluent setters (returning this).
     */
    default void setProjectIdValue(String projectId) {
        if (this instanceof com.spulido.tfg.domain.target.model.Target) {
            ((com.spulido.tfg.domain.target.model.Target) this).setProjectId(projectId);
        } else if (this instanceof com.spulido.tfg.domain.agent.model.Agent) {
            ((com.spulido.tfg.domain.agent.model.Agent) this).setProjectId(projectId);
        }
    }
}
