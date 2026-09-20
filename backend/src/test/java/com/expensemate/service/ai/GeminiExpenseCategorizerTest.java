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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeminiExpenseCategorizerTest {

    private static final LocalDate REFERENCE_DATE =
            LocalDate.of(2026, 9, 21);

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

        stubProviderResponse(
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
                categorize();

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
                REFERENCE_DATE,
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

        stubProviderResponse(
                "{invalid-json"
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void truncatedJsonShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category":
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void invalidCategoryShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": "MAGIC",
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void missingCategoryShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void nullCategoryShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": null,
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void numericCategoryShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": 123,
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void blankCategoryShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": "   ",
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void lowercaseAndSpacePaddedCategoryShouldBeNormalized() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": "  food  ",
                  "description": "  Lunch  ",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        ExpenseCategorizationResult result =
                categorize();

        assertNotNull(result);

        assertEquals(
                ExpenseCategory.FOOD,
                result.category()
        );

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(result.amount())
        );

        assertEquals(
                "Lunch",
                result.description()
        );

        assertEquals(
                REFERENCE_DATE,
                result.expenseDate()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void invalidAmountShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": "not-a-number",
                  "category": "FOOD",
                  "description": "Lunch",
                  "expenseDate": "2026-09-21"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void invalidExpenseDateShouldFailWithoutRetry() {

        stubProviderResponse(
                """
                {
                  "amount": 250.00,
                  "category": "FOOD",
                  "description": "Lunch",
                  "expenseDate": "21-09-2026"
                }
                """
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
    }

    @Test
    void emptyProviderResponseShouldFailWithoutRetry() {

        stubProviderResponse(
                "   "
        );

        AiServiceException exception =
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

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
                        REFERENCE_DATE
                );

        assertNotNull(result);

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
                assertCategorizationFails();

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifySingleProviderInvocation();
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
                                REFERENCE_DATE
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
                                REFERENCE_DATE
                        )
                );

        assertEquals(
                "AI categorization is temporarily unavailable",
                exception.getMessage()
        );

        verifyNoInteractions(providerClient);
    }

    private ExpenseCategorizationResult categorize() {

        return categorizer.categorize(
                "Lunch 250",
                REFERENCE_DATE
        );
    }

    private AiServiceException assertCategorizationFails() {

        return assertThrows(
                AiServiceException.class,
                this::categorize
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
}