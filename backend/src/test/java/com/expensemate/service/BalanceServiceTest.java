package com.expensemate.service;

import com.expensemate.balance.BalanceCalculator;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.*;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private SharedExpenseRepository
            sharedExpenseRepository;

    @Mock
    private ExpenseSplitRepository
            expenseSplitRepository;

    @Mock
    private ExpenseGroupRepository
            expenseGroupRepository;

    @Mock
    private GroupMemberRepository
            groupMemberRepository;

    @Mock
    private UserRepository
            userRepository;

    @Mock
    private SettlementRepository
            settlementRepository;

    @Mock
    private BalanceCalculator
            balanceCalculator;

    private BalanceService service;

    @BeforeEach
    void setUp() {

        service =
                new BalanceService(
                        sharedExpenseRepository,
                        expenseSplitRepository,
                        expenseGroupRepository,
                        groupMemberRepository,
                        userRepository,
                        settlementRepository,
                        balanceCalculator
                );
    }

    @Test
    void shouldDelegateGroupDataToCalculator() {

        Long groupId = 2L;

        String email =
                "sree@example.com";

        User user =
                mock(
                        User.class
                );

        ExpenseGroup group =
                mock(
                        ExpenseGroup.class
                );

        SharedExpense expense =
                mock(
                        SharedExpense.class
                );

        ExpenseSplit split =
                mock(
                        ExpenseSplit.class
                );

        Settlement settlement =
                mock(
                        Settlement.class
                );

        GroupBalanceResponse expected =
                new GroupBalanceResponse(
                        groupId,
                        List.of(),
                        List.of()
                );

        when(
                user.getId()
        ).thenReturn(
                1L
        );

        when(
                userRepository
                        .findByEmail(
                                email
                        )
        ).thenReturn(
                Optional.of(
                        user
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                groupId
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                sharedExpenseRepository
                        .findByGroup_Id(
                                groupId
                        )
        ).thenReturn(
                List.of(
                        expense
                )
        );

        when(
                expenseSplitRepository
                        .findByExpense_Group_Id(
                                groupId
                        )
        ).thenReturn(
                List.of(
                        split
                )
        );

        when(
                settlementRepository
                        .findByGroup_Id(
                                groupId
                        )
        ).thenReturn(
                List.of(
                        settlement
                )
        );

        when(
                balanceCalculator.calculate(
                        groupId,
                        List.of(expense),
                        List.of(split),
                        List.of(settlement)
                )
        ).thenReturn(
                expected
        );

        GroupBalanceResponse actual =
                service
                        .getGroupBalances(
                                groupId,
                                email
                        );

        assertEquals(
                expected,
                actual
        );

        verify(
                balanceCalculator
        ).calculate(
                groupId,
                List.of(expense),
                List.of(split),
                List.of(settlement)
        );
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {

        when(
                userRepository
                        .findByEmail(
                                "missing@example.com"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.getGroupBalances(
                                1L,
                                "missing@example.com"
                        )
        );

        verifyNoInteractions(
                balanceCalculator
        );
    }

    @Test
    void shouldThrowWhenGroupDoesNotExist() {

        User user =
                mock(
                        User.class
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        user
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                99L
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.getGroupBalances(
                                99L,
                                "sree@example.com"
                        )
        );

        verifyNoInteractions(
                balanceCalculator
        );
    }

    @Test
    void shouldThrowWhenUserIsNotMember() {

        User user =
                mock(
                        User.class
                );

        ExpenseGroup group =
                mock(
                        ExpenseGroup.class
                );

        when(
                user.getId()
        ).thenReturn(
                1L
        );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        user
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                false
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.getGroupBalances(
                                2L,
                                "sree@example.com"
                        )
        );

        verifyNoInteractions(
                sharedExpenseRepository,
                expenseSplitRepository,
                settlementRepository,
                balanceCalculator
        );
    }

    @Test
    void shouldDelegateEmptyLists() {

        User user =
                mock(
                        User.class
                );

        ExpenseGroup group =
                mock(
                        ExpenseGroup.class
                );

        when(
                user.getId()
        ).thenReturn(
                1L
        );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        user
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                1L,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                sharedExpenseRepository
                        .findByGroup_Id(
                                1L
                        )
        ).thenReturn(
                List.of()
        );

        when(
                expenseSplitRepository
                        .findByExpense_Group_Id(
                                1L
                        )
        ).thenReturn(
                List.of()
        );

        when(
                settlementRepository
                        .findByGroup_Id(
                                1L
                        )
        ).thenReturn(
                List.of()
        );

        GroupBalanceResponse expected =
                new GroupBalanceResponse(
                        1L,
                        List.of(),
                        List.of()
                );

        when(
                balanceCalculator.calculate(
                        1L,
                        List.of(),
                        List.of(),
                        List.of()
                )
        ).thenReturn(
                expected
        );

        GroupBalanceResponse actual =
                service
                        .getGroupBalances(
                                1L,
                                "sree@example.com"
                        );

        assertEquals(
                expected,
                actual
        );
    }
}