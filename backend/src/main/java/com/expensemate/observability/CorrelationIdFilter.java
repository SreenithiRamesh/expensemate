package com.expensemate.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter
        extends OncePerRequestFilter {

    /*
     * Restrict client-supplied correlation IDs to a
     * safe format.
     *
     * Spaces, control characters and newline characters
     * are rejected to prevent log injection.
     */
    private static final Pattern SAFE_CORRELATION_ID =
            Pattern.compile(
                    "^[A-Za-z0-9_-]{1,"
                            + CorrelationIdConstants.MAX_LENGTH
                            + "}$"
            );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId =
                resolveCorrelationId(request);

        /*
         * Store the ID before invoking the remaining
         * application filters so authentication, rate
         * limiting, controllers and exception handlers
         * can include it in their logs.
         */
        MDC.put(
                CorrelationIdConstants.MDC_KEY,
                correlationId
        );

        /*
         * Return the ID to API consumers so frontend
         * errors can be matched with backend logs.
         */
        response.setHeader(
                CorrelationIdConstants.HEADER_NAME,
                correlationId
        );

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            /*
             * Servlet-container threads are reused.
             * Always remove request-specific MDC data.
             */
            MDC.remove(
                    CorrelationIdConstants.MDC_KEY
            );
        }
    }

    private String resolveCorrelationId(
            HttpServletRequest request
    ) {

        String suppliedCorrelationId =
                request.getHeader(
                        CorrelationIdConstants.HEADER_NAME
                );

        if (suppliedCorrelationId != null
                && SAFE_CORRELATION_ID
                .matcher(suppliedCorrelationId)
                .matches()) {

            return suppliedCorrelationId;
        }

        return UUID.randomUUID()
                .toString();
    }
}