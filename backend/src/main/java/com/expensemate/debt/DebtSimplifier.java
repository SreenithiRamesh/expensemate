package com.expensemate.debt;

import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.debt.DebtSimplificationResponse;

import java.util.List;

public interface DebtSimplifier {

    DebtSimplificationResponse simplify(
            Long groupId,
            List<MemberBalanceResponse> balances
    );
}