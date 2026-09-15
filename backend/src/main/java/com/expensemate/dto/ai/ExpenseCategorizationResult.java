package com.expensemate.dto.ai;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseCategorizationResult(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate
) {
}