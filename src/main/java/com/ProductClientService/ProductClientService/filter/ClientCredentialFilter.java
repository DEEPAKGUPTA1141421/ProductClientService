package com.ProductClientService.ProductClientService.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Verifies caller identity for service-to-service endpoints that carry
 * sensitive user data (currently just the cart lookup used during checkout).
 *
 * Calling service must include, on top of X-Internal-Api-Key:
 *   X-Client-Id:     <value of internal.client.id>
 *   X-Client-Secret: <value of internal.client.secret>
 *
 * Requests without the header(s), or with a wrong value, are rejected
 * with 401 before they reach any controller. Runs only for
 * /internal/v1/cart/** — all other paths are passed through untouched.
 */
@Component
public class ClientCredentialFilter extends OncePerRequestFilter {

    public static final String CLIENT_ID_HEADER = "X-Client-Id";
    public static final String CLIENT_SECRET_HEADER = "X-Client-Secret";
    private static final String GUARDED_PATH_PREFIX = "/internal/v1/cart/";
    private static final Logger log = LoggerFactory.getLogger(ClientCredentialFilter.class);

    private final String internalClientId;
    private final String internalClientSecret;

    public ClientCredentialFilter(
            @Value("${internal.client.id}") String internalClientId,
            @Value("${internal.client.secret}") String internalClientSecret) {
        this.internalClientId = internalClientId;
        this.internalClientSecret = internalClientSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String clientId = request.getHeader(CLIENT_ID_HEADER);
        String clientSecret = request.getHeader(CLIENT_SECRET_HEADER);

        if (clientId == null || clientSecret == null
                || !clientId.equals(internalClientId) || !clientSecret.equals(internalClientSecret)) {
            log.warn("Rejected inter-service request from {} — missing or invalid client credentials",
                    request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(GUARDED_PATH_PREFIX);
    }
}
