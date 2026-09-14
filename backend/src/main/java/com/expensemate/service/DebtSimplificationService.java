package com.expensemate.service;

import com.expensemate.debt.DebtSimplifier;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DebtSimplificationService {

    private final BalanceService balanceService;
    private final DebtSimplifier debtSimplifier;

    public DebtSimplificationService(
            BalanceService balanceService,
            DebtSimplifier debtSimplifier
    ) {
        this.balanceService = balanceService;
        this.debtSimplifier = debtSimplifier;
    }

    @Transactional(readOnly = true)
    public DebtSimplificationResponse simplifyGroupDebt(
            Long groupId,
            String email
    ) {

        GroupBalanceResponse balanceResponse =
                balanceService.getGroupBalances(
                        groupId,
                        email
                );

        return debtSimplifier.simplify(
                groupId,
                balanceResponse.memberBalances()
        );
    }
}