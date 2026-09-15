package com.expensemate.dto.dashboard;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;

public record BudgetStatusResponse(
        ExpenseCategory category,
        BigDecimal monthlyLimit,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        String status
) {
}