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

        /*
         * Keep retry delays tiny in unit tests.
         *
         * Resilience4j exponential backoff requires the initial
         * interval to be at least 1 millisecond.
         */
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

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                "  Food was your largest spending category this month.  "
        );

        String result =
                narrator.narrate(
                        createMonthlyInsightData()
                );

        assertEquals(
                "Food was your largest spending category this month.",
                result
        );

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

    @Test
    void blankProviderResponseShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                "   "
        );

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () ->
                                narrator.narrate(
                                        createMonthlyInsightData()
                                )
                );

        assertEquals(
                "AI monthly insight is temporarily unavailable",
                exception.getMessage()
        );

        /*
         * Blank narration is malformed/invalid AI output.
         * Retrying the same provider response is not useful.
         */
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
                narrator.narrate(
                        createMonthlyInsightData()
                );

        assertEquals(
                "Spending increased this month.",
                result
        );

        /*
         * Initial request + two bounded retries.
         */
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
                assertThrows(
                        AiServiceException.class,
                        () ->
                                narrator.narrate(
                                        createMonthlyInsightData()
                                )
                );

        assertEquals(
                "AI monthly insight is temporarily unavailable",
                exception.getMessage()
        );

        /*
         * M22 policy:
         *
         * timeout -> NO retry
         * timeout -> YES circuit-breaker failure
         */
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
                        () ->
                                unconfiguredNarrator.narrate(
                                        createMonthlyInsightData()
                                )
                );

        assertEquals(
                "AI monthly insight is temporarily unavailable",
                exception.getMessage()
        );

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
                        () ->
                                unconfiguredNarrator.narrate(
                                        createMonthlyInsightData()
                                )
                );

        assertEquals(
                "AI monthly insight is temporarily unavailable",
                exception.getMessage()
        );

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

    @Test
    void nullMonthlyInsightDataShouldFailBeforeProviderCall() {

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () ->
                                narrator.narrate(null)
                );

        assertEquals(
                "Monthly insight data is unavailable",
                exception.getMessage()
        );

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