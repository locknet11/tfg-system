package com.spulido.tfg.config.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.spulido.tfg.common.context.ProjectContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that extracts organization and project IDs from HTTP headers
 * and stores them in ProjectContext for the duration of the request.
 */
@Component
public class ProjectContextFilter extends OncePerRequestFilter {

    private static final String ORGANIZATION_HEADER = "X-Organization-Id";
    private static final String PROJECT_HEADER = "X-Project-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String organizationId = request.getHeader(ORGANIZATION_HEADER);
            String projectId = request.getHeader(PROJECT_HEADER);
            
            // Set context if headers are present
            if (organizationId != null && projectId != null) {
                ProjectContext.set(organizationId, projectId);
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear context to prevent memory leaks
            ProjectContext.clear();
        }
    }
}
