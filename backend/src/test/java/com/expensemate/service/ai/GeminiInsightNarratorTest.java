package com.expensemate.service.ai;

import com.expensemate.dto.ai.BudgetInsightData;
import com.expensemate.dto.ai.CategoryInsightData;
import com.expensemate.dto.ai.MonthlyInsightData;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.exception.AiServiceException;
import com.expensemate.service.ai.provider.GeminiProviderClient;
import com.expensemate.service.ai.resilience.GeminiProviderUnavailableException;
import com.expensemate.service.ai.resilience.GeminiResilienceExecutor;
import com.expensemate.service.ai.resilience.GeminiTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeminiInsightNarratorTest {

    private GeminiProviderClient providerClient;
    private GeminiInsightNarrator narrator;

    @BeforeEach
    void setUp() {

        providerClient =
                mock(GeminiProviderClient.class);

        GeminiResilienceExecutor resilienceExecutor =
                new GeminiResilienceExecutor(
                        3,
                        1,
                        5,
                        1
                );

        narrator =
                new GeminiInsightNarrator(
                        "test-api-key",
                        "test-model",
                        resilienceExecutor,
                        providerClient
                );
    }

    @Test
    void validProviderResponseShouldReturnTrimmedNarration() {

        stubProviderResponse(
                "  Food was your largest spending category this month.  "
        );

        String result =
                narrate();

        assertEquals(
                "Food was your largest spending category this month.",
                result
        );

        verifySingleProviderInvocation();
    }

    @Test
    void blankProviderResponseShouldFailWithoutRetry() {

        stubProviderResponse(
                "   "
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        verifySingleProviderInvocation();
    }

    @Test
    void emptyProviderResponseShouldFailWithoutRetry() {

        stubProviderResponse(
                ""
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        verifySingleProviderInvocation();
    }

    @Test
    void nullProviderResponseShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                (String) null
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        verifySingleProviderInvocation();
    }

    @Test
    void providerRuntimeFailureShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenThrow(
                new IllegalStateException(
                        "unexpected provider response"
                )
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        verifySingleProviderInvocation();
    }

    @Test
    void transientProviderFailureShouldRetryAndThenSucceed() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        )
                .thenThrow(
                        new GeminiProviderUnavailableException(
                                "temporary provider failure"
                        )
                )
                .thenThrow(
                        new GeminiProviderUnavailableException(
                                "temporary provider failure"
                        )
                )
                .thenReturn(
                        "Spending increased this month."
                );

        String result =
                narrate();

        assertEquals(
                "Spending increased this month.",
                result
        );

        verify(
                providerClient,
                times(3)
        ).generate(
                anyString(),
                anyString(),
                any(),
                anyInt()
        );
    }

    @Test
    void transientProviderFailureShouldStopAfterMaximumAttempts() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenThrow(
                new GeminiProviderUnavailableException(
                        "provider remains unavailable"
                )
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        verify(
                providerClient,
                times(3)
        ).generate(
                anyString(),
                anyString(),
                any(),
                anyInt()
        );
    }

    @Test
    void timeoutShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenThrow(
                new GeminiTimeoutException(
                        "Gemini request timed out",
                        new RuntimeException("timeout")
                )
        );

        AiServiceException exception =
                assertNarrationFails();

        assertTemporaryUnavailable(exception);

        /*
         * Established M22 policy:
         *
         * timeout -> no retry
         * timeout -> circuit-breaker failure
         */
        verifySingleProviderInvocation();
    }

    @Test
    void missingApiKeyShouldFailBeforeProviderCall() {

        GeminiInsightNarrator unconfiguredNarrator =
                new GeminiInsightNarrator(
                        "",
                        "test-model",
                        new GeminiResilienceExecutor(
                                3,
                                1,
                                5,
                                1
                        ),
                        providerClient
                );

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () -> unconfiguredNarrator.narrate(
                                createMonthlyInsightData()
                        )
                );

        assertTemporaryUnavailable(exception);

        verifyProviderWasNeverCalled();
    }

    @Test
    void missingModelShouldFailBeforeProviderCall() {

        GeminiInsightNarrator unconfiguredNarrator =
                new GeminiInsightNarrator(
                        "test-api-key",
                        "   ",
                        new GeminiResilienceExecutor(
                                3,
                                1,
                                5,
                                1
                        ),
                        providerClient
                );

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () -> unconfiguredNarrator.narrate(
                                createMonthlyInsightData()
                        )
                );

        assertTemporaryUnavailable(exception);

        verifyProviderWasNeverCalled();
    }

    @Test
    void nullMonthlyInsightDataShouldFailBeforeProviderCall() {

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () -> narrator.narrate(null)
                );

        assertEquals(
                "Monthly insight data is unavailable",
                exception.getMessage()
        );

        verifyProviderWasNeverCalled();
    }

    private String narrate() {

        return narrator.narrate(
                createMonthlyInsightData()
        );
    }

    private AiServiceException assertNarrationFails() {

        return assertThrows(
                AiServiceException.class,
                this::narrate
        );
    }

    private void assertTemporaryUnavailable(
            AiServiceException exception
    ) {

        assertEquals(
                "AI monthly insight is temporarily unavailable",
                exception.getMessage()
        );
    }

    private void stubProviderResponse(
            String response
    ) {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(response);
    }

    private void verifySingleProviderInvocation() {

        verify(
                providerClient,
                times(1)
        ).generate(
                anyString(),
                anyString(),
                any(),
                anyInt()
        );
    }

    private void verifyProviderWasNeverCalled() {

        verify(
                providerClient,
                never()
        ).generate(
                anyString(),
                anyString(),
                any(),
                anyInt()
        );
    }

    private MonthlyInsightData createMonthlyInsightData() {

        CategoryInsightData food =
                new CategoryInsightData(
                        ExpenseCategory.FOOD,
                        new BigDecimal("600.00"),
                        new BigDecimal("60.00")
                );

        CategoryInsightData travel =
                new CategoryInsightData(
                        ExpenseCategory.TRAVEL,
                        new BigDecimal("400.00"),
                        new BigDecimal("40.00")
                );

        BudgetInsightData foodBudget =
                new BudgetInsightData(
                        ExpenseCategory.FOOD,
                        new BigDecimal("800.00"),
                        new BigDecimal("600.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("75.00"),
                        "SAFE"
                );

        return new MonthlyInsightData(
                YearMonth.of(2026, 9),
                new BigDecimal("1000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("200.00"),
                List.of(
                        food,
                        travel
                ),
                List.of(
                        foodBudget
                )
        );
    }
}