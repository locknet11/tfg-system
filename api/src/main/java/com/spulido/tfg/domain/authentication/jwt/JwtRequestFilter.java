package com.spulido.tfg.domain.authentication.jwt;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsService userService;

	@AllArgsConstructor
	@Getter
	protected class AuthenticationDataHolder {
		private String username;
		private String jwt;

	}

	private AuthenticationDataHolder getAuthenticationData(HttpServletRequest request) {
		final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String jwt = null;
		String username = null;
		if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
			jwt = authorizationHeader;
			username = jwtUtil.extractUsername(jwt);
			return new AuthenticationDataHolder(username, jwt);
		}
		return null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		try {
			AuthenticationDataHolder auth = getAuthenticationData(request);
			if (auth != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userService.loadUserByUsername(auth.getUsername());
				if (jwtUtil.validateToken(auth.getJwt(), userDetails)) {
					UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(userDetails,
							userDetails.getPassword(), userDetails.getAuthorities());
					SecurityContextHolder.getContext().setAuthentication(authReq);
				}
			}
		} catch (Exception e) {
		}

		filterChain.doFilter(request, response);

	}
}
