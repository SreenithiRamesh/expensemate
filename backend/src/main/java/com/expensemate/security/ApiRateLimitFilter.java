package com.expensemate.security;

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
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60L;

    /*
     * Cleanup does not need to run on every request.
     * Running it periodically keeps stale client entries
     * from accumulating indefinitely.
     */
    private static final long CLEANUP_INTERVAL_SECONDS = 60L;

    private final Map<String, RateLimitWindow> authWindows =
            new ConcurrentHashMap<>();

    private final Map<String, RateLimitWindow> apiWindows =
            new ConcurrentHashMap<>();

    private final AtomicLong lastCleanupEpochSecond =
            new AtomicLong(0L);

    private final int authRequestsPerMinute;
    private final int apiRequestsPerMinute;

    public ApiRateLimitFilter(
            @Value("${app.rate-limit.auth.requests-per-minute:10}")
            int authRequestsPerMinute,

            @Value("${app.rate-limit.api.requests-per-minute:120}")
            int apiRequestsPerMinute
    ) {

        /*
         * Invalid security configuration should fail fast
         * instead of silently disabling or breaking
         * rate-limit behaviour.
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
         * CORS preflight requests should not consume
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
         * Health and API documentation endpoints are
         * intentionally excluded from application
         * rate limiting.
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

        /*
         * Remove expired client windows periodically.
         *
         * Without cleanup, attacker-controlled IP addresses
         * could leave stale entries in these maps forever.
         */
        cleanupExpiredWindowsIfNeeded(now);

        String clientKey =
                resolveClientKey(request);

        RateLimitResult result = null;

        if (path.startsWith(
                "/api/v1/auth/"
        )) {

            result =
                    consume(
                            authWindows,
                            clientKey,
                            authRequestsPerMinute,
                            now
                    );

        } else if (path.startsWith(
                "/api/v1/"
        )) {

            result =
                    consume(
                            apiWindows,
                            clientKey,
                            apiRequestsPerMinute,
                            now
                    );
        }

        if (result != null
                && !result.allowed()) {

            writeRateLimitResponse(
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
         * Retry-After should represent the number of
         * seconds remaining until the current fixed
         * one-minute window resets.
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
         * Only one request should perform cleanup for
         * a particular cleanup interval.
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
         * ConcurrentHashMap supports safe concurrent
         * conditional removal while requests are updating
         * other entries.
         *
         * Keep the current window and remove only entries
         * belonging to older windows.
         */
        windows.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue()
                                        .window()
                                        < currentWindow
                );
    }

    private String resolveClientKey(
            HttpServletRequest request
    ) {

        /*
         * Do not trust X-Forwarded-For yet.
         *
         * Forwarded headers can be spoofed unless the
         * application is behind a trusted proxy that
         * overwrites them.
         *
         * Proxy-aware client-IP handling should be added
         * when the deployment environment is fixed.
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
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {

        response.setStatus(
                HttpStatus.TOO_MANY_REQUESTS.value()
        );

        response.setContentType(
                "application/json"
        );

        response.setCharacterEncoding(
                "UTF-8"
        );

        response.setHeader(
                "Retry-After",
                Long.toString(
                        retryAfterSeconds
                )
        );

        response.getWriter()
                .write(
                        """
                        {
                          "status": 429,
                          "error": "Too Many Requests",
                          "message": "Rate limit exceeded. Please try again later."
                        }
                        """
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