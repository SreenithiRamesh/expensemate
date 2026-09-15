package com.expensemate.service.ai;

import com.expensemate.dto.ai.ExpenseCategorizationResult;

import java.time.LocalDate;

public interface ExpenseCategorizer {

    ExpenseCategorizationResult categorize(
            String text,
            LocalDate currentDate
    );
}