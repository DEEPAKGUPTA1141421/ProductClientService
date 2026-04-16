package com.ProductClientService.ProductClientService.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards all /internal/** endpoints with a shared secret key.
 *
 * Calling service must include:
 *   X-Internal-Api-Key: <value of INTERNAL_API_KEY env var>
 *
 * Requests without the header, or with a wrong value, are rejected
 * with 401 before they reach any controller. This filter runs only
 * for /internal/** paths — all other paths are passed through untouched.
 */
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Api-Key";
    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String provided = request.getHeader(HEADER_NAME);

        if (provided == null || !provided.equals(internalApiKey)) {
            log.warn("Rejected internal request from {} — missing or invalid {}",
                    request.getRemoteAddr(), HEADER_NAME);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/internal/");
    }
}
