package com.expensemate.service.ai;

import com.expensemate.dto.ai.AiExpenseSuggestionResponse;
import com.expensemate.dto.ai.ExpenseCategorizationResult;
import com.expensemate.entity.User;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AiExpenseServiceImpl implements AiExpenseService {

    private final UserRepository userRepository;
    private final ExpenseCategorizer expenseCategorizer;
    private final AiUsageService aiUsageService;

    public AiExpenseServiceImpl(
            UserRepository userRepository,
            ExpenseCategorizer expenseCategorizer,
            AiUsageService aiUsageService
    ) {
        this.userRepository = userRepository;
        this.expenseCategorizer = expenseCategorizer;
        this.aiUsageService = aiUsageService;
    }

    @Override
    public AiExpenseSuggestionResponse categorize(
            String currentUserEmail,
            String text
    ) {

        User user = userRepository
                .findByEmail(currentUserEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        aiUsageService.verifyLimit(user.getId());

        ExpenseCategorizationResult result =
                expenseCategorizer.categorize(
                        text.trim(),
                        LocalDate.now()
                );

        int remainingRequests =
                aiUsageService.recordSuccessfulRequest(
                        user.getId()
                );

        return new AiExpenseSuggestionResponse(
                result.amount(),
                result.category(),
                result.description(),
                result.expenseDate(),
                true,
                remainingRequests
        );
    }
}