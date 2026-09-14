// MemberBalanceResponse.java
package com.expensemate.dto.balance;

import java.math.BigDecimal;

public record MemberBalanceResponse(
        Long userId,
        String name,
        BigDecimal netBalance
) {
}