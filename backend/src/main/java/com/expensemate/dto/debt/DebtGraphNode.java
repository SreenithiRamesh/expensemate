package com.expensemate.dto.debt;

import java.math.BigDecimal;

public record DebtGraphNode(
        Long userId,
        String name,
        BigDecimal netBalance,
        String role
) {
}