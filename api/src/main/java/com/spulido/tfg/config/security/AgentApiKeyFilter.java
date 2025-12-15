package com.spulido.tfg.config.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that authenticates agents using their API key.
 * The API key should be sent in the X-Agent-Api-Key header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Agent-Api-Key";
    private static final String AGENT_ID_HEADER = "X-Agent-Id";

    private final AgentRepository agentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only process if no authentication is already set
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            String agentId = request.getHeader(AGENT_ID_HEADER);

            if (apiKey != null && !apiKey.isEmpty() && agentId != null && !agentId.isEmpty()) {
                try {
                    Optional<Agent> agentOpt = agentRepository.findByApiKey(apiKey);

                    if (agentOpt.isPresent()) {
                        Agent agent = agentOpt.get();

                        // Verify the agent ID matches
                        if (agent.getId().equals(agentId)) {
                            // Create authentication token with AGENT role
                            AgentAuthenticationToken authToken = new AgentAuthenticationToken(
                                    agent,
                                    apiKey,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_AGENT")));

                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            // Set project context for scoped queries
                            ProjectContext.set(agent.getOrganizationId(), agent.getProjectId());

                            log.debug("Agent authenticated: {}", agent.getName());
                        } else {
                            log.warn("Agent ID mismatch for API key");
                        }
                    } else {
                        log.warn("Invalid API key provided");
                    }
                } catch (Exception e) {
                    log.error("Error authenticating agent", e);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only apply to agent-specific endpoints
        return !path.startsWith("/api/agent/");
    }

    /**
     * Custom authentication token for agents.
     */
    public static class AgentAuthenticationToken extends UsernamePasswordAuthenticationToken {

        private final Agent agent;

        public AgentAuthenticationToken(Agent agent, String credentials,
                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
            super(agent.getId(), credentials, authorities);
            this.agent = agent;
        }

        public Agent getAgent() {
            return agent;
        }
    }
}
