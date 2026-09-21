package com.expensemate.security;

import com.expensemate.exception.ApiProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ApiProblemFactory problemFactory;

    public RestAccessDeniedHandler(
            ApiProblemFactory problemFactory
    ) {
        this.problemFactory = problemFactory;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        problemFactory.write(
                response,
                HttpStatus.FORBIDDEN,
                "access-denied",
                "Access denied",
                "You do not have permission to access this resource",
                request
        );
    }
}