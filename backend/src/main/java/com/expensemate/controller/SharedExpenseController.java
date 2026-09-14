package com.expensemate.controller;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.service.SharedExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
public class SharedExpenseController {

    private final SharedExpenseService sharedExpenseService;

    public SharedExpenseController(
            SharedExpenseService sharedExpenseService
    ) {
        this.sharedExpenseService = sharedExpenseService;
    }

    @PostMapping
    public ResponseEntity<SharedExpenseResponse>
    createSharedExpense(
            @PathVariable Long groupId,
            @Valid
            @RequestBody SharedExpenseCreateRequest request,
            Authentication authentication
    ) {
        SharedExpenseResponse response =
                sharedExpenseService
                        .createSharedExpense(
                                groupId,
                                authentication.getName(),
                                request
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SharedExpenseResponse>>
    getGroupExpenses(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        List<SharedExpenseResponse> response =
                sharedExpenseService
                        .getGroupExpenses(
                                groupId,
                                authentication.getName()
                        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<SharedExpenseResponse>
    getSharedExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            Authentication authentication
    ) {
        SharedExpenseResponse response =
                sharedExpenseService
                        .getSharedExpense(
                                groupId,
                                expenseId,
                                authentication.getName()
                        );

        return ResponseEntity.ok(response);
    }
}