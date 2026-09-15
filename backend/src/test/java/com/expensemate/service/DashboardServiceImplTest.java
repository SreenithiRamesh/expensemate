package com.expensemate.service;

import com.expensemate.dto.BudgetResponse;
import com.expensemate.dto.GroupSummaryResponse;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.dashboard.DashboardResponse;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupActivity;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.GroupActivityRepository;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.repository.projection.CategorySpendingProjection;
import com.expensemate.repository.projection.MonthlyTrendProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private PersonalExpenseRepository personalExpenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private GroupService groupService;

    @Mock
    private BalanceService balanceService;

    @Mock
    private GroupActivityRepository groupActivityRepository;

    private DashboardServiceImpl dashboardService;

    private User currentUser;

    @BeforeEach
    void setUp() {

        dashboardService =
                new DashboardServiceImpl(
                        personalExpenseRepository,
                        userRepository,
                        budgetService,
                        groupService,
                        balanceService,
                        groupActivityRepository
                );

        /*
         * Only create the mock here.
         *
         * Do not globally stub getId() or getName().
         * Mockito strict mode reports unused global
         * stubbings as UnnecessaryStubbingException.
         */
        currentUser = mock(User.class);
    }

    @Test
    void shouldBuildMonthlySpendingSummary() {

        mockCurrentUser();

        when(personalExpenseRepository.sumExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(
                new BigDecimal("1250.50")
        );

        when(personalExpenseRepository.countExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(4L);

        mockEmptyDashboardDependencies();

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                "2026-09",
                response.month()
        );

        assertEquals(
                new BigDecimal("1250.50"),
                response.monthlySpending()
                        .totalSpent()
        );

        assertEquals(
                4L,
                response.monthlySpending()
                        .transactionCount()
        );
    }

    @Test
    void shouldReturnZeroWhenNoPersonalExpensesExist() {

        mockCurrentUser();

        when(personalExpenseRepository.sumExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(BigDecimal.ZERO);

        when(personalExpenseRepository.countExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(0L);

        mockEmptyDashboardDependencies();

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                new BigDecimal("0.00"),
                response.monthlySpending()
                        .totalSpent()
        );

        assertEquals(
                0L,
                response.monthlySpending()
                        .transactionCount()
        );

        assertTrue(
                response.categoryBreakdown()
                        .isEmpty()
        );
    }

    @Test
    void shouldCalculateCategoryPercentages() {

        mockCurrentUser();

        CategorySpendingProjection food =
                mock(CategorySpendingProjection.class);

        CategorySpendingProjection travel =
                mock(CategorySpendingProjection.class);

        when(food.getCategory())
                .thenReturn(ExpenseCategory.FOOD);

        when(food.getAmount())
                .thenReturn(
                        new BigDecimal("600.00")
                );

        when(travel.getCategory())
                .thenReturn(ExpenseCategory.TRAVEL);

        when(travel.getAmount())
                .thenReturn(
                        new BigDecimal("400.00")
                );

        when(personalExpenseRepository.sumExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(
                new BigDecimal("1000.00")
        );

        when(personalExpenseRepository.countExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(2L);

        when(personalExpenseRepository.findCategorySpending(
                eq(1L),
                any(),
                any()
        )).thenReturn(
                List.of(food, travel)
        );

        when(personalExpenseRepository.findMonthlyTrend(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());

        when(groupService.getGroupsForUser(
                anyString()
        )).thenReturn(List.of());

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(List.of());

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                2,
                response.categoryBreakdown()
                        .size()
        );

        assertEquals(
                new BigDecimal("60.00"),
                response.categoryBreakdown()
                        .get(0)
                        .percentage()
        );

        assertEquals(
                new BigDecimal("40.00"),
                response.categoryBreakdown()
                        .get(1)
                        .percentage()
        );
    }

    @Test
    void shouldReturnSixContinuousTrendMonths() {

        mockCurrentUser();

        MonthlyTrendProjection july =
                mock(MonthlyTrendProjection.class);

        MonthlyTrendProjection september =
                mock(MonthlyTrendProjection.class);

        when(july.getYear())
                .thenReturn(2026);

        when(july.getMonth())
                .thenReturn(7);

        when(july.getAmount())
                .thenReturn(
                        new BigDecimal("700.00")
                );

        when(september.getYear())
                .thenReturn(2026);

        when(september.getMonth())
                .thenReturn(9);

        when(september.getAmount())
                .thenReturn(
                        new BigDecimal("900.00")
                );

        when(personalExpenseRepository.sumExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(BigDecimal.ZERO);

        when(personalExpenseRepository.countExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(0L);

        when(personalExpenseRepository.findCategorySpending(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(personalExpenseRepository.findMonthlyTrend(
                eq(1L),
                any(),
                any()
        )).thenReturn(
                List.of(july, september)
        );

        when(budgetService.getBudgets(
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());

        when(groupService.getGroupsForUser(
                anyString()
        )).thenReturn(List.of());

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(List.of());

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                6,
                response.spendingTrend()
                        .size()
        );

        assertEquals(
                "2026-04",
                response.spendingTrend()
                        .get(0)
                        .month()
        );

        assertEquals(
                "2026-09",
                response.spendingTrend()
                        .get(5)
                        .month()
        );

        /*
         * April, May, June, July, August, September.
         * Therefore July is index 3.
         */
        assertEquals(
                new BigDecimal("700.00"),
                response.spendingTrend()
                        .get(3)
                        .amount()
        );

        /*
         * August was absent from the repository result,
         * therefore DashboardService should fill it
         * with 0.00.
         */
        assertEquals(
                new BigDecimal("0.00"),
                response.spendingTrend()
                        .get(4)
                        .amount()
        );

        assertEquals(
                new BigDecimal("900.00"),
                response.spendingTrend()
                        .get(5)
                        .amount()
        );
    }

    @Test
    void shouldDetermineBudgetStatuses() {

        mockCurrentUser();

        BudgetResponse within =
                createBudgetResponse(
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("50.00")
                );

        BudgetResponse near =
                createBudgetResponse(
                        new BigDecimal("1000.00"),
                        new BigDecimal("850.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("85.00")
                );

        BudgetResponse exceeded =
                createBudgetResponse(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1100.00"),
                        new BigDecimal("-100.00"),
                        new BigDecimal("110.00")
                );

        mockPersonalExpenseDefaults();

        when(budgetService.getBudgets(
                "sree@example.com",
                9,
                2026
        )).thenReturn(
                List.of(
                        within,
                        near,
                        exceeded
                )
        );

        when(groupService.getGroupsForUser(
                anyString()
        )).thenReturn(List.of());

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(List.of());

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                "WITHIN_BUDGET",
                response.budgetStatus()
                        .get(0)
                        .status()
        );

        assertEquals(
                "NEAR_LIMIT",
                response.budgetStatus()
                        .get(1)
                        .status()
        );

        assertEquals(
                "EXCEEDED",
                response.budgetStatus()
                        .get(2)
                        .status()
        );
    }

    @Test
    void shouldComposeCurrentUserGroupBalance() {

        mockCurrentUser();
        mockPersonalExpenseDefaults();

        GroupSummaryResponse group =
                new GroupSummaryResponse(
                        3L,
                        "Trip",
                        "Trip expenses",
                        1L,
                        "Sree",
                        2L,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        when(groupService.getGroupsForUser(
                "sree@example.com"
        )).thenReturn(
                List.of(group)
        );

        GroupBalanceResponse groupBalance =
                new GroupBalanceResponse(
                        3L,
                        List.of(
                                new MemberBalanceResponse(
                                        1L,
                                        "Sree",
                                        new BigDecimal("450.00")
                                ),
                                new MemberBalanceResponse(
                                        2L,
                                        "Test User",
                                        new BigDecimal("-450.00")
                                )
                        ),
                        List.of()
                );

        when(balanceService.getGroupBalances(
                3L,
                "sree@example.com"
        )).thenReturn(groupBalance);

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(List.of());

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                1,
                response.groupBalances()
                        .size()
        );

        assertEquals(
                3L,
                response.groupBalances()
                        .get(0)
                        .groupId()
        );

        assertEquals(
                "Trip",
                response.groupBalances()
                        .get(0)
                        .groupName()
        );

        assertEquals(
                new BigDecimal("450.00"),
                response.groupBalances()
                        .get(0)
                        .netBalance()
        );

        assertEquals(
                "GETS_BACK",
                response.groupBalances()
                        .get(0)
                        .status()
        );
    }

    @Test
    void shouldReturnRecentAccessibleActivity() {

        mockCurrentUser();
        mockPersonalExpenseDefaults();

        /*
         * getName() is needed only for activity mapping,
         * so stub it only in this test.
         */
        when(currentUser.getName())
                .thenReturn("Sree");

        when(groupService.getGroupsForUser(
                anyString()
        )).thenReturn(List.of());

        ExpenseGroup group =
                mock(ExpenseGroup.class);

        when(group.getId())
                .thenReturn(3L);

        when(group.getName())
                .thenReturn("Trip");

        GroupActivity activity =
                new GroupActivity(
                        group,
                        currentUser,
                        ActivityType.SHARED_EXPENSE_CREATED,
                        "Sree added expense",
                        10L
                );

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(
                List.of(activity)
        );

        DashboardResponse response =
                dashboardService.getDashboard(
                        "sree@example.com",
                        YearMonth.of(2026, 9)
                );

        assertEquals(
                1,
                response.recentActivity()
                        .size()
        );

        assertEquals(
                ActivityType.SHARED_EXPENSE_CREATED,
                response.recentActivity()
                        .get(0)
                        .activityType()
        );

        assertEquals(
                "Trip",
                response.recentActivity()
                        .get(0)
                        .groupName()
        );

        assertEquals(
                "Sree",
                response.recentActivity()
                        .get(0)
                        .actorName()
        );

        assertEquals(
                "Sree added expense",
                response.recentActivity()
                        .get(0)
                        .description()
        );

        assertEquals(
                10L,
                response.recentActivity()
                        .get(0)
                        .referenceId()
        );
    }

    @Test
    void shouldThrowWhenCurrentUserDoesNotExist() {

        when(userRepository.findByEmail(
                "missing@example.com"
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        dashboardService.getDashboard(
                                "missing@example.com",
                                YearMonth.of(
                                        2026,
                                        9
                                )
                        )
        );

        verifyNoInteractions(
                personalExpenseRepository,
                budgetService,
                groupService,
                balanceService,
                groupActivityRepository
        );
    }

    /*
     * ---------------------------------------------------------
     * Helper methods
     * ---------------------------------------------------------
     */

    private void mockCurrentUser() {

        when(userRepository.findByEmail(
                "sree@example.com"
        )).thenReturn(
                Optional.of(currentUser)
        );

        /*
         * Dashboard aggregation needs the authenticated
         * user's ID for personal expenses, balances and
         * recent group activity.
         */
        when(currentUser.getId())
                .thenReturn(1L);
    }

    private void mockEmptyDashboardDependencies() {

        when(personalExpenseRepository.findCategorySpending(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(personalExpenseRepository.findMonthlyTrend(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());

        when(groupService.getGroupsForUser(
                anyString()
        )).thenReturn(List.of());

        when(groupActivityRepository.findRecentActivityForUser(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(List.of());
    }

    private void mockPersonalExpenseDefaults() {

        when(personalExpenseRepository.sumExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(BigDecimal.ZERO);

        when(personalExpenseRepository.countExpensesForPeriod(
                eq(1L),
                any(),
                any()
        )).thenReturn(0L);

        when(personalExpenseRepository.findCategorySpending(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(personalExpenseRepository.findMonthlyTrend(
                eq(1L),
                any(),
                any()
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                anyString(),
                anyInt(),
                anyInt()
        )).thenReturn(List.of());
    }

    private BudgetResponse createBudgetResponse(
            BigDecimal limit,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal percentage
    ) {

        return new BudgetResponse(
                1L,
                ExpenseCategory.FOOD,
                limit,
                9,
                2026,
                spent,
                remaining,
                percentage,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}