package com.spulido.tfg.common.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thread-local context to hold organization and project IDs for the current request.
 * This allows automatic scoping of entities without passing IDs through every method.
 */
public class ProjectContext {
    
    private static final ThreadLocal<Context> contextHolder = new ThreadLocal<>();
    
    /**
     * Sets the organization and project context for the current thread.
     */
    public static void set(String organizationId, String projectId) {
        contextHolder.set(new Context(organizationId, projectId));
    }
    
    /**
     * Gets the organization ID from the current context.
     */
    public static String getOrganizationId() {
        Context context = contextHolder.get();
        return context != null ? context.getOrganizationId() : null;
    }
    
    /**
     * Gets the project ID from the current context.
     */
    public static String getProjectId() {
        Context context = contextHolder.get();
        return context != null ? context.getProjectId() : null;
    }
    
    /**
     * Clears the context for the current thread.
     * Should be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        contextHolder.remove();
    }
    
    /**
     * Checks if there is an active context.
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Context {
        private String organizationId;
        private String projectId;
    }
}
