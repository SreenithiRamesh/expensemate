package com.expensemate.dto.debt;

import java.math.BigDecimal;

public record DebtSettlementSuggestion(
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        BigDecimal amount
) {
}