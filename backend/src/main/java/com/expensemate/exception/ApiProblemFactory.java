package com.expensemate.exception;

import com.expensemate.observability.CorrelationIdConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Component
public class ApiProblemFactory {

    private static final String PROBLEM_TYPE_BASE =
            "https://api.expensemate.com/problems/";

    private final ObjectMapper objectMapper;

    public ApiProblemFactory(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public ProblemDetail create(
            HttpStatus status,
            String problemType,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        return create(
                status,
                problemType,
                title,
                detail,
                request,
                null
        );
    }

    public ProblemDetail create(
            HttpStatus status,
            String problemType,
            String title,
            String detail,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        detail
                );

        problem.setType(
                URI.create(
                        PROBLEM_TYPE_BASE
                                + problemType
                )
        );

        problem.setTitle(title);

        String requestUri =
                request.getRequestURI();

        if (requestUri != null
                && !requestUri.isBlank()) {

            problem.setInstance(
                    URI.create(requestUri)
            );
        }

        problem.setProperty(
                "timestamp",
                Instant.now()
        );

        String correlationId =
                MDC.get(
                        CorrelationIdConstants.MDC_KEY
                );

        if (correlationId != null
                && !correlationId.isBlank()) {

            problem.setProperty(
                    "correlationId",
                    correlationId
            );
        }

        if (validationErrors != null
                && !validationErrors.isEmpty()) {

            problem.setProperty(
                    "validationErrors",
                    validationErrors
            );
        }

        return problem;
    }

    public void write(
            HttpServletResponse response,
            ProblemDetail problem
    ) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.resetBuffer();

        response.setStatus(
                problem.getStatus()
        );

        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        Map<String, Object> properties =
                problem.getProperties();

        Object correlationId =
                properties == null
                        ? null
                        : properties.get(
                        "correlationId"
                );

        if (correlationId != null) {
            response.setHeader(
                    CorrelationIdConstants.HEADER_NAME,
                    correlationId.toString()
            );
        }

        objectMapper.writeValue(
                response.getOutputStream(),
                problem
        );

        response.flushBuffer();
    }

    public void write(
            HttpServletResponse response,
            HttpStatus status,
            String problemType,
            String title,
            String detail,
            HttpServletRequest request
    ) throws IOException {
        ProblemDetail problem =
                create(
                        status,
                        problemType,
                        title,
                        detail,
                        request
                );

        write(
                response,
                problem
        );
    }
}