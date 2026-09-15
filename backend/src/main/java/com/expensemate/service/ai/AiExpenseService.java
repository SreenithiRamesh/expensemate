package com.expensemate.service.ai;

import com.expensemate.dto.ai.AiExpenseSuggestionResponse;

public interface AiExpenseService {

    AiExpenseSuggestionResponse categorize(
            String currentUserEmail,
            String text
    );
}