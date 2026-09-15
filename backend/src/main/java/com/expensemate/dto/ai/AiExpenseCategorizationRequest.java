package com.expensemate.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiExpenseCategorizationRequest(

        @NotBlank(message = "Expense text is required")
        @Size(
                max = 500,
                message = "Expense text must not exceed 500 characters"
        )
        String text
) {
}