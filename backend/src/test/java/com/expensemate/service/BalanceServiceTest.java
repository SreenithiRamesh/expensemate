package com.expensemate.service;

import com.expensemate.balance.BalanceCalculator;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.ExpenseSplitRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SharedExpenseRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BalanceCalculator balanceCalculator;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(
                sharedExpenseRepository,
                expenseSplitRepository,
                expenseGroupRepository,
                groupMemberRepository,
                userRepository,
                balanceCalculator
        );
    }

    @Test
    void shouldReturnCalculatedGroupBalancesForValidMember() {

        Long groupId = 2L;
        String email = "sree@example.com";

        User user = mock(User.class);
        ExpenseGroup group = mock(ExpenseGroup.class);

        SharedExpense expense = mock(SharedExpense.class);
        ExpenseSplit split = mock(ExpenseSplit.class);

        List<SharedExpense> expenses =
                List.of(expense);

        List<ExpenseSplit> splits =
                List.of(split);

        GroupBalanceResponse expectedResponse =
                new GroupBalanceResponse(
                        groupId,
                        List.of(),
                        List.of()
                );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(user.getId())
                .thenReturn(1L);

        when(expenseGroupRepository.findById(groupId))
                .thenReturn(Optional.of(group));

        when(
                groupMemberRepository.existsByGroupIdAndUserId(
                        groupId,
                        1L
                )
        ).thenReturn(true);

        when(sharedExpenseRepository.findByGroup_Id(groupId))
                .thenReturn(expenses);

        when(expenseSplitRepository.findByExpense_Group_Id(groupId))
                .thenReturn(splits);

        when(
                balanceCalculator.calculate(
                        groupId,
                        expenses,
                        splits
                )
        ).thenReturn(expectedResponse);

        GroupBalanceResponse actualResponse =
                balanceService.getGroupBalances(
                        groupId,
                        email
                );

        assertEquals(
                expectedResponse,
                actualResponse
        );

        verify(balanceCalculator).calculate(
                groupId,
                expenses,
                splits
        );
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {

        String email = "missing@example.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        balanceService.getGroupBalances(
                                2L,
                                email
                        )
        );

        verifyNoInteractions(
                expenseGroupRepository,
                groupMemberRepository,
                sharedExpenseRepository,
                expenseSplitRepository,
                balanceCalculator
        );
    }

    @Test
    void shouldThrowWhenGroupDoesNotExist() {

        Long groupId = 999L;
        String email = "sree@example.com";

        User user = mock(User.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(expenseGroupRepository.findById(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        balanceService.getGroupBalances(
                                groupId,
                                email
                        )
        );

        verifyNoInteractions(
                groupMemberRepository,
                sharedExpenseRepository,
                expenseSplitRepository,
                balanceCalculator
        );
    }

    @Test
    void shouldThrowWhenUserIsNotGroupMember() {

        Long groupId = 2L;
        String email = "outsider@example.com";

        User user = mock(User.class);
        ExpenseGroup group = mock(ExpenseGroup.class);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(user.getId())
                .thenReturn(5L);

        when(expenseGroupRepository.findById(groupId))
                .thenReturn(Optional.of(group));

        when(
                groupMemberRepository.existsByGroupIdAndUserId(
                        groupId,
                        5L
                )
        ).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        balanceService.getGroupBalances(
                                groupId,
                                email
                        )
        );

        verifyNoInteractions(
                sharedExpenseRepository,
                expenseSplitRepository,
                balanceCalculator
        );
    }

    @Test
    void shouldDelegateEmptyExpenseListsToCalculator() {

        Long groupId = 2L;
        String email = "sree@example.com";

        User user = mock(User.class);
        ExpenseGroup group = mock(ExpenseGroup.class);

        List<SharedExpense> expenses =
                List.of();

        List<ExpenseSplit> splits =
                List.of();

        GroupBalanceResponse expectedResponse =
                new GroupBalanceResponse(
                        groupId,
                        List.of(),
                        List.of()
                );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(user.getId())
                .thenReturn(1L);

        when(expenseGroupRepository.findById(groupId))
                .thenReturn(Optional.of(group));

        when(
                groupMemberRepository.existsByGroupIdAndUserId(
                        groupId,
                        1L
                )
        ).thenReturn(true);

        when(sharedExpenseRepository.findByGroup_Id(groupId))
                .thenReturn(expenses);

        when(expenseSplitRepository.findByExpense_Group_Id(groupId))
                .thenReturn(splits);

        when(
                balanceCalculator.calculate(
                        groupId,
                        expenses,
                        splits
                )
        ).thenReturn(expectedResponse);

        GroupBalanceResponse response =
                balanceService.getGroupBalances(
                        groupId,
                        email
                );

        assertEquals(
                expectedResponse,
                response
        );
    }
}