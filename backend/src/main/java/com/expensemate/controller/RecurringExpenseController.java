package com.expensemate.controller;

import com.expensemate.dto.RecurringExpenseRequest;
import com.expensemate.dto.RecurringExpenseResponse;
import com.expensemate.dto.RecurringPaymentRequest;
import com.expensemate.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(
            RecurringExpenseService recurringExpenseService
    ) {
        this.recurringExpenseService =
                recurringExpenseService;
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse>
    createRecurringExpense(
            Authentication authentication,
            @Valid
            @RequestBody
            RecurringExpenseRequest request
    ) {
        RecurringExpenseResponse response =
                recurringExpenseService
                        .createRecurringExpense(
                                authentication.getName(),
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse>
    getRecurringExpenseById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                recurringExpenseService
                        .getRecurringExpenseById(
                                authentication.getName(),
                                id
                        )
        );
    }

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>>
    getRecurringExpenses(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                recurringExpenseService
                        .getRecurringExpenses(
                                authentication.getName()
                        )
        );
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<RecurringExpenseResponse>>
    getUpcomingBills(
            Authentication authentication,
            @RequestParam(
                    required = false,
                    defaultValue = "30"
            )
            Integer days
    ) {
        return ResponseEntity.ok(
                recurringExpenseService
                        .getUpcomingBills(
                                authentication.getName(),
                                days
                        )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse>
    updateRecurringExpense(
            Authentication authentication,
            @PathVariable Long id,
            @Valid
            @RequestBody
            RecurringExpenseRequest request
    ) {
        return ResponseEntity.ok(
                recurringExpenseService
                        .updateRecurringExpense(
                                authentication.getName(),
                                id,
                                request
                        )
        );
    }

    @PostMapping("/{id}/record-payment")
    public ResponseEntity<RecurringExpenseResponse>
    recordPayment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid
            @RequestBody
            RecurringPaymentRequest request
    ) {
        return ResponseEntity.ok(
                recurringExpenseService
                        .recordPayment(
                                authentication.getName(),
                                id,
                                request
                        )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    deleteRecurringExpense(
            Authentication authentication,
            @PathVariable Long id
    ) {
        recurringExpenseService
                .deleteRecurringExpense(
                        authentication.getName(),
                        id
                );

        return ResponseEntity.noContent().build();
    }
}