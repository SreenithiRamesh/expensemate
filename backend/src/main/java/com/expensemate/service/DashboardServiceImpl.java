package com.expensemate.service;

import com.expensemate.dto.BudgetResponse;
import com.expensemate.dto.GroupSummaryResponse;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.dashboard.BudgetStatusResponse;
import com.expensemate.dto.dashboard.CategorySpendingResponse;
import com.expensemate.dto.dashboard.DashboardActivityResponse;
import com.expensemate.dto.dashboard.DashboardGroupBalanceResponse;
import com.expensemate.dto.dashboard.DashboardResponse;
import com.expensemate.dto.dashboard.MonthlySpendingResponse;
import com.expensemate.dto.dashboard.MonthlyTrendResponse;
import com.expensemate.entity.GroupActivity;
import com.expensemate.entity.User;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.GroupActivityRepository;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.repository.projection.CategorySpendingProjection;
import com.expensemate.repository.projection.MonthlyTrendProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private static final BigDecimal NEAR_LIMIT_PERCENTAGE =
            new BigDecimal("80");

    private static final int TREND_MONTHS = 6;

    private static final int RECENT_ACTIVITY_LIMIT = 10;

    private final PersonalExpenseRepository personalExpenseRepository;
    private final UserRepository userRepository;
    private final BudgetService budgetService;
    private final GroupService groupService;
    private final BalanceService balanceService;
    private final GroupActivityRepository groupActivityRepository;

    public DashboardServiceImpl(
            PersonalExpenseRepository personalExpenseRepository,
            UserRepository userRepository,
            BudgetService budgetService,
            GroupService groupService,
            BalanceService balanceService,
            GroupActivityRepository groupActivityRepository
    ) {
        this.personalExpenseRepository = personalExpenseRepository;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
        this.groupService = groupService;
        this.balanceService = balanceService;
        this.groupActivityRepository = groupActivityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(
            String currentUserEmail,
            YearMonth month
    ) {

        User currentUser =
                getCurrentUser(currentUserEmail);

        YearMonth selectedMonth =
                month == null
                        ? YearMonth.now()
                        : month;

        MonthlySpendingResponse monthlySpending =
                buildMonthlySpending(
                        currentUser.getId(),
                        selectedMonth
                );

        List<CategorySpendingResponse> categoryBreakdown =
                buildCategoryBreakdown(
                        currentUser.getId(),
                        selectedMonth,
                        monthlySpending.totalSpent()
                );

        List<MonthlyTrendResponse> spendingTrend =
                buildSpendingTrend(
                        currentUser.getId(),
                        selectedMonth
                );

        List<BudgetStatusResponse> budgetStatus =
                buildBudgetStatus(
                        currentUserEmail,
                        selectedMonth
                );

        List<DashboardGroupBalanceResponse> groupBalances =
                buildGroupBalances(
                        currentUserEmail,
                        currentUser.getId()
                );

        List<DashboardActivityResponse> recentActivity =
                buildRecentActivity(
                        currentUser.getId()
                );

        return new DashboardResponse(
                selectedMonth.toString(),
                monthlySpending,
                categoryBreakdown,
                spendingTrend,
                budgetStatus,
                groupBalances,
                recentActivity
        );
    }

    private MonthlySpendingResponse buildMonthlySpending(
            Long userId,
            YearMonth month
    ) {

        LocalDate startDate =
                month.atDay(1);

        LocalDate endDate =
                month.plusMonths(1)
                        .atDay(1);

        BigDecimal totalSpent =
                personalExpenseRepository
                        .sumExpensesForPeriod(
                                userId,
                                startDate,
                                endDate
                        );

        long transactionCount =
                personalExpenseRepository
                        .countExpensesForPeriod(
                                userId,
                                startDate,
                                endDate
                        );

        return new MonthlySpendingResponse(
                money(totalSpent),
                transactionCount
        );
    }

    private List<CategorySpendingResponse> buildCategoryBreakdown(
            Long userId,
            YearMonth month,
            BigDecimal totalSpent
    ) {

        LocalDate startDate =
                month.atDay(1);

        LocalDate endDate =
                month.plusMonths(1)
                        .atDay(1);

        List<CategorySpendingProjection> projections =
                personalExpenseRepository
                        .findCategorySpending(
                                userId,
                                startDate,
                                endDate
                        );

        return projections.stream()
                .map(projection ->
                        new CategorySpendingResponse(
                                projection.getCategory(),
                                money(
                                        projection.getAmount()
                                ),
                                calculatePercentage(
                                        projection.getAmount(),
                                        totalSpent
                                )
                        )
                )
                .toList();
    }

    private List<MonthlyTrendResponse> buildSpendingTrend(
            Long userId,
            YearMonth selectedMonth
    ) {

        YearMonth firstMonth =
                selectedMonth.minusMonths(
                        TREND_MONTHS - 1
                );

        LocalDate startDate =
                firstMonth.atDay(1);

        LocalDate endDate =
                selectedMonth
                        .plusMonths(1)
                        .atDay(1);

        List<MonthlyTrendProjection> projections =
                personalExpenseRepository
                        .findMonthlyTrend(
                                userId,
                                startDate,
                                endDate
                        );

        Map<YearMonth, BigDecimal> totals =
                new HashMap<>();

        for (MonthlyTrendProjection projection : projections) {

            YearMonth projectionMonth =
                    YearMonth.of(
                            projection.getYear(),
                            projection.getMonth()
                    );

            totals.put(
                    projectionMonth,
                    money(
                            projection.getAmount()
                    )
            );
        }

        List<MonthlyTrendResponse> trend =
                new ArrayList<>();

        for (int index = 0;
             index < TREND_MONTHS;
             index++) {

            YearMonth currentMonth =
                    firstMonth.plusMonths(index);

            trend.add(
                    new MonthlyTrendResponse(
                            currentMonth.toString(),
                            totals.getOrDefault(
                                    currentMonth,
                                    zero()
                            )
                    )
            );
        }

        return trend;
    }

    private List<BudgetStatusResponse> buildBudgetStatus(
            String email,
            YearMonth month
    ) {

        List<BudgetResponse> budgets =
                budgetService.getBudgets(
                        email,
                        month.getMonthValue(),
                        month.getYear()
                );

        return budgets.stream()
                .map(budget ->
                        new BudgetStatusResponse(
                                budget.getCategory(),
                                money(
                                        budget.getMonthlyLimit()
                                ),
                                money(
                                        budget.getSpent()
                                ),
                                money(
                                        budget.getRemaining()
                                ),
                                percentage(
                                        budget.getPercentageUsed()
                                ),
                                determineBudgetStatus(
                                        budget.getPercentageUsed()
                                )
                        )
                )
                .toList();
    }

    private List<DashboardGroupBalanceResponse> buildGroupBalances(
            String email,
            Long currentUserId
    ) {

        /*
         * Reuse GroupService instead of querying group membership
         * independently inside DashboardService.
         */
        List<GroupSummaryResponse> groups =
                groupService.getGroupsForUser(
                        email
                );

        List<DashboardGroupBalanceResponse> result =
                new ArrayList<>();

        for (GroupSummaryResponse group : groups) {

            GroupBalanceResponse groupBalance =
                    balanceService.getGroupBalances(
                            group.getId(),
                            email
                    );

            MemberBalanceResponse currentUserBalance =
                    groupBalance.memberBalances()
                            .stream()
                            .filter(member ->
                                    member.userId()
                                            .equals(
                                                    currentUserId
                                            )
                            )
                            .findFirst()
                            .orElse(null);

            BigDecimal netBalance =
                    currentUserBalance == null
                            ? zero()
                            : money(
                            currentUserBalance
                                    .netBalance()
                    );

            result.add(
                    new DashboardGroupBalanceResponse(
                            group.getId(),
                            group.getName(),
                            netBalance,
                            determineBalanceStatus(
                                    netBalance
                            )
                    )
            );
        }

        return result;
    }

    private List<DashboardActivityResponse> buildRecentActivity(
            Long currentUserId
    ) {

        List<GroupActivity> activities =
                groupActivityRepository
                        .findRecentActivityForUser(
                                currentUserId,
                                PageRequest.of(
                                        0,
                                        RECENT_ACTIVITY_LIMIT
                                )
                        );

        return activities.stream()
                .map(activity ->
                        new DashboardActivityResponse(
                                activity.getId(),
                                activity.getGroup()
                                        .getId(),
                                activity.getGroup()
                                        .getName(),
                                activity.getActor()
                                        .getName(),
                                activity.getActivityType(),
                                activity.getDescription(),
                                activity.getReferenceId(),
                                activity.getCreatedAt()
                        )
                )
                .toList();
    }

    private BigDecimal calculatePercentage(
            BigDecimal amount,
            BigDecimal total
    ) {

        if (amount == null
                || total == null
                || total.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            return zero();
        }

        return amount
                .multiply(ONE_HUNDRED)
                .divide(
                        total,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private String determineBudgetStatus(
            BigDecimal percentageUsed
    ) {

        if (percentageUsed == null) {
            return "WITHIN_BUDGET";
        }

        if (percentageUsed.compareTo(
                ONE_HUNDRED
        ) > 0) {

            return "EXCEEDED";
        }

        if (percentageUsed.compareTo(
                NEAR_LIMIT_PERCENTAGE
        ) >= 0) {

            return "NEAR_LIMIT";
        }

        return "WITHIN_BUDGET";
    }

    private String determineBalanceStatus(
            BigDecimal netBalance
    ) {

        int comparison =
                netBalance.compareTo(
                        BigDecimal.ZERO
                );

        if (comparison > 0) {
            return "GETS_BACK";
        }

        if (comparison < 0) {
            return "OWES";
        }

        return "SETTLED";
    }

    private User getCurrentUser(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        String normalizedEmail =
                email.trim()
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