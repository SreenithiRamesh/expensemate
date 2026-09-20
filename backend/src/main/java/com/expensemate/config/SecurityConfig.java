package com.expensemate.config;

import com.expensemate.security.ApiRateLimitFilter;
import com.expensemate.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ApiRateLimitFilter apiRateLimitFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiRateLimitFilter = apiRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                /*
                 * Spring Security already provides important
                 * default response headers such as:
                 *
                 * X-Content-Type-Options: nosniff
                 * X-Frame-Options: DENY
                 * Cache-Control
                 *
                 * M21 adds only the additional policies that
                 * ExpenseMate currently needs.
                 */
                .headers(headers -> headers

                        /*
                         * Prevents the browser from sending the
                         * complete URL as referrer information
                         * when navigating cross-origin.
                         */
                        .referrerPolicy(referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter
                                                .ReferrerPolicy
                                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                                )
                        )

                        /*
                         * ExpenseMate does not currently need
                         * browser camera, microphone or
                         * geolocation capabilities.
                         */
                        .permissionsPolicy(permissions ->
                                permissions.policy(
                                        "camera=(), microphone=(), geolocation=()"
                                )
                        )
                )

                /*
                 * REST API authentication is JWT based.
                 * No server-side HTTP session is required.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpStatus.UNAUTHORIZED.value(),
                                                "Unauthorized"
                                        )
                        )
                )

                /*
                 * Public endpoints.
                 *
                 * All remaining application endpoints require
                 * authentication.
                 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/**",
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
                 * Rate limiting executes before JWT
                 * authentication so abusive traffic can be
                 * rejected before authentication work occurs.
                 */
                .addFilterBefore(
                        apiRateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                 * JWT authentication executes after our
                 * rate-limit filter.
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
                        "Idempotency-Key"
                )
        );

        /*
         * Location is used by REST resource creation.
         *
         * Retry-After is exposed so the React frontend
         * can read it after HTTP 429 responses.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Location",
                        "Retry-After"
                )
        );

        /*
         * Current authentication uses Authorization headers
         * and JSON refresh tokens rather than cross-origin
         * credential cookies.
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