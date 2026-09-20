package com.expensemate.service.ai.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiResilienceExecutorTest {

    @Test
    void executeShouldReturnImmediatelyWhenProviderSucceeds() {

        GeminiResilienceExecutor executor =
                createExecutor();

        AtomicInteger calls =
                new AtomicInteger();

        String result =
                executor.execute(
                        "test-operation",
                        () -> {
                            calls.incrementAndGet();
                            return "success";
                        }
                );

        assertEquals("success", result);
        assertEquals(1, calls.get());

        assertEquals(
                CircuitBreaker.State.CLOSED,
                executor.getCircuitBreakerState()
        );
    }

    @Test
    void executeShouldRetryTransientFailureAndThenSucceed() {

        GeminiResilienceExecutor executor =
                createExecutor();

        AtomicInteger calls =
                new AtomicInteger();

        String result =
                executor.execute(
                        "test-operation",
                        () -> {
                            int attempt =
                                    calls.incrementAndGet();

                            if (attempt < 3) {
                                throw new RuntimeException(
                                        "temporary provider failure"
                                );
                            }

                            return "recovered";
                        }
                );

        assertEquals("recovered", result);
        assertEquals(3, calls.get());
    }

    @Test
    void executeShouldStopAfterMaximumRetryAttempts() {

        GeminiResilienceExecutor executor =
                createExecutor();

        AtomicInteger calls =
                new AtomicInteger();

        assertThrows(
                GeminiProviderUnavailableException.class,
                () -> executor.execute(
                        "test-operation",
                        () -> {
                            calls.incrementAndGet();

                            throw new RuntimeException(
                                    "provider unavailable"
                            );
                        }
                )
        );

        assertEquals(3, calls.get());
    }

    @Test
    void nonRetryableFailureShouldNotBeRetried() {

        GeminiResilienceExecutor executor =
                createExecutor();

        AtomicInteger calls =
                new AtomicInteger();

        assertThrows(
                GeminiNonRetryableException.class,
                () -> executor.execute(
                        "test-operation",
                        () -> {
                            calls.incrementAndGet();

                            throw new GeminiNonRetryableException(
                                    "invalid provider response"
                            );
                        }
                )
        );

        assertEquals(1, calls.get());
    }

    @Test
    void repeatedProviderFailuresShouldOpenCircuit() {

        GeminiResilienceExecutor executor =
                createExecutorWithSingleAttempt();

        for (int attempt = 0; attempt < 5; attempt++) {

            assertThrows(
                    GeminiProviderUnavailableException.class,
                    () -> executor.execute(
                            "test-operation",
                            () -> {
                                throw new RuntimeException(
                                        "provider unavailable"
                                );
                            }
                    )
            );
        }

        assertEquals(
                CircuitBreaker.State.OPEN,
                executor.getCircuitBreakerState()
        );
    }

    @Test
    void openCircuitShouldRejectRequestWithoutCallingProvider() {

        GeminiResilienceExecutor executor =
                createExecutorWithSingleAttempt();

        AtomicInteger providerCalls =
                new AtomicInteger();

        for (int attempt = 0; attempt < 5; attempt++) {

            assertThrows(
                    GeminiProviderUnavailableException.class,
                    () -> executor.execute(
                            "test-operation",
                            () -> {
                                providerCalls.incrementAndGet();

                                throw new RuntimeException(
                                        "provider unavailable"
                                );
                            }
                    )
            );
        }

        assertEquals(5, providerCalls.get());

        assertThrows(
                GeminiProviderUnavailableException.class,
                () -> executor.execute(
                        "test-operation",
                        () -> {
                            providerCalls.incrementAndGet();
                            return "should-not-run";
                        }
                )
        );

        /*
         * The provider must not be called once the
         * circuit has opened.
         */
        assertEquals(5, providerCalls.get());
    }

    @Test
    void nonRetryableFailureShouldNotOpenCircuit() {

        GeminiResilienceExecutor executor =
                createExecutorWithSingleAttempt();

        for (int attempt = 0; attempt < 10; attempt++) {

            assertThrows(
                    GeminiNonRetryableException.class,
                    () -> executor.execute(
                            "test-operation",
                            () -> {
                                throw new GeminiNonRetryableException(
                                        "invalid response"
                                );
                            }
                    )
            );
        }

        assertEquals(
                CircuitBreaker.State.CLOSED,
                executor.getCircuitBreakerState()
        );
    }

    @Test
    void timeoutShouldNotRetryButShouldCountAsCircuitFailure() {

        GeminiResilienceExecutor executor =
                new GeminiResilienceExecutor(
                        3,
                        1,
                        2,
                        30
                );

        AtomicInteger calls =
                new AtomicInteger();

        /*
         * Two logical operations time out.
         *
         * Each operation must call the provider only once,
         * even though maxAttempts is 3.
         */
        for (int attempt = 0; attempt < 2; attempt++) {

            assertThrows(
                    GeminiProviderUnavailableException.class,
                    () -> executor.execute(
                            "timeout-operation",
                            () -> {
                                calls.incrementAndGet();

                                throw new GeminiTimeoutException(
                                        "timed out",
                                        new RuntimeException(
                                                "timeout"
                                        )
                                );
                            }
                    )
            );
        }

        /*
         * If timeout had been retried, this value would
         * be greater than 2.
         */
        assertEquals(2, calls.get());

        /*
         * Timeout is still a provider availability failure,
         * so two failures open this test circuit.
         */
        assertEquals(
                CircuitBreaker.State.OPEN,
                executor.getCircuitBreakerState()
        );
    }

    @Test
    void invalidConfigurationShouldFailFast() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiResilienceExecutor(
                        0,
                        1,
                        5,
                        30
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiResilienceExecutor(
                        3,
                        -1,
                        5,
                        30
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiResilienceExecutor(
                        3,
                        1,
                        0,
                        30
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiResilienceExecutor(
                        3,
                        1,
                        5,
                        0
                )
        );
    }

    @Test
    void blankOperationShouldBeRejected() {

        GeminiResilienceExecutor executor =
                createExecutor();

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        " ",
                        () -> "result"
                )
        );
    }

    @Test
    void nullSupplierShouldBeRejected() {

        GeminiResilienceExecutor executor =
                createExecutor();

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        "test-operation",
                        null
                )
        );
    }

    @Test
    void circuitShouldAllowHalfOpenProbeAfterWaitDuration()
            throws InterruptedException {

        GeminiResilienceExecutor executor =
                new GeminiResilienceExecutor(
                        1,
                        1,
                        2,
                        1
                );

        for (int attempt = 0; attempt < 2; attempt++) {

            assertThrows(
                    GeminiProviderUnavailableException.class,
                    () -> executor.execute(
                            "test-operation",
                            () -> {
                                throw new RuntimeException(
                                        "provider unavailable"
                                );
                            }
                    )
            );
        }

        assertEquals(
                CircuitBreaker.State.OPEN,
                executor.getCircuitBreakerState()
        );

        Thread.sleep(1_100);

        AtomicInteger calls =
                new AtomicInteger();

        String result =
                executor.execute(
                        "test-operation",
                        () -> {
                            calls.incrementAndGet();
                            return "recovered";
                        }
                );

        assertEquals("recovered", result);
        assertEquals(1, calls.get());

        assertEquals(
                CircuitBreaker.State.CLOSED,
                executor.getCircuitBreakerState()
        );
    }

    private GeminiResilienceExecutor createExecutor() {

        return new GeminiResilienceExecutor(
                3,
                1,
                5,
                30
        );
    }

    private GeminiResilienceExecutor
    createExecutorWithSingleAttempt() {

        return new GeminiResilienceExecutor(
                1,
                1,
                5,
                30
        );
    }
}