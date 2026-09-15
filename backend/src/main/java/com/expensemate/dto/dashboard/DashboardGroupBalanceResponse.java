package com.expensemate.dto.dashboard;

import java.math.BigDecimal;

public record DashboardGroupBalanceResponse(
        Long groupId,
        String groupName,
        BigDecimal netBalance,
        String status
) {
}