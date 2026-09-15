package com.expensemate.service;

import com.expensemate.dto.BudgetRequest;
import com.expensemate.dto.BudgetResponse;
import com.expensemate.entity.Budget;
import com.expensemate.entity.User;
import com.expensemate.exception.BudgetAlreadyExistsException;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.BudgetRepository;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.ai.MonthlyInsightCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final PersonalExpenseRepository expenseRepository;
    private final MonthlyInsightCacheService monthlyInsightCacheService;

    public BudgetService(
            BudgetRepository budgetRepository,
            UserRepository userRepository,
            PersonalExpenseRepository expenseRepository,
            MonthlyInsightCacheService monthlyInsightCacheService
    ) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.monthlyInsightCacheService =
                monthlyInsightCacheService;
    }

    @Transactional
    public BudgetResponse createBudget(
            String email,
            BudgetRequest request
    ) {

        User user =
                getUserByEmail(email);

        boolean exists =
                budgetRepository
                        .existsByUserIdAndCategoryAndMonthAndYear(
                                user.getId(),
                                request.getCategory(),
                                request.getMonth(),
                                request.getYear()
                        );

        if (exists) {
            throw new BudgetAlreadyExistsException(
                    "Budget already exists for this category and month"
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        Budget budget =
                new Budget(
                        user,
                        request.getCategory(),
                        request.getMonthlyLimit(),
                        request.getMonth(),
                        request.getYear(),
                        now,
                        now
                );

        Budget saved =
                budgetRepository.save(budget);

        /*
         * The newly created budget changes the
         * monthly insight for this month.
         */
        YearMonth budgetMonth =
                YearMonth.of(
                        saved.getYear(),
                        saved.getMonth()
                );

        monthlyInsightCacheService
                .invalidateForBudgetChange(
                        user.getId(),
                        budgetMonth
                );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(
            String email,
            Long budgetId
    ) {

        User user =
                getUserByEmail(email);

        Budget budget =
                budgetRepository
                        .findOwnedBudget(
                                budgetId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Budget not found"
                                )
                        );

        return toResponse(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(
            String email,
            Integer month,
            Integer year
    ) {

        User user =
                getUserByEmail(email);

        /*
         * Business rule:
         * month and year must either both be provided
         * or both be absent.
         */
        if ((month == null && year != null)
                || (month != null && year == null)) {

            throw new InvalidRequestException(
                    "Month and year must be provided together"
            );
        }

        /*
         * Protect against invalid query values
         * such as month=15.
         */
        if (month != null
                && (month < 1 || month > 12)) {

            throw new InvalidRequestException(
                    "Month must be between 1 and 12"
            );
        }

        if (year != null && year < 2000) {

            throw new InvalidRequestException(
                    "Year must be 2000 or later"
            );
        }

        List<Budget> budgets;

        if (month != null && year != null) {

            budgets =
                    budgetRepository
                            .findAllForUserAndPeriod(
                                    user.getId(),
                                    month,
                                    year
                            );

        } else {

            budgets =
                    budgetRepository
                            .findAllForUser(
                                    user.getId()
                            );
        }

        return budgets.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse updateBudget(
            String email,
            Long budgetId,
            BudgetRequest request
    ) {

        User user =
                getUserByEmail(email);

        Budget budget =
                budgetRepository
                        .findOwnedBudget(
                                budgetId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Budget not found"
                                )
                        );

        /*
         * Capture the old budget period BEFORE
         * modifying month or year.
         *
         * If a budget moves:
         *
         * August -> September
         *
         * both cached monthly insights become stale.
         */
        YearMonth oldBudgetMonth =
                YearMonth.of(
                        budget.getYear(),
                        budget.getMonth()
                );

        /*
         * A duplicate check is only required if the
         * category/month/year combination changes.
         */
        boolean changedIdentity =
                budget.getCategory()
                        != request.getCategory()
                        || !budget.getMonth()
                        .equals(request.getMonth())
                        || !budget.getYear()
                        .equals(request.getYear());

        if (changedIdentity) {

            boolean duplicate =
                    budgetRepository
                            .existsByUserIdAndCategoryAndMonthAndYear(
                                    user.getId(),
                                    request.getCategory(),
                                    request.getMonth(),
                                    request.getYear()
                            );

            if (duplicate) {
                throw new BudgetAlreadyExistsException(
                        "Budget already exists for this category and month"
                );
            }
        }

        budget.setCategory(
                request.getCategory()
        );

        budget.setMonthlyLimit(
                request.getMonthlyLimit()
        );

        budget.setMonth(
                request.getMonth()
        );

        budget.setYear(
                request.getYear()
        );

        budget.setUpdatedAt(
                LocalDateTime.now()
        );

        Budget updated =
                budgetRepository.save(budget);

        YearMonth newBudgetMonth =
                YearMonth.of(
                        updated.getYear(),
                        updated.getMonth()
                );

        /*
         * If old and new months are identical,
         * the cache service deduplicates the month.
         *
         * If the period changed, both old and
         * new monthly insights are invalidated.
         */
        monthlyInsightCacheService
                .invalidateForBudgetChange(
                        user.getId(),
                        oldBudgetMonth,
                        newBudgetMonth
                );

        return toResponse(updated);
    }

    @Transactional
    public void deleteBudget(
            String email,
            Long budgetId
    ) {

        User user =
                getUserByEmail(email);

        Budget budget =
                budgetRepository
                        .findOwnedBudget(
                                budgetId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Budget not found"
                                )
                        );

        /*
         * Capture the budget period before deletion
         * so the correct cached insight can be removed.
         */
        YearMonth budgetMonth =
                YearMonth.of(
                        budget.getYear(),
                        budget.getMonth()
                );

        budgetRepository.delete(budget);

        monthlyInsightCacheService
                .invalidateForBudgetChange(
                        user.getId(),
                        budgetMonth
                );
    }

    private BudgetResponse toResponse(
            Budget budget
    ) {

        YearMonth yearMonth =
                YearMonth.of(
                        budget.getYear(),
                        budget.getMonth()
                );

        LocalDate startDate =
                yearMonth.atDay(1);

        LocalDate endDate =
                yearMonth.atEndOfMonth();

        /*
         * Expense records are the source of truth.
         * Spending is calculated directly from
         * personal_expenses.
         */
        BigDecimal spent =
                expenseRepository.calculateTotalSpent(
                        budget.getUser().getId(),
                        budget.getCategory(),
                        startDate,
                        endDate
                );

        /*
         * Defensive fallback in case the aggregate
         * query unexpectedly returns null.
         */
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        BigDecimal remaining =
                budget.getMonthlyLimit()
                        .subtract(spent);

        BigDecimal percentageUsed =
                spent
                        .multiply(
                                BigDecimal.valueOf(100)
                        )
                        .divide(
                                budget.getMonthlyLimit(),
                                2,
                                RoundingMode.HALF_UP
                        );

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory(),
                budget.getMonthlyLimit(),
                budget.getMonth(),
                budget.getYear(),
                spent,
                remaining,
                percentageUsed,
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }

    private User getUserByEmail(
            String email
    ) {

        String normalizedEmail =
                email.trim().toLowerCase();

        return userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }
}