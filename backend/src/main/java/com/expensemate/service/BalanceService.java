package com.expensemate.service;

import com.expensemate.balance.BalanceCalculator;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.ExpenseSplitRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SharedExpenseRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BalanceService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final BalanceCalculator balanceCalculator;

    public BalanceService(
            SharedExpenseRepository sharedExpenseRepository,
            ExpenseSplitRepository expenseSplitRepository,
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            BalanceCalculator balanceCalculator
    ) {
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.balanceCalculator = balanceCalculator;
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse getGroupBalances(
            Long groupId,
            String email
    ) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        expenseGroupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        boolean isMember =
                groupMemberRepository.existsByGroupIdAndUserId(
                        groupId,
                        currentUser.getId()
                );

        if (!isMember) {
            throw new ResourceNotFoundException("Group not found");
        }

        List<SharedExpense> expenses =
                sharedExpenseRepository.findByGroup_Id(groupId);

        List<ExpenseSplit> splits =
                expenseSplitRepository.findByExpense_Group_Id(groupId);

        return balanceCalculator.calculate(
                groupId,
                expenses,
                splits
        );
    }
}