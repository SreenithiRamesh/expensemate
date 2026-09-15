package com.expensemate.service.ai;

import com.expensemate.dto.ai.AiExpenseSuggestionResponse;
import com.expensemate.dto.ai.ExpenseCategorizationResult;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.User;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiExpenseServiceImplTest {

    private UserRepository userRepository;
    private ExpenseCategorizer expenseCategorizer;
    private AiUsageService aiUsageService;

    private AiExpenseServiceImpl service;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        expenseCategorizer = mock(ExpenseCategorizer.class);
        aiUsageService = mock(AiUsageService.class);

        service = new AiExpenseServiceImpl(
                userRepository,
                expenseCategorizer,
                aiUsageService
        );
    }

    @Test
    void shouldReturnSuggestionWithoutSavingExpense() {

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);

        when(userRepository.findByEmail("sree@test.com"))
                .thenReturn(Optional.of(user));

        when(expenseCategorizer.categorize(
                eq("Paid 450 for petrol today"),
                any(LocalDate.class)
        )).thenReturn(
                new ExpenseCategorizationResult(
                        new BigDecimal("450.00"),
                        ExpenseCategory.TRAVEL,
                        "Petrol",
                        LocalDate.now()
                )
        );

        when(aiUsageService.recordSuccessfulRequest(1L))
                .thenReturn(9);

        AiExpenseSuggestionResponse response =
                service.categorize(
                        "sree@test.com",
                        "Paid 450 for petrol today"
                );

        assertEquals(
                new BigDecimal("450.00"),
                response.amount()
        );

        assertEquals(
                ExpenseCategory.TRAVEL,
                response.category()
        );

        assertEquals(
                "Petrol",
                response.description()
        );

        assertTrue(response.requiresReview());
        assertEquals(9, response.remainingRequests());

        verify(aiUsageService).verifyLimit(1L);

        verify(aiUsageService)
                .recordSuccessfulRequest(1L);
    }

    @Test
    void shouldTrimInputBeforeSendingToCategorizer() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        when(expenseCategorizer.categorize(
                anyString(),
                any(LocalDate.class)
        )).thenReturn(
                new ExpenseCategorizationResult(
                        new BigDecimal("100.00"),
                        ExpenseCategory.FOOD,
                        "Lunch",
                        LocalDate.now()
                )
        );

        when(aiUsageService.recordSuccessfulRequest(5L))
                .thenReturn(8);

        service.categorize(
                "user@test.com",
                "   Lunch 100   "
        );

        ArgumentCaptor<String> captor =
                ArgumentCaptor.forClass(String.class);

        verify(expenseCategorizer)
                .categorize(
                        captor.capture(),
                        any(LocalDate.class)
                );

        assertEquals(
                "Lunch 100",
                captor.getValue()
        );
    }

    @Test
    void shouldCheckLimitBeforeCallingGemini() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        doThrow(new RuntimeException("limit"))
                .when(aiUsageService)
                .verifyLimit(7L);

        assertThrows(
                RuntimeException.class,
                () -> service.categorize(
                        "user@test.com",
                        "Food 100"
                )
        );

        verifyNoInteractions(expenseCategorizer);

        verify(aiUsageService, never())
                .recordSuccessfulRequest(anyLong());
    }

    @Test
    void shouldNotRecordUsageWhenCategorizationFails() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(8L);

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        when(expenseCategorizer.categorize(
                anyString(),
                any(LocalDate.class)
        )).thenThrow(
                new RuntimeException("Gemini unavailable")
        );

        assertThrows(
                RuntimeException.class,
                () -> service.categorize(
                        "user@test.com",
                        "Dinner 200"
                )
        );

        verify(aiUsageService, never())
                .recordSuccessfulRequest(anyLong());
    }
}