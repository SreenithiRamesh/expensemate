package com.expensemate.security;

import com.expensemate.exception.ApiProblemFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ApiProblemFactory problemFactory;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            ApiProblemFactory problemFactory
    ) {
        this.jwtService =
                jwtService;

        this.userDetailsService =
                userDetailsService;

        this.problemFactory =
                problemFactory;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader(
                        "Authorization"
                );

        if (authHeader == null
                || !authHeader.startsWith(
                "Bearer "
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader.substring(7);

        if (token.isBlank()) {

            writeInvalidTokenProblem(
                    request,
                    response
            );

            return;
        }

        try {
            String email =
                    jwtService.extractEmail(
                            token
                    );

            if (email != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        email
                                );

                if (jwtService.isTokenValid(
                        token,
                        userDetails.getUsername()
                )) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(
                                            request
                                    )
                    );

                    SecurityContext securityContext =
                            SecurityContextHolder
                                    .createEmptyContext();

                    securityContext.setAuthentication(
                            authentication
                    );

                    SecurityContextHolder.setContext(
                            securityContext
                    );

                } else {

                    SecurityContextHolder.clearContext();

                    writeInvalidTokenProblem(
                            request,
                            response
                    );

                    return;
                }
            }

        } catch (Exception exception) {

            SecurityContextHolder.clearContext();

            writeInvalidTokenProblem(
                    request,
                    response
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private void writeInvalidTokenProblem(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        problemFactory.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "invalid-access-token",
                "Invalid access token",
                "The access token is invalid or has expired",
                request
        );
    }
}