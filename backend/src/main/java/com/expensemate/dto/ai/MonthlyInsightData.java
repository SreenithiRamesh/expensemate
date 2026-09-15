package com.expensemate.dto.ai;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyInsightData(

        YearMonth month,

        BigDecimal totalSpending,

        BigDecimal previousMonthSpending,

        BigDecimal spendingChange,

        List<CategoryInsightData> categorySpending,

        List<BudgetInsightData> budgets

) {
}