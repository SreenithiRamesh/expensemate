package com.expensemate.dto.ai;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;

public record CategoryInsightData(

        ExpenseCategory category,

        BigDecimal amount,

        BigDecimal percentage

) {
}