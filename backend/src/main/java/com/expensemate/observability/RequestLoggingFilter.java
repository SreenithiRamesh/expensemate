package com.expensemate.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RequestLoggingFilter.class
            );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startedAt =
                System.nanoTime();

        try {

            filterChain.doFilter(
                    request,
                    response
            );

        } finally {

            long durationNanos =
                    System.nanoTime()
                            - startedAt;

            long durationMillis =
                    TimeUnit.NANOSECONDS
                            .toMillis(durationNanos);

            /*
             * Intentionally log only operational metadata.
             *
             * Never log request/response bodies,
             * authorization headers, cookies, tokens,
             * idempotency keys or query strings.
             */
            log.info(
                    "request_completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMillis
            );
        }
    }
}