package com.expensemate.dto.ai;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;

public record BudgetInsightData(

        ExpenseCategory category,

        BigDecimal monthlyLimit,

        BigDecimal spent,

        BigDecimal remaining,

        BigDecimal percentageUsed,

        String status

) {
}