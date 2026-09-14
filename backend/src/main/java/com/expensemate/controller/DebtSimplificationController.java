package com.expensemate.controller;

import com.expensemate.dto.debt.DebtSimplificationResponse;
import com.expensemate.service.DebtSimplificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/groups/{groupId}/debt-simplification"
)
public class DebtSimplificationController {

    private final DebtSimplificationService
            debtSimplificationService;

    public DebtSimplificationController(
            DebtSimplificationService
                    debtSimplificationService
    ) {
        this.debtSimplificationService =
                debtSimplificationService;
    }

    @GetMapping
    public DebtSimplificationResponse simplifyDebt(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        return debtSimplificationService
                .simplifyGroupDebt(
                        groupId,
                        email
                );
    }
}