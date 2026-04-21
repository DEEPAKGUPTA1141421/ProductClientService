package com.ProductClientService.ProductClientService.Configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ProductClientService.ProductClientService.Service.JwtService;
import com.ProductClientService.ProductClientService.filter.InternalApiKeyFilter;
import com.ProductClientService.ProductClientService.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

        private final JwtService jwtService;
        private final InternalApiKeyFilter internalApiKeyFilter;

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
                                .cors(cors -> {
                                }) // ✅ MUST ENABLE CORS HERE
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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
                                                                "/api/v1/brands/category/**")
                                                .permitAll()

                                                // ✅ Review reads are public; writes require auth (handled below)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**")
                                                .permitAll()

                                                // ✅ Product detail — public read
                                                .requestMatchers(HttpMethod.GET, "/api/v1/product/**")
                                                .permitAll()

                                                // ✅ Category filters — GET is public, DELETE (cache evict) requires
                                                // auth
                                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/*/filters")
                                                .permitAll()

                                                // 🔒 Secure everything else
                                                .anyRequest().authenticated())

                                // ✅ Internal API key filter (before JWT — handles /internal/** only)
                                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
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
// jliio uiu8u88uuiiiu8iui