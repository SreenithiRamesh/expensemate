package com.expensemate.dto.dashboard;

import java.util.List;

public record DashboardResponse(
        String month,
        MonthlySpendingResponse monthlySpending,
        List<CategorySpendingResponse> categoryBreakdown,
        List<MonthlyTrendResponse> spendingTrend,
        List<BudgetStatusResponse> budgetStatus,
        List<DashboardGroupBalanceResponse> groupBalances,
        List<DashboardActivityResponse> recentActivity
) {
}