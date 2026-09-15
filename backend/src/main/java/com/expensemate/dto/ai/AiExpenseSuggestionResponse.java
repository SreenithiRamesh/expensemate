package com.expensemate.dto.ai;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiExpenseSuggestionResponse(
        BigDecimal amount,
        ExpenseCategory category,
        String description,
        LocalDate expenseDate,
        boolean requiresReview,
        int remainingRequests
) {
}