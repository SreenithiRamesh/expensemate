package com.expensemate.strategy;

import java.math.BigDecimal;

public record SplitResult(
        Long userId,
        BigDecimal shareAmount,
        BigDecimal percentage
) {
}