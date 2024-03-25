package com.project.app.config.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.app.common.JsonUtils;
import com.project.app.common.exception.ErrorCode;
import com.project.app.common.exception.ErrorDetails;
import com.project.app.common.exception.GenericErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        log.error(String.format("Token [%s] is not valid", request.getHeader(HttpHeaders.AUTHORIZATION)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(getErrorBody());
    }

    private static String getErrorBody() throws JsonProcessingException {
        ErrorDetails details = ErrorDetails.builder()
                .code(ErrorCode.UNAUTHORIZED)
                .detail(ErrorCode.UNAUTHORIZED.getDefaultMessage())
                .build();
        Map<String, Object> bodyMap = new GenericErrorResponse(details, null).mapOf();
        return JsonUtils.objectToJson(bodyMap);
    }
}
