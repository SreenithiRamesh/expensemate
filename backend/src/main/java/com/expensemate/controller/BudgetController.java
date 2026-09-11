package com.expensemate.controller;

import com.expensemate.dto.BudgetRequest;
import com.expensemate.dto.BudgetResponse;
import com.expensemate.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(
            BudgetService budgetService
    ) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request
    ) {

        BudgetResponse response =
                budgetService.createBudget(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                budgetService.getBudgetById(
                        authentication.getName(),
                        id
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            Authentication authentication,
            @RequestParam(required = false)
            Integer month,
            @RequestParam(required = false)
            Integer year
    ) {

        return ResponseEntity.ok(
                budgetService.getBudgets(
                        authentication.getName(),
                        month,
                        year
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request
    ) {

        return ResponseEntity.ok(
                budgetService.updateBudget(
                        authentication.getName(),
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            Authentication authentication,
            @PathVariable Long id
    ) {

        budgetService.deleteBudget(
                authentication.getName(),
                id
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}