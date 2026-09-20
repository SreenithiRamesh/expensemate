package com.expensemate.service.ai;

import com.expensemate.dto.ai.ExpenseCategorizationResult;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.exception.AiServiceException;
import com.expensemate.service.ai.provider.GeminiProviderClient;
import com.expensemate.service.ai.resilience.GeminiProviderUnavailableException;
import com.expensemate.service.ai.resilience.GeminiResilienceExecutor;
import com.expensemate.service.ai.resilience.GeminiTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeminiExpenseCategorizerTest {

    private GeminiProviderClient providerClient;
    private GeminiExpenseCategorizer categorizer;

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

        categorizer =
                new GeminiExpenseCategorizer(
                        "test-api-key",
                        "test-model",
                        JsonMapper.builder().build(),
                        resilienceExecutor,
                        providerClient
                );
    }

    @Test
    void shouldReturnValidatedCategorizationWhenProviderResponseIsValid() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                """
                {
                  "amount": 250.00,
                  "category": "FOOD",
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        ExpenseCategorizationResult result =
                categorizer.categorize(
                        "Spent 250 for lunch",
                        LocalDate.of(2026, 9, 21)
                );

        assertNotNull(result);
        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(result.amount())
        );
        assertEquals(
                ExpenseCategory.FOOD,
                result.category()
        );
        assertEquals(
                "Lunch",
                result.description()
        );
        assertEquals(
                LocalDate.of(2026, 9, 21),
                result.expenseDate()
        );

        verify(
                providerClient,
                times(1)
        ).generate(
                eq("test-model"),
                anyString(),
                any(),
                eq(15_000)
        );
    }

    @Test
    void malformedJsonShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                "{invalid-json"
        );

        AiServiceException exception =
                assertThrows(
                        AiServiceException.class,
                        () -> categorizer.categorize(
                                "Lunch 250",
                                LocalDate.of(2026, 9, 21)
                        )
                );

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
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
    void invalidCategoryShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn(
                """
                {
                  "amount": 250.00,
                  "category": "MAGIC",
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        assertThrows(
                AiServiceException.class,
                () -> categorizer.categorize(
                        "Lunch 250",
                        LocalDate.of(2026, 9, 21)
                )
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
    void emptyProviderResponseShouldFailWithoutRetry() {

        when(
                providerClient.generate(
                        anyString(),
                        anyString(),
                        any(),
                        anyInt()
                )
        ).thenReturn("   ");

        assertThrows(
                AiServiceException.class,
                () -> categorizer.categorize(
                        "Lunch 250",
                        LocalDate.of(2026, 9, 21)
                )
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
                        """
                        {
                          "amount": 100.00,
                          "category": "TRAVEL",
                          "description": "Bus",
                          "expenseDate": "2026-09-21"
                        }
                        """
                );

        ExpenseCategorizationResult result =
                categorizer.categorize(
                        "Bus 100",
                        LocalDate.of(2026, 9, 21)
                );

        assertEquals(
                ExpenseCategory.TRAVEL,
                result.category()
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(result.amount())
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
                        () -> categorizer.categorize(
                                "Lunch 250",
                                LocalDate.of(2026, 9, 21)
                        )
                );

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
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
    void missingApiKeyShouldFailBeforeCallingProvider() {

        GeminiExpenseCategorizer missingKeyCategorizer =
                new GeminiExpenseCategorizer(
                        "",
                        "test-model",
                        JsonMapper.builder().build(),
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
                        () -> missingKeyCategorizer.categorize(
                                "Lunch 250",
                                LocalDate.of(2026, 9, 21)
                        )
                );

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
    }

    @Test
    void missingModelShouldFailBeforeCallingProvider() {

        GeminiExpenseCategorizer missingModelCategorizer =
                new GeminiExpenseCategorizer(
                        "test-api-key",
                        "",
                        JsonMapper.builder().build(),
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
                        () -> missingModelCategorizer.categorize(
                                "Lunch 250",
                                LocalDate.of(2026, 9, 21)
                        )
                );

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
    }
}
