package com.expensemate.service.ai.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class GeminiResilienceExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiResilienceExecutor.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public GeminiResilienceExecutor(
            @Value("${gemini.resilience.retry.max-attempts:3}")
            int maxAttempts,

            @Value("${gemini.resilience.retry.initial-delay-ms:500}")
            long initialDelayMs,

            @Value("${gemini.resilience.circuit-breaker.failure-threshold:5}")
            int failureThreshold,

            @Value("${gemini.resilience.circuit-breaker.open-duration-seconds:30}")
            long openDurationSeconds
    ) {

        validateConfiguration(
                maxAttempts,
                initialDelayMs,
                failureThreshold,
                openDurationSeconds
        );

        /*
         * Retry policy:
         *
         * - Maximum attempts includes the original call.
         * - Backoff is exponential.
         * - Only transient provider failures are retried.
         * - Timeouts and invalid AI responses are deliberately
         *   not retried.
         */
        RetryConfig retryConfig =
                RetryConfig.custom()
                        .maxAttempts(maxAttempts)
                        .intervalFunction(
                                IntervalFunction.ofExponentialBackoff(
                                        initialDelayMs,
                                        2.0
                                )
                        )
                        .retryOnException(
                                this::isRetryableException
                        )
                        .build();

        this.retry =
                Retry.of(
                        "geminiRetry",
                        retryConfig
                );

        /*
         * Circuit breaker policy:
         *
         * A count-based sliding window is used.
         *
         * With the default configuration:
         * - window size = 5
         * - minimum calls = 5
         * - failure rate threshold = 100%
         *
         * Therefore the circuit opens when all calls in the
         * current five-call window are recorded provider failures.
         *
         * Invalid AI output and other application-level
         * non-retryable failures do not count against provider
         * availability.
         */
        CircuitBreakerConfig circuitBreakerConfig =
                CircuitBreakerConfig.custom()
                        .slidingWindowType(
                                CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                        )
                        .slidingWindowSize(failureThreshold)
                        .minimumNumberOfCalls(failureThreshold)
                        .failureRateThreshold(100.0F)
                        .waitDurationInOpenState(
                                Duration.ofSeconds(
                                        openDurationSeconds
                                )
                        )
                        .permittedNumberOfCallsInHalfOpenState(1)
                        .automaticTransitionFromOpenToHalfOpenEnabled(false)
                        .recordException(
                                this::isCircuitBreakerFailure
                        )
                        .build();

        this.circuitBreaker =
                CircuitBreaker.of(
                        "geminiCircuitBreaker",
                        circuitBreakerConfig
                );
    }

    public <T> T execute(
            String operation,
            Supplier<T> supplier
    ) {

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini operation name must not be blank"
            );
        }

        if (supplier == null) {
            throw new IllegalArgumentException(
                    "Gemini supplier must not be null"
            );
        }

        try {

            /*
             * Retry is the inner decorator.
             *
             * This means one logical Gemini operation may perform
             * multiple provider attempts, but the circuit breaker
             * observes the final outcome of that logical operation.
             */
            Supplier<T> retriedSupplier =
                    Retry.decorateSupplier(
                            retry,
                            supplier
                    );

            /*
             * Circuit breaker is the outer decorator.
             *
             * When the circuit is OPEN, the supplier is rejected
             * immediately and no retry/provider call occurs.
             */
            Supplier<T> protectedSupplier =
                    CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            retriedSupplier
                    );

            return protectedSupplier.get();

        } catch (CallNotPermittedException exception) {

            /*
             * Circuit is OPEN.
             *
             * Fail fast without exposing Resilience4j details
             * outside the AI infrastructure layer.
             */
            log.warn(
                    "Gemini request rejected because circuit breaker is open. Operation={}",
                    operation
            );

            throw new GeminiProviderUnavailableException(
                    "Gemini service is temporarily unavailable",
                    exception
            );

        } catch (GeminiNonRetryableException exception) {

            /*
             * Preserve the classification.
             *
             * Examples:
             * - malformed Gemini JSON
             * - invalid category
             * - invalid amount
             * - non-transient provider rejection
             *
             * These failures must not suddenly become provider
             * availability failures.
             */
            throw exception;

        } catch (GeminiTimeoutException exception) {

            /*
             * Timeout is intentionally not retried, but it represents
             * provider unavailability from the caller's perspective.
             *
             * It has already been recorded as a circuit-breaker
             * failure by Resilience4j.
             */
            log.warn(
                    "Gemini provider operation timed out. Operation={}",
                    operation
            );

            throw new GeminiProviderUnavailableException(
                    "Gemini service is temporarily unavailable",
                    exception
            );

        } catch (GeminiProviderUnavailableException exception) {

            /*
             * A transient provider failure has already been classified
             * correctly by the Gemini boundary.
             */
            throw exception;

        } catch (RuntimeException exception) {

            /*
             * Defensive fallback.
             *
             * Do not expose implementation details to callers and
             * never log API keys, prompts, user expense text, or
             * Gemini response bodies.
             */
            log.warn(
                    "Gemini provider operation failed. Operation={}, Cause={}",
                    operation,
                    exception.getClass().getSimpleName()
            );

            throw new GeminiProviderUnavailableException(
                    "Gemini service is temporarily unavailable",
                    exception
            );
        }
    }

    boolean isRetryableException(
            Throwable throwable
    ) {

        if (throwable == null) {
            return false;
        }

        /*
         * Invalid AI output, application validation failures and
         * non-transient provider responses must never trigger
         * another Gemini request.
         */
        if (throwable instanceof GeminiNonRetryableException) {
            return false;
        }

        /*
         * A full Gemini timeout can already consume significant
         * request time. Retrying it could multiply latency.
         */
        if (throwable instanceof GeminiTimeoutException) {
            return false;
        }

        /*
         * An OPEN circuit must always fail fast.
         */
        if (throwable instanceof CallNotPermittedException) {
            return false;
        }

        /*
         * Remaining provider failures are considered transient
         * candidates and may use the bounded retry policy.
         */
        return true;
    }

    boolean isCircuitBreakerFailure(
            Throwable throwable
    ) {

        if (throwable == null) {
            return false;
        }

        /*
         * Malformed output and other non-provider failures should
         * not make Gemini appear unavailable.
         */
        if (throwable instanceof GeminiNonRetryableException) {
            return false;
        }

        /*
         * CallNotPermittedException means the circuit was already
         * open. It is not a new provider failure.
         */
        if (throwable instanceof CallNotPermittedException) {
            return false;
        }

        /*
         * This deliberately includes GeminiTimeoutException.
         *
         * Timeout:
         * - NO retry
         * - YES circuit-breaker failure
         */
        return true;
    }

    /*
     * Package-private accessors are intentionally available for
     * deterministic unit testing without exposing resilience
     * implementation details as part of the public application API.
     */

    CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    Retry getRetry() {
        return retry;
    }

    private void validateConfiguration(
            int maxAttempts,
            long initialDelayMs,
            int failureThreshold,
            long openDurationSeconds
    ) {

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Gemini retry max attempts must be at least 1"
            );
        }

        if (initialDelayMs < 1) {
            throw new IllegalArgumentException(
                    "Gemini retry delay must be at least 1 millisecond"
            );
        }

        if (failureThreshold < 1) {
            throw new IllegalArgumentException(
                    "Gemini circuit breaker failure threshold must be at least 1"
            );
        }

        if (openDurationSeconds < 1) {
            throw new IllegalArgumentException(
                    "Gemini circuit breaker open duration must be at least 1 second"
            );
        }
    }
}