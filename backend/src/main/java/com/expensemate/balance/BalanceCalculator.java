package com.expensemate.balance;

import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.SharedExpense;

import java.util.List;

public interface BalanceCalculator {

    GroupBalanceResponse calculate(
            Long groupId,
            List<SharedExpense> expenses,
            List<ExpenseSplit> splits,
            List<Settlement> settlements
    );
}