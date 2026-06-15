package com.spulido.tfg.config.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.organization.db.OrganizationRepository;
import com.spulido.tfg.domain.project.db.ProjectRepository;
import com.spulido.tfg.domain.project.model.Project;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that extracts organization and project IDs from HTTP headers,
 * validates their existence and relationship, and stores them in ProjectContext
 * for the duration of the request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectContextFilter extends OncePerRequestFilter {

    private static final String ORGANIZATION_HEADER = "X-Organization-Id";
    private static final String PROJECT_HEADER = "X-Project-Id";

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String organizationId = request.getHeader(ORGANIZATION_HEADER);
            String projectId = request.getHeader(PROJECT_HEADER);

            if (organizationId == null && projectId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (organizationId == null || projectId == null) {
                log.warn("Missing organization or project header. Organization: {}, Project: {}",
                        organizationId, projectId);
                writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                        ProjectContextErrorReason.MISSING_CONTEXT_HEADER,
                        "Both organization and project headers are required when either is provided.");
                return;
            }

            if (!organizationRepository.existsById(organizationId)) {
                log.warn("Organization not found: {}", organizationId);
                writeErrorResponse(response, HttpStatus.NOT_FOUND,
                        ProjectContextErrorReason.ORGANIZATION_NOT_FOUND,
                        "Organization not found: " + organizationId);
                return;
            }

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                log.warn("Project not found: {}", projectId);
                writeErrorResponse(response, HttpStatus.NOT_FOUND,
                        ProjectContextErrorReason.PROJECT_NOT_FOUND,
                        "Project not found: " + projectId);
                return;
            }

            if (!project.getOrganizationId().equals(organizationId)) {
                log.warn("Project {} does not belong to organization {}", projectId, organizationId);
                writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                        ProjectContextErrorReason.PROJECT_ORGANIZATION_MISMATCH,
                        "Project does not belong to the provided organization.");
                return;
            }

            ProjectContext.set(organizationId, projectId);
            filterChain.doFilter(request, response);

        } finally {
            ProjectContext.clear();
        }
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            ProjectContextErrorReason reason,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("errorCode", "INVALID_PROJECT_CONTEXT");
        body.put("reason", reason.name());
        body.put("message", message);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private enum ProjectContextErrorReason {
        MISSING_CONTEXT_HEADER,
        ORGANIZATION_NOT_FOUND,
        PROJECT_NOT_FOUND,
        PROJECT_ORGANIZATION_MISMATCH
    }
}
