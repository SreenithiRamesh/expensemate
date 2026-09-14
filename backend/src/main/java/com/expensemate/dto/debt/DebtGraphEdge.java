package com.expensemate.dto.debt;

import java.math.BigDecimal;

public record DebtGraphEdge(
        Long fromUserId,
        Long toUserId,
        BigDecimal amount
) {
}