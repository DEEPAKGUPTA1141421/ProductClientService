package com.ProductClientService.ProductClientService.filter;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.ProductClientService.ProductClientService.DTO.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Without this bean, Spring Security's default entry point kicks in for any
 * unauthenticated request to a secured route — it responds 401 with an empty
 * body and a "WWW-Authenticate: Basic" header, even though this app never
 * uses HTTP Basic auth. That header is what makes some HTTP clients (browsers
 * in particular) treat the 401 as a credentials challenge instead of handing
 * it back to application code, and it breaks every client's expectation of a
 * JSON ApiResponse body on every other endpoint.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Object> body = new ApiResponse<>(false, "Authentication required", null, 401);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
