package com.expensemate.service;

import com.expensemate.dto.BudgetRequest;
import com.expensemate.dto.BudgetResponse;
import com.expensemate.entity.Budget;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.User;
import com.expensemate.exception.BudgetAlreadyExistsException;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.BudgetRepository;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.ai.MonthlyInsightCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalExpenseRepository expenseRepository;

    @Mock
    private MonthlyInsightCacheService monthlyInsightCacheService;

    private BudgetService budgetService;

    @BeforeEach
    void setUp() {

        budgetService = new BudgetService(
                budgetRepository,
                userRepository,
                expenseRepository,
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldCreateBudgetAndInvalidateMonthlyInsightCache() {

        User user = user();

        BudgetRequest request = request(
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository
                        .existsByUserIdAndCategoryAndMonthAndYear(
                                5L,
                                ExpenseCategory.FOOD,
                                9,
                                2026
                        )
        ).thenReturn(false);

        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(
                expenseRepository.calculateTotalSpent(
                        eq(5L),
                        eq(ExpenseCategory.FOOD),
                        eq(LocalDate.of(2026, 9, 1)),
                        eq(LocalDate.of(2026, 9, 30))
                )
        ).thenReturn(
                new BigDecimal("1250.00")
        );

        BudgetResponse response =
                budgetService.createBudget(
                        "  SREE@EXAMPLE.COM  ",
                        request
                );

        assertEquals(
                ExpenseCategory.FOOD,
                response.getCategory()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                response.getMonthlyLimit()
        );

        assertEquals(
                9,
                response.getMonth()
        );

        assertEquals(
                2026,
                response.getYear()
        );

        assertEquals(
                new BigDecimal("1250.00"),
                response.getSpent()
        );

        assertEquals(
                new BigDecimal("3750.00"),
                response.getRemaining()
        );

        assertEquals(
                new BigDecimal("25.00"),
                response.getPercentageUsed()
        );

        verify(userRepository)
                .findByEmail(
                        "sree@example.com"
                );

        verify(monthlyInsightCacheService)
                .invalidateForBudgetChange(
                        5L,
                        YearMonth.of(2026, 9)
                );
    }

    @Test
    void shouldRejectDuplicateBudgetOnCreate() {

        User user = user();

        BudgetRequest request = request(
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository
                        .existsByUserIdAndCategoryAndMonthAndYear(
                                5L,
                                ExpenseCategory.FOOD,
                                9,
                                2026
                        )
        ).thenReturn(true);

        BudgetAlreadyExistsException exception =
                assertThrows(
                        BudgetAlreadyExistsException.class,
                        () -> budgetService.createBudget(
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "Budget already exists for this category and month",
                exception.getMessage()
        );

        verify(
                budgetRepository,
                never()
        ).save(any(Budget.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldReturnOwnedBudgetAndCalculateSpending() {

        User user = user();

        Budget budget = budget(
                user,
                ExpenseCategory.TRAVEL,
                "2000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        10L,
                        5L
                )
        ).thenReturn(
                Optional.of(budget)
        );

        when(
                expenseRepository.calculateTotalSpent(
                        5L,
                        ExpenseCategory.TRAVEL,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(
                new BigDecimal("500.00")
        );

        BudgetResponse response =
                budgetService.getBudgetById(
                        "sree@example.com",
                        10L
                );

        assertEquals(
                ExpenseCategory.TRAVEL,
                response.getCategory()
        );

        assertEquals(
                new BigDecimal("2000.00"),
                response.getMonthlyLimit()
        );

        assertEquals(
                new BigDecimal("500.00"),
                response.getSpent()
        );

        assertEquals(
                new BigDecimal("1500.00"),
                response.getRemaining()
        );

        assertEquals(
                new BigDecimal("25.00"),
                response.getPercentageUsed()
        );

        verify(budgetRepository)
                .findOwnedBudget(
                        10L,
                        5L
                );
    }

    @Test
    void shouldTreatNullSpentAsZero() {

        User user = user();

        Budget budget = budget(
                user,
                ExpenseCategory.SHOPPING,
                "3000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        10L,
                        5L
                )
        ).thenReturn(
                Optional.of(budget)
        );

        when(
                expenseRepository.calculateTotalSpent(
                        5L,
                        ExpenseCategory.SHOPPING,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(null);

        BudgetResponse response =
                budgetService.getBudgetById(
                        "sree@example.com",
                        10L
                );

        assertEquals(
                BigDecimal.ZERO,
                response.getSpent()
        );

        assertEquals(
                new BigDecimal("3000.00"),
                response.getRemaining()
        );

        assertEquals(
                new BigDecimal("0.00"),
                response.getPercentageUsed()
        );
    }

    @Test
    void shouldRejectBudgetLookupWhenBudgetDoesNotBelongToUser() {

        User user = user();

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        99L,
                        5L
                )
        ).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> budgetService.getBudgetById(
                                "sree@example.com",
                                99L
                        )
                );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                expenseRepository
        );
    }

    @Test
    void shouldRejectBudgetFilterWhenOnlyMonthIsProvided() {

        User user = mock(User.class);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> budgetService.getBudgets(
                                "sree@example.com",
                                9,
                                null
                        )
                );

        assertEquals(
                "Month and year must be provided together",
                exception.getMessage()
        );

        verifyNoInteractions(
                budgetRepository
        );
    }

    @Test
    void shouldRejectBudgetFilterWhenOnlyYearIsProvided() {

        User user = mock(User.class);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> budgetService.getBudgets(
                                "sree@example.com",
                                null,
                                2026
                        )
                );

        assertEquals(
                "Month and year must be provided together",
                exception.getMessage()
        );

        verifyNoInteractions(
                budgetRepository
        );
    }

    @Test
    void shouldRejectInvalidMonth() {

        User user = mock(User.class);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> budgetService.getBudgets(
                                "sree@example.com",
                                13,
                                2026
                        )
                );

        assertEquals(
                "Month must be between 1 and 12",
                exception.getMessage()
        );

        verifyNoInteractions(
                budgetRepository
        );
    }

    @Test
    void shouldRejectYearBefore2000() {

        User user = mock(User.class);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> budgetService.getBudgets(
                                "sree@example.com",
                                9,
                                1999
                        )
                );

        assertEquals(
                "Year must be 2000 or later",
                exception.getMessage()
        );

        verifyNoInteractions(
                budgetRepository
        );
    }

    @Test
    void shouldGetBudgetsForSpecifiedMonthAndYear() {

        User user = user();

        Budget budget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findAllForUserAndPeriod(
                        5L,
                        9,
                        2026
                )
        ).thenReturn(
                List.of(budget)
        );

        when(
                expenseRepository.calculateTotalSpent(
                        5L,
                        ExpenseCategory.FOOD,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(
                new BigDecimal("1000.00")
        );

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        "sree@example.com",
                        9,
                        2026
                );

        assertEquals(
                1,
                responses.size()
        );

        assertEquals(
                ExpenseCategory.FOOD,
                responses.get(0).getCategory()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                responses.get(0).getSpent()
        );

        verify(budgetRepository)
                .findAllForUserAndPeriod(
                        5L,
                        9,
                        2026
                );
    }

    @Test
    void shouldGetAllBudgetsWhenPeriodIsNotProvided() {

        User user = user();

        Budget foodBudget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        Budget travelBudget = budget(
                user,
                ExpenseCategory.TRAVEL,
                "2000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findAllForUser(5L)
        ).thenReturn(
                List.of(
                        foodBudget,
                        travelBudget
                )
        );

        when(
                expenseRepository.calculateTotalSpent(
                        eq(5L),
                        any(ExpenseCategory.class),
                        eq(LocalDate.of(2026, 9, 1)),
                        eq(LocalDate.of(2026, 9, 30))
                )
        ).thenReturn(BigDecimal.ZERO);

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        "sree@example.com",
                        null,
                        null
                );

        assertEquals(
                2,
                responses.size()
        );

        verify(budgetRepository)
                .findAllForUser(5L);
    }

    @Test
    void shouldUpdateBudgetAndInvalidateOldAndNewMonths() {

        User user = user();

        Budget existingBudget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                8,
                2026
        );

        BudgetRequest request = request(
                ExpenseCategory.TRAVEL,
                "3000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        7L,
                        5L
                )
        ).thenReturn(
                Optional.of(existingBudget)
        );

        when(
                budgetRepository
                        .existsByUserIdAndCategoryAndMonthAndYear(
                                5L,
                                ExpenseCategory.TRAVEL,
                                9,
                                2026
                        )
        ).thenReturn(false);

        when(budgetRepository.save(existingBudget))
                .thenReturn(existingBudget);

        when(
                expenseRepository.calculateTotalSpent(
                        5L,
                        ExpenseCategory.TRAVEL,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(
                new BigDecimal("750.00")
        );

        BudgetResponse response =
                budgetService.updateBudget(
                        "sree@example.com",
                        7L,
                        request
                );

        assertEquals(
                ExpenseCategory.TRAVEL,
                response.getCategory()
        );

        assertEquals(
                new BigDecimal("3000.00"),
                response.getMonthlyLimit()
        );

        assertEquals(
                9,
                response.getMonth()
        );

        assertEquals(
                2026,
                response.getYear()
        );

        assertEquals(
                new BigDecimal("750.00"),
                response.getSpent()
        );

        assertEquals(
                new BigDecimal("2250.00"),
                response.getRemaining()
        );

        assertEquals(
                new BigDecimal("25.00"),
                response.getPercentageUsed()
        );

        verify(monthlyInsightCacheService)
                .invalidateForBudgetChange(
                        5L,
                        YearMonth.of(2026, 8),
                        YearMonth.of(2026, 9)
                );
    }

    @Test
    void shouldNotCheckDuplicateWhenBudgetIdentityDoesNotChange() {

        User user = user();

        Budget existingBudget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        BudgetRequest request = request(
                ExpenseCategory.FOOD,
                "6000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        7L,
                        5L
                )
        ).thenReturn(
                Optional.of(existingBudget)
        );

        when(budgetRepository.save(existingBudget))
                .thenReturn(existingBudget);

        when(
                expenseRepository.calculateTotalSpent(
                        5L,
                        ExpenseCategory.FOOD,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(
                new BigDecimal("1000.00")
        );

        BudgetResponse response =
                budgetService.updateBudget(
                        "sree@example.com",
                        7L,
                        request
                );

        assertEquals(
                new BigDecimal("6000.00"),
                response.getMonthlyLimit()
        );

        verify(
                budgetRepository,
                never()
        ).existsByUserIdAndCategoryAndMonthAndYear(
                anyLong(),
                any(ExpenseCategory.class),
                anyInt(),
                anyInt()
        );

        verify(monthlyInsightCacheService)
                .invalidateForBudgetChange(
                        5L,
                        YearMonth.of(2026, 9),
                        YearMonth.of(2026, 9)
                );
    }

    @Test
    void shouldRejectDuplicateWhenUpdatingBudgetIdentity() {

        User user = user();

        Budget existingBudget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                8,
                2026
        );

        BudgetRequest request = request(
                ExpenseCategory.TRAVEL,
                "3000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        7L,
                        5L
                )
        ).thenReturn(
                Optional.of(existingBudget)
        );

        when(
                budgetRepository
                        .existsByUserIdAndCategoryAndMonthAndYear(
                                5L,
                                ExpenseCategory.TRAVEL,
                                9,
                                2026
                        )
        ).thenReturn(true);

        BudgetAlreadyExistsException exception =
                assertThrows(
                        BudgetAlreadyExistsException.class,
                        () -> budgetService.updateBudget(
                                "sree@example.com",
                                7L,
                                request
                        )
                );

        assertEquals(
                "Budget already exists for this category and month",
                exception.getMessage()
        );

        verify(
                budgetRepository,
                never()
        ).save(any(Budget.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldDeleteBudgetAndInvalidateAffectedMonth() {

        User user = user();

        Budget existingBudget = budget(
                user,
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        7L,
                        5L
                )
        ).thenReturn(
                Optional.of(existingBudget)
        );

        budgetService.deleteBudget(
                "sree@example.com",
                7L
        );

        verify(budgetRepository)
                .delete(existingBudget);

        verify(monthlyInsightCacheService)
                .invalidateForBudgetChange(
                        5L,
                        YearMonth.of(2026, 9)
                );
    }

    @Test
    void shouldRejectDeleteWhenBudgetDoesNotExistForUser() {

        User user = user();

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(
                budgetRepository.findOwnedBudget(
                        999L,
                        5L
                )
        ).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> budgetService.deleteBudget(
                                "sree@example.com",
                                999L
                        )
                );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        verify(
                budgetRepository,
                never()
        ).delete(any(Budget.class));

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    @Test
    void shouldRejectOperationWhenUserDoesNotExist() {

        when(
                userRepository.findByEmail(
                        "missing@example.com"
                )
        ).thenReturn(Optional.empty());

        BudgetRequest request = request(
                ExpenseCategory.FOOD,
                "5000.00",
                9,
                2026
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> budgetService.createBudget(
                                "  MISSING@EXAMPLE.COM ",
                                request
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                budgetRepository
        );

        verifyNoInteractions(
                expenseRepository
        );

        verifyNoInteractions(
                monthlyInsightCacheService
        );
    }

    private BudgetRequest request(
            ExpenseCategory category,
            String monthlyLimit,
            int month,
            int year
    ) {

        BudgetRequest request =
                new BudgetRequest();

        request.setCategory(category);
        request.setMonthlyLimit(
                new BigDecimal(monthlyLimit)
        );
        request.setMonth(month);
        request.setYear(year);

        return request;
    }

    private User user() {

        User user =
                mock(User.class);

        when(user.getId())
                .thenReturn(5L);

        return user;
    }

    private Budget budget(
            User user,
            ExpenseCategory category,
            String monthlyLimit,
            int month,
            int year
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        return new Budget(
                user,
                category,
                new BigDecimal(monthlyLimit),
                month,
                year,
                now,
                now
        );
    }
}