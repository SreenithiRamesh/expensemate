package com.expensemate.service;

import com.expensemate.dto.PersonalExpenseRequest;
import com.expensemate.dto.PersonalExpenseResponse;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.PersonalExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.ai.MonthlyInsightCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalExpenseServiceTest {

    @Mock
    private PersonalExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MonthlyInsightCacheService monthlyInsightCacheService;

    private PersonalExpenseService expenseService;

    @BeforeEach
    void setUp() {

        expenseService = new PersonalExpenseService(
                expenseRepository,
                userRepository,
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldCreateExpenseAndInvalidateMonthlyInsightCache() {

        User user = user();

        PersonalExpenseRequest request = request(
                "500.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 15),
                "  Lunch  "
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.save(any(PersonalExpense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PersonalExpenseResponse response =
                expenseService.createExpense(
                        "sree@example.com",
                        request
                );

        ArgumentCaptor<PersonalExpense> captor =
                ArgumentCaptor.forClass(
                        PersonalExpense.class
                );

        verify(expenseRepository)
                .save(captor.capture());

        PersonalExpense savedExpense =
                captor.getValue();

        assertEquals(
                new BigDecimal("500.00"),
                savedExpense.getAmount()
        );

        assertEquals(
                ExpenseCategory.FOOD,
                savedExpense.getCategory()
        );

        assertEquals(
                LocalDate.of(2026, 9, 15),
                savedExpense.getExpenseDate()
        );

        assertEquals(
                "Lunch",
                savedExpense.getDescription()
        );

        assertEquals(
                "Lunch",
                response.getDescription()
        );

        assertNotNull(
                savedExpense.getCreatedAt()
        );

        assertNotNull(
                savedExpense.getUpdatedAt()
        );

        verify(monthlyInsightCacheService)
                .invalidateForExpenseChange(
                        5L,
                        LocalDate.of(2026, 9, 15)
                );
    }

    @Test
    void shouldNormalizeBlankDescriptionToNullWhenCreatingExpense() {

        User user = user();

        PersonalExpenseRequest request = request(
                "100.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 15),
                "   "
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.save(any(PersonalExpense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PersonalExpenseResponse response =
                expenseService.createExpense(
                        "sree@example.com",
                        request
                );

        assertNull(
                response.getDescription()
        );

        verify(monthlyInsightCacheService)
                .invalidateForExpenseChange(
                        5L,
                        LocalDate.of(2026, 9, 15)
                );
    }

    @Test
    void shouldReturnExpenseOwnedByCurrentUser() {

        User user = user();

        PersonalExpense expense = expense(
                user,
                "250.00",
                ExpenseCategory.TRAVEL,
                LocalDate.of(2026, 9, 10),
                "Bus"
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                10L,
                5L
        )).thenReturn(Optional.of(expense));

        PersonalExpenseResponse response =
                expenseService.getExpenseById(
                        "sree@example.com",
                        10L
                );

        assertEquals(
                new BigDecimal("250.00"),
                response.getAmount()
        );

        assertEquals(
                ExpenseCategory.TRAVEL,
                response.getCategory()
        );

        assertEquals(
                LocalDate.of(2026, 9, 10),
                response.getExpenseDate()
        );

        assertEquals(
                "Bus",
                response.getDescription()
        );

        verify(expenseRepository)
                .findByIdAndUserId(
                        10L,
                        5L
                );
    }

    @Test
    void shouldRejectExpenseLookupWhenExpenseDoesNotBelongToUser() {

        User user = user();

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                99L,
                5L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> expenseService.getExpenseById(
                                "sree@example.com",
                                99L
                        )
                );

        assertEquals(
                "Expense not found",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectInvalidExpenseDateRange() {

        /*
         * This test does not need user.getId().
         *
         * The service validates the date range immediately
         * after loading the user and throws before building
         * the expense specification.
         */
        User user = mock(User.class);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> expenseService.getExpenses(
                                "sree@example.com",
                                null,
                                LocalDate.of(2026, 9, 20),
                                LocalDate.of(2026, 9, 10),
                                null,
                                PageRequest.of(0, 10)
                        )
                );

        assertEquals(
                "Start date must not be after end date",
                exception.getMessage()
        );

        verify(expenseRepository, never())
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }

    @Test
    void shouldUpdateExpenseAndInvalidateOldAndNewMonths() {

        User user = user();

        PersonalExpense existingExpense = expense(
                user,
                "300.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 8, 20),
                "Old expense"
        );

        PersonalExpenseRequest request = request(
                "450.00",
                ExpenseCategory.SHOPPING,
                LocalDate.of(2026, 9, 5),
                "  Updated expense  "
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                7L,
                5L
        )).thenReturn(
                Optional.of(existingExpense)
        );

        when(expenseRepository.save(existingExpense))
                .thenReturn(existingExpense);

        PersonalExpenseResponse response =
                expenseService.updateExpense(
                        "sree@example.com",
                        7L,
                        request
                );

        assertEquals(
                new BigDecimal("450.00"),
                response.getAmount()
        );

        assertEquals(
                ExpenseCategory.SHOPPING,
                response.getCategory()
        );

        assertEquals(
                LocalDate.of(2026, 9, 5),
                response.getExpenseDate()
        );

        assertEquals(
                "Updated expense",
                response.getDescription()
        );

        verify(expenseRepository)
                .save(existingExpense);

        verify(monthlyInsightCacheService)
                .invalidateForExpenseChange(
                        5L,
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 9, 5)
                );
    }

    @Test
    void shouldDeleteExpenseAndInvalidateAffectedMonth() {

        User user = user();

        PersonalExpense existingExpense = expense(
                user,
                "100.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 12),
                "Snack"
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                7L,
                5L
        )).thenReturn(
                Optional.of(existingExpense)
        );

        expenseService.deleteExpense(
                "sree@example.com",
                7L
        );

        verify(expenseRepository)
                .delete(existingExpense);

        verify(monthlyInsightCacheService)
                .invalidateForExpenseChange(
                        5L,
                        LocalDate.of(2026, 9, 12)
                );
    }

    @Test
    void shouldRejectUpdateWhenExpenseDoesNotExistForUser() {

        User user = user();

        PersonalExpenseRequest request = request(
                "200.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 15),
                "Updated"
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                999L,
                5L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> expenseService.updateExpense(
                                "sree@example.com",
                                999L,
                                request
                        )
                );

        assertEquals(
                "Expense not found",
                exception.getMessage()
        );

        verify(expenseRepository, never())
                .save(any(PersonalExpense.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldRejectDeleteWhenExpenseDoesNotExistForUser() {

        User user = user();

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(expenseRepository.findByIdAndUserId(
                999L,
                5L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> expenseService.deleteExpense(
                                "sree@example.com",
                                999L
                        )
                );

        assertEquals(
                "Expense not found",
                exception.getMessage()
        );

        verify(expenseRepository, never())
                .delete(any(PersonalExpense.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldRejectOperationWhenUserDoesNotExist() {

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        PersonalExpenseRequest request = request(
                "100.00",
                ExpenseCategory.FOOD,
                LocalDate.of(2026, 9, 15),
                "Lunch"
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> expenseService.createExpense(
                                "missing@example.com",
                                request
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(expenseRepository, never())
                .save(any(PersonalExpense.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    private PersonalExpenseRequest request(
            String amount,
            ExpenseCategory category,
            LocalDate date,
            String description
    ) {

        PersonalExpenseRequest request =
                new PersonalExpenseRequest();

        request.setAmount(
                new BigDecimal(amount)
        );

        request.setCategory(category);
        request.setExpenseDate(date);
        request.setDescription(description);

        return request;
    }

    private User user() {

        /*
         * User ID is generated by JPA in production and the
         * entity intentionally has no setId().
         *
         * For isolated service tests we mock the User and
         * provide only the ID actually required by the service.
         */
        User user = mock(User.class);

        when(user.getId())
                .thenReturn(5L);

        return user;
    }

    private PersonalExpense expense(
            User user,
            String amount,
            ExpenseCategory category,
            LocalDate date,
            String description
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        return new PersonalExpense(
                user,
                new BigDecimal(amount),
                category,
                date,
                description,
                now,
                now
        );
    }
}