package com.expensemate.dto.dashboard;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;

public record CategorySpendingResponse(
        ExpenseCategory category,
        BigDecimal amount,
        BigDecimal percentage
) {
}