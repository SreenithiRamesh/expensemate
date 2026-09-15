package com.expensemate.dto.dashboard;

import java.math.BigDecimal;

public record MonthlySpendingResponse(
        BigDecimal totalSpent,
        long transactionCount
) {
}