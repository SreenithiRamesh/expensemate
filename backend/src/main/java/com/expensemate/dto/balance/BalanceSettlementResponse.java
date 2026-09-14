// BalanceSettlementResponse.java
package com.expensemate.dto.balance;

import java.math.BigDecimal;

public record BalanceSettlementResponse(
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        BigDecimal amount
) {
}