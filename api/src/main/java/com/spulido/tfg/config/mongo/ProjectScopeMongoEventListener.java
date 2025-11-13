package com.spulido.tfg.config.mongo;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.ScopedEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * MongoDB event listener that automatically sets organizationId and projectId
 * on entities implementing ScopedEntity before they are saved to the database.
 * 
 * This listener only sets the IDs if:
 * 1. The entity implements ScopedEntity
 * 2. There is an active ProjectContext
 * 3. The entity's organizationId or projectId is null (doesn't override existing values)
 */
@Slf4j
@Component
public class ProjectScopeMongoEventListener extends AbstractMongoEventListener<Object> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        
        // Only process entities that implement ScopedEntity
        if (!(source instanceof ScopedEntity)) {
            return;
        }
        
        ScopedEntity scopedEntity = (ScopedEntity) source;
        
        // Only set IDs if there's an active context
        if (!ProjectContext.hasContext()) {
            return;
        }
        
        String contextOrgId = ProjectContext.getOrganizationId();
        String contextProjectId = ProjectContext.getProjectId();
        
        // Set organizationId from context if not already set
        if (scopedEntity.getOrganizationId() == null && contextOrgId != null) {
            scopedEntity.setOrganizationIdValue(contextOrgId);
            log.debug("Auto-set organizationId={} on entity {}", contextOrgId, source.getClass().getSimpleName());
        }
        
        // Set projectId from context if not already set
        if (scopedEntity.getProjectId() == null && contextProjectId != null) {
            scopedEntity.setProjectIdValue(contextProjectId);
            log.debug("Auto-set projectId={} on entity {}", contextProjectId, source.getClass().getSimpleName());
        }
    }
}
