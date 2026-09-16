package com.expensemate.controller;

import com.expensemate.dto.PersonalExpenseRequest;
import com.expensemate.dto.PersonalExpenseResponse;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.service.PersonalExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
@Validated
public class PersonalExpenseController {

    private final PersonalExpenseService expenseService;

    public PersonalExpenseController(
            PersonalExpenseService expenseService
    ) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<PersonalExpenseResponse> createExpense(
            Authentication authentication,
            @Valid @RequestBody PersonalExpenseRequest request
    ) {

        PersonalExpenseResponse response =
                expenseService.createExpense(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonalExpenseResponse> getExpenseById(
            Authentication authentication,
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                expenseService.getExpenseById(
                        authentication.getName(),
                        id
                )
        );
    }

    @GetMapping
    public ResponseEntity<Page<PersonalExpenseResponse>> getExpenses(
            Authentication authentication,

            @RequestParam(required = false)
            ExpenseCategory category,

            @RequestParam(required = false)
            LocalDate startDate,

            @RequestParam(required = false)
            LocalDate endDate,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "0")
            @Min(
                    value = 0,
                    message = "Page must be zero or greater"
            )
            int page,

            @RequestParam(defaultValue = "10")
            @Min(
                    value = 1,
                    message = "Size must be at least 1"
            )
            @Max(
                    value = 100,
                    message = "Size must not exceed 100"
            )
            int size
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "expenseDate"
                ).and(
                        Sort.by(
                                Sort.Direction.DESC,
                                "id"
                        )
                )
        );

        return ResponseEntity.ok(
                expenseService.getExpenses(
                        authentication.getName(),
                        category,
                        startDate,
                        endDate,
                        search,
                        pageable
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonalExpenseResponse> updateExpense(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PersonalExpenseRequest request
    ) {

        return ResponseEntity.ok(
                expenseService.updateExpense(
                        authentication.getName(),
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(
            Authentication authentication,
            @PathVariable Long id
    ) {

        expenseService.deleteExpense(
                authentication.getName(),
                id
        );

        return ResponseEntity.noContent().build();
    }
}