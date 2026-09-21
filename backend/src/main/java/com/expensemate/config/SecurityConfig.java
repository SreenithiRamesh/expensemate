package com.expensemate.config;

import com.expensemate.security.ApiRateLimitFilter;
import com.expensemate.security.JwtAuthenticationFilter;
import com.expensemate.security.RestAccessDeniedHandler;
import com.expensemate.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiRateLimitFilter apiRateLimitFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.apiRateLimitFilter =
                apiRateLimitFilter;

        this.authenticationEntryPoint =
                authenticationEntryPoint;

        this.accessDeniedHandler =
                accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * ExpenseMate is a stateless REST API using
                 * bearer tokens rather than browser sessions.
                 */
                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                /*
                 * Spring Security already provides:
                 *
                 * X-Content-Type-Options: nosniff
                 * X-Frame-Options: DENY
                 * Cache-Control
                 *
                 * These additional policies restrict referrer
                 * information and unused browser capabilities.
                 */
                .headers(headers -> headers
                        .referrerPolicy(referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter
                                                .ReferrerPolicy
                                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                                )
                        )
                        .permissionsPolicy(permissions ->
                                permissions.policy(
                                        "camera=(), "
                                                + "microphone=(), "
                                                + "geolocation=()"
                                )
                        )
                )

                /*
                 * No server-side HTTP session is maintained.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * M28 — RFC 7807 security errors.
                 *
                 * Missing or invalid authentication produces
                 * a standardized HTTP 401 ProblemDetail.
                 *
                 * Authenticated requests without permission
                 * produce a standardized HTTP 403 ProblemDetail.
                 */
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                /*
                 * Public endpoints.
                 *
                 * All other application endpoints require a
                 * valid authenticated user.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/**",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        )
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                /*
                 * Rate limiting runs before JWT processing so
                 * abusive requests can be rejected before any
                 * authentication work is performed.
                 */
                .addFilterBefore(
                        apiRateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                 * JWT authentication runs after rate limiting.
                 */
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        ApiRateLimitFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        List<String> origins =
                Arrays.stream(
                                allowedOrigins.split(",")
                        )
                        .map(String::trim)
                        .filter(origin ->
                                !origin.isBlank()
                        )
                        .toList();

        configuration.setAllowedOrigins(
                origins
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Idempotency-Key",
                        "X-Correlation-ID"
                )
        );

        /*
         * Location is used for REST resource creation.
         *
         * Retry-After allows the frontend to identify when a
         * rate-limited request may be attempted again.
         *
         * X-Correlation-ID allows frontend and backend logs to
         * be connected during production troubleshooting.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Location",
                        "Retry-After",
                        "X-Correlation-ID"
                )
        );

        /*
         * ExpenseMate currently transports access tokens in
         * Authorization headers and refresh tokens in JSON,
         * not cross-origin credential cookies.
         */
        configuration.setAllowCredentials(
                false
        );

        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}