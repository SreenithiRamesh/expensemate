package com.expensemate.service.ai;

import com.expensemate.dto.ai.BudgetInsightData;
import com.expensemate.dto.ai.CategoryInsightData;
import com.expensemate.dto.ai.MonthlyInsightData;
import com.expensemate.dto.ai.MonthlyInsightResponse;
import com.expensemate.dto.dashboard.DashboardResponse;
import com.expensemate.dto.dashboard.MonthlyTrendResponse;
import com.expensemate.entity.MonthlyInsightCache;
import com.expensemate.entity.User;
import com.expensemate.exception.AiServiceException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.MonthlyInsightCacheRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

@Service
public class MonthlyInsightServiceImpl
        implements MonthlyInsightService {

    private final DashboardService dashboardService;
    private final InsightNarrator insightNarrator;
    private final MonthlyInsightCacheRepository cacheRepository;
    private final UserRepository userRepository;

    public MonthlyInsightServiceImpl(
            DashboardService dashboardService,
            InsightNarrator insightNarrator,
            MonthlyInsightCacheRepository cacheRepository,
            UserRepository userRepository
    ) {
        this.dashboardService = dashboardService;
        this.insightNarrator = insightNarrator;
        this.cacheRepository = cacheRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MonthlyInsightResponse getMonthlyInsight(
            String currentUserEmail,
            YearMonth month
    ) {

        YearMonth selectedMonth =
                month == null
                        ? YearMonth.now()
                        : month;

        User currentUser =
                getCurrentUser(currentUserEmail);

        String monthKey =
                selectedMonth.toString();

        return cacheRepository
                .findByUserIdAndInsightMonth(
                        currentUser.getId(),
                        monthKey
                )
                .map(cache ->
                        new MonthlyInsightResponse(
                                selectedMonth,
                                cache.getInsight(),
                                true
                        )
                )
                .orElseGet(() ->
                        generateInsight(
                                currentUser,
                                currentUserEmail,
                                selectedMonth
                        )
                );
    }

    private MonthlyInsightResponse generateInsight(
            User currentUser,
            String currentUserEmail,
            YearMonth month
    ) {

        DashboardResponse dashboard =
                dashboardService.getDashboard(
                        currentUserEmail,
                        month
                );

        MonthlyInsightData data =
                buildInsightData(
                        dashboard,
                        month
                );

        String insight =
                insightNarrator.narrate(data);

        /*
         * Defensive protection.
         *
         * The real Gemini implementation already rejects
         * blank responses, but the business service should
         * also protect itself from any future narrator
         * implementation returning invalid output.
         */
        if (insight == null || insight.isBlank()) {
            throw new AiServiceException(
                    "AI monthly insight returned an empty response"
            );
        }

        String normalizedInsight =
                insight.trim();

        MonthlyInsightCache cache =
                new MonthlyInsightCache(
                        currentUser,
                        month.toString(),
                        normalizedInsight
                );

        cacheRepository.save(cache);

        return new MonthlyInsightResponse(
                month,
                normalizedInsight,
                false
        );
    }

    private MonthlyInsightData buildInsightData(
            DashboardResponse dashboard,
            YearMonth month
    ) {

        BigDecimal totalSpending =
                money(
                        dashboard
                                .monthlySpending()
                                .totalSpent()
                );

        BigDecimal previousMonthSpending =
                findPreviousMonthSpending(
                        dashboard,
                        month
                );

        /*
         * Positive = spending increased.
         * Negative = spending decreased.
         *
         * This calculation happens in Java.
         * Gemini is never responsible for computing it.
         */
        BigDecimal spendingChange =
                money(
                        totalSpending.subtract(
                                previousMonthSpending
                        )
                );

        var categorySpending =
                dashboard
                        .categoryBreakdown()
                        .stream()
                        .map(category ->
                                new CategoryInsightData(
                                        category.category(),
                                        money(
                                                category.amount()
                                        ),
                                        percentage(
                                                category.percentage()
                                        )
                                )
                        )
                        .toList();

        var budgets =
                dashboard
                        .budgetStatus()
                        .stream()
                        .map(budget ->
                                new BudgetInsightData(
                                        budget.category(),
                                        money(
                                                budget.monthlyLimit()
                                        ),
                                        money(
                                                budget.spent()
                                        ),
                                        money(
                                                budget.remaining()
                                        ),
                                        percentage(
                                                budget.percentageUsed()
                                        ),
                                        budget.status()
                                )
                        )
                        .toList();

        return new MonthlyInsightData(
                month,
                totalSpending,
                previousMonthSpending,
                spendingChange,
                categorySpending,
                budgets
        );
    }

    private BigDecimal findPreviousMonthSpending(
            DashboardResponse dashboard,
            YearMonth month
    ) {

        String previousMonth =
                month
                        .minusMonths(1)
                        .toString();

        return dashboard
                .spendingTrend()
                .stream()
                .filter(trend ->
                        previousMonth.equals(
                                trend.month()
                        )
                )
                .map(
                        MonthlyTrendResponse::amount
                )
                .findFirst()
                .map(this::money)
                .orElseGet(this::zero);
    }

    private User getCurrentUser(
            String email
    ) {

        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase();

        return userRepository
                .findByEmail(
                        normalizedEmail
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private BigDecimal money(
            BigDecimal value
    ) {

        if (value == null) {
            return zero();
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal percentage(
            BigDecimal value
    ) {

        if (value == null) {
            return zero();
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal zero() {

        return BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}