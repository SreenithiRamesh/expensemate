package com.expensemate.controller;

import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.service.BalanceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/balances")
public class BalanceController {

    private final BalanceService balanceService;

    public BalanceController(
            BalanceService balanceService
    ) {
        this.balanceService = balanceService;
    }

    @GetMapping
    public GroupBalanceResponse getGroupBalances(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return balanceService.getGroupBalances(
                groupId,
                email
        );
    }
}