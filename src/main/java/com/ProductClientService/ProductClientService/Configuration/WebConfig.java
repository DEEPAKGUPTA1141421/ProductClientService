package com.ProductClientService.ProductClientService.Configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ProductClientService.ProductClientService.Service.JwtService;
import com.ProductClientService.ProductClientService.filter.ClientCredentialFilter;
import com.ProductClientService.ProductClientService.filter.InternalApiKeyFilter;
import com.ProductClientService.ProductClientService.filter.JwtAuthenticationFilter;
import com.ProductClientService.ProductClientService.filter.RestAccessDeniedHandler;
import com.ProductClientService.ProductClientService.filter.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebConfig {

        private final JwtService jwtService;
        private final InternalApiKeyFilter internalApiKeyFilter;
        private final ClientCredentialFilter clientCredentialFilter;
        private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
        private final RestAccessDeniedHandler restAccessDeniedHandler;

        // ✅ CORS Configuration (IMPORTANT)
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOriginPatterns(List.of("*"));
                config.setAllowedMethods(List.of("*"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                // Allow all standard HTTP methods
                config.setAllowedMethods(List.of(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS"));

                // Allow headers
                config.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept"));

                // If using JWT (Authorization header)
                config.setAllowCredentials(false);

                // Cache preflight response (optional)
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);

                return source;
        }

        // ✅ Security Configuration
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);

                http
                                // Wire the CorsConfigurationSource bean explicitly — relying on
                                // Spring Security to auto-discover it via an empty cors(cors -> {})
                                // customizer left preflight OPTIONS requests falling through to
                                // Spring MVC's own (unconfigured) CORS check, which rejects them
                                // with a plain "Invalid CORS request" 403 and no CORS headers —
                                // that's what browsers report as a CORS error.
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Return JSON (no WWW-Authenticate: Basic challenge) on 401/403 —
                                // this app never uses HTTP Basic auth, and the default entry
                                // point's empty body + Basic challenge breaks clients that expect
                                // a normal ApiResponse and can cause browsers to intercept the
                                // response as a credentials prompt instead of a plain 401.
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(restAuthenticationEntryPoint)
                                                .accessDeniedHandler(restAccessDeniedHandler))

                                .authorizeHttpRequests(auth -> auth

                                                // ✅ Allow preflight requests
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // ✅ Internal service endpoints (secured by InternalApiKeyFilter)
                                                .requestMatchers("/internal/**").permitAll()

                                                // ✅ Public APIs
                                                .requestMatchers(
                                                                "/api/v1/auth/**",
                                                                "/public/**",
                                                                "/api/v1/search/**",
                                                                "/",
                                                                "/api/v1/seller/product/test",
                                                                "/api/v1/product/categorylevelwise/**",
                                                                "/api/v1/product/category",
                                                                "api/v1/wishlist/token/**",
                                                                "/api/v1/product/search",
                                                                "/api/v1/admin/auth/login",
                                                                "/api/v1/brands/category/**")
                                                .permitAll()

                                                // 🔒 Everything else under /api/v1/admin/** requires an ADMIN JWT.
                                                // (Login above is the one deliberately public sub-path.)
                                                // AdminKycController/AdminSellerController already enforce this
                                                // via @PreAuthorize("hasRole('ADMIN')") too — this matcher closes
                                                // the gap for controllers that had no role check at all, and
                                                // removes the accidental permitAll() that used to cover
                                                // /api/v1/admin/sellers/**.
                                                .requestMatchers("/api/v1/admin/**")
                                                .hasRole("ADMIN")

                                                // ✅ Review reads are public; writes require auth (handled below)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**")
                                                .permitAll()

                                                // ✅ Product detail — public read
                                                .requestMatchers(HttpMethod.GET, "/api/v1/product/**")
                                                .permitAll()

                                                // ✅ Sections page — public read
                                                .requestMatchers(HttpMethod.GET, "/api/v1/sections/**")
                                                .permitAll()

                                                // ✅ Shops — all GET endpoints are public (listing, search, detail, storefront)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/shops/**")
                                                .permitAll()

                                                // ✅ Interaction tracking — accept guests (userId attached if JWT
                                                // present)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/track/**")
                                                .permitAll()

                                                // ✅ Category filters — GET is public, DELETE (cache evict) requires
                                                // auth
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/*/filters")
                                                .permitAll()

                                                // 🔒 Secure everything else
                                                .anyRequest().authenticated())

                                // ✅ Internal API key filter (before JWT — handles /internal/** only)
                                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                                // ✅ Client credential filter (interservice auth — handles /internal/v1/cart/** only)
                                .addFilterBefore(clientCredentialFilter, UsernamePasswordAuthenticationFilter.class)
                                // ✅ JWT Filter
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // ✅ RestTemplate Bean
        @Bean
        public RestTemplate restTemplate() {
                return new RestTemplate();
        }
}
// juoiiojnji jioji mmjio uiouoinjjjknjknjnjnjkjjnkjnk
// jliio uiu8u88uuiiiu8iuinbhui hukjijijijijjn