package com.expensemate.security;

import com.expensemate.exception.ApiProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ApiProblemFactory problemFactory;

    public RestAuthenticationEntryPoint(
            ApiProblemFactory problemFactory
    ) {
        this.problemFactory = problemFactory;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {

        problemFactory.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "authentication-required",
                "Authentication required",
                "Valid authentication is required to access this resource",
                request
        );
    }
}