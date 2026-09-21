package com.expensemate.security;

import com.expensemate.exception.ApiProblemFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApiRateLimitFilter
        extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS =
            60L;

    private static final long CLEANUP_INTERVAL_SECONDS =
            60L;

    private final Map<String, RateLimitWindow> authWindows =
            new ConcurrentHashMap<>();

    private final Map<String, RateLimitWindow> apiWindows =
            new ConcurrentHashMap<>();

    private final AtomicLong lastCleanupEpochSecond =
            new AtomicLong(0L);

    private final int authRequestsPerMinute;
    private final int apiRequestsPerMinute;
    private final ApiProblemFactory problemFactory;

    public ApiRateLimitFilter(
            @Value("${app.rate-limit.auth.requests-per-minute:10}")
            int authRequestsPerMinute,

            @Value("${app.rate-limit.api.requests-per-minute:120}")
            int apiRequestsPerMinute,

            ApiProblemFactory problemFactory
    ) {

        /*
         * Invalid security configuration must fail during
         * application startup rather than silently disabling
         * rate-limit protection.
         */
        if (authRequestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "Auth rate limit must be greater than zero"
            );
        }

        if (apiRequestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "API rate limit must be greater than zero"
            );
        }

        this.authRequestsPerMinute =
                authRequestsPerMinute;

        this.apiRequestsPerMinute =
                apiRequestsPerMinute;

        this.problemFactory =
                problemFactory;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path =
                request.getRequestURI();

        /*
         * CORS preflight requests must not consume normal
         * application rate-limit capacity.
         */
        if ("OPTIONS".equalsIgnoreCase(
                request.getMethod()
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        /*
         * Health and API documentation endpoints are excluded
         * from application rate limiting.
         */
        if (isExcludedPath(path)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        long now =
                Instant.now()
                        .getEpochSecond();

        cleanupExpiredWindowsIfNeeded(
                now
        );

        String clientKey =
                resolveClientKey(
                        request
                );

        RateLimitResult result =
                resolveRateLimitResult(
                        path,
                        clientKey,
                        now
                );

        if (result != null
                && !result.allowed()) {

            writeRateLimitResponse(
                    request,
                    response,
                    result.retryAfterSeconds()
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private RateLimitResult resolveRateLimitResult(
            String path,
            String clientKey,
            long now
    ) {

        if (path.startsWith(
                "/api/v1/auth/"
        )) {

            return consume(
                    authWindows,
                    clientKey,
                    authRequestsPerMinute,
                    now
            );
        }

        if (path.startsWith(
                "/api/v1/"
        )) {

            return consume(
                    apiWindows,
                    clientKey,
                    apiRequestsPerMinute,
                    now
            );
        }

        return null;
    }

    private RateLimitResult consume(
            Map<String, RateLimitWindow> windows,
            String key,
            int maximumRequests,
            long now
    ) {

        long currentWindow =
                now / WINDOW_SECONDS;

        RateLimitWindow window =
                windows.compute(
                        key,
                        (ignored, existing) -> {

                            if (existing == null
                                    || existing.window()
                                    != currentWindow) {

                                return new RateLimitWindow(
                                        currentWindow,
                                        1
                                );
                            }

                            return new RateLimitWindow(
                                    existing.window(),
                                    existing.count() + 1
                            );
                        }
                );

        boolean allowed =
                window.count()
                        <= maximumRequests;

        /*
         * Retry-After represents the number of seconds
         * remaining before the fixed window resets.
         */
        long retryAfterSeconds =
                Math.max(
                        1L,
                        WINDOW_SECONDS
                                - (now % WINDOW_SECONDS)
                );

        return new RateLimitResult(
                allowed,
                retryAfterSeconds
        );
    }

    private void cleanupExpiredWindowsIfNeeded(
            long now
    ) {

        long previousCleanup =
                lastCleanupEpochSecond.get();

        /*
         * Avoid scanning both maps for every request.
         */
        if (now - previousCleanup
                < CLEANUP_INTERVAL_SECONDS) {

            return;
        }

        /*
         * Allow only one request to perform cleanup during
         * each cleanup interval.
         */
        if (!lastCleanupEpochSecond.compareAndSet(
                previousCleanup,
                now
        )) {

            return;
        }

        long currentWindow =
                now / WINDOW_SECONDS;

        removeExpiredWindows(
                authWindows,
                currentWindow
        );

        removeExpiredWindows(
                apiWindows,
                currentWindow
        );
    }

    private void removeExpiredWindows(
            Map<String, RateLimitWindow> windows,
            long currentWindow
    ) {

        /*
         * ConcurrentHashMap supports concurrent conditional
         * removal while other requests update active entries.
         */
        windows.entrySet()
                .removeIf(entry ->
                        entry.getValue()
                                .window()
                                < currentWindow
                );
    }

    private String resolveClientKey(
            HttpServletRequest request
    ) {

        /*
         * X-Forwarded-For is intentionally not trusted until
         * the application is deployed behind a trusted proxy
         * that overwrites forwarded headers.
         */
        String remoteAddress =
                request.getRemoteAddr();

        if (remoteAddress == null
                || remoteAddress.isBlank()) {

            return "unknown";
        }

        return remoteAddress;
    }

    private boolean isExcludedPath(
            String path
    ) {

        return path.equals(
                "/api/v1/health"
        )
                || path.startsWith(
                "/swagger-ui"
        )
                || path.startsWith(
                "/v3/api-docs"
        );
    }

    private void writeRateLimitResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {

        /*
         * Preserve Retry-After while ApiProblemFactory writes
         * the RFC 7807 response body.
         */
        response.setHeader(
                "Retry-After",
                Long.toString(
                        retryAfterSeconds
                )
        );

        var problem =
                problemFactory.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "rate-limit-exceeded",
                        "Too many requests",
                        "Rate limit exceeded. Please try again later.",
                        request
                );

        problem.setProperty(
                "retryAfterSeconds",
                retryAfterSeconds
        );

        problemFactory.write(
                response,
                problem
        );
    }

    private record RateLimitWindow(
            long window,
            int count
    ) {
    }

    private record RateLimitResult(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }
}