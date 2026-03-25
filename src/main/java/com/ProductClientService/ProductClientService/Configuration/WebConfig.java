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
import com.ProductClientService.ProductClientService.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig {

    private final JwtService jwtService;

    // ✅ CORS Configuration (IMPORTANT)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 🔥 Allow your frontend origins (update if needed)
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8000",
                "http://localhost:8080",
                "http://192.168.1.104:8080",
                "http://localhost:5173",
                "http://10.0.2.2:3000",
                "http://127.0.0.1:8080"));

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

                        // ✅ Public APIs
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/public/**",
                                "/api/v1/search/**",
                                "/api/v1/seller/product/test",
                                "/api/v1/product/categorylevelwise/**", "/api/v1/product/category",
                                "/api/v1/brands/category/**")
                        .permitAll()

                        // 🔒 Secure everything else
                        .anyRequest().authenticated())

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
// mlkklijkjiljijijijnnjjijihuhuhuhhuhuhugihhuihuikuhuigyjjijinjkj
// hujijhjjujijkj