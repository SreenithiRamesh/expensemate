package com.expensemate.controller;

import com.expensemate.dto.ai.AiExpenseCategorizationRequest;
import com.expensemate.dto.ai.AiExpenseSuggestionResponse;
import com.expensemate.service.ai.AiExpenseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/expenses")
public class AiExpenseController {

    private final AiExpenseService aiExpenseService;

    public AiExpenseController(
            AiExpenseService aiExpenseService
    ) {
        this.aiExpenseService = aiExpenseService;
    }

    @PostMapping("/categorize")
    public AiExpenseSuggestionResponse categorize(
            @Valid @RequestBody
            AiExpenseCategorizationRequest request,
            Authentication authentication
    ) {

        return aiExpenseService.categorize(
                authentication.getName(),
                request.text()
        );
    }
}