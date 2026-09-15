package com.expensemate.dto.dashboard;

import java.math.BigDecimal;

public record MonthlyTrendResponse(
        String month,
        BigDecimal amount
) {
}