package com.expensemate.balance;

import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetBalanceCalculatorTest {

    private NetBalanceCalculator calculator;

    @BeforeEach
    void setUp() {

        calculator =
                new NetBalanceCalculator();
    }

    @Test
    void shouldCalculateSingleCreditorAndDebtor() {

        User sree =
                user(
                        1L,
                        "Sree"
                );

        User testUser =
                user(
                        2L,
                        "Test User"
                );

        SharedExpense expense =
                expense(
                        sree,
                        "1000.00"
                );

        ExpenseSplit split1 =
                split(
                        sree,
                        "600.00"
                );

        ExpenseSplit split2 =
                split(
                        testUser,
                        "400.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        2L,
                        List.of(expense),
                        List.of(
                                split1,
                                split2
                        ),
                        List.of()
                );

        assertMoney(
                "400.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertMoney(
                "-400.00",
                balanceOf(
                        response,
                        2L
                )
        );

        assertEquals(
                1,
                response
                        .settlements()
                        .size()
        );

        assertMoney(
                "400.00",
                response
                        .settlements()
                        .get(0)
                        .amount()
        );
    }

    @Test
    void shouldApplyPartialSettlementToBalances() {

        User sree =
                user(
                        1L,
                        "Sree"
                );

        User testUser =
                user(
                        2L,
                        "Test User"
                );

        SharedExpense expense =
                expense(
                        sree,
                        "1000.00"
                );

        ExpenseSplit sreeSplit =
                split(
                        sree,
                        "200.00"
                );

        ExpenseSplit testSplit =
                split(
                        testUser,
                        "800.00"
                );

        Settlement settlement =
                settlement(
                        testUser,
                        sree,
                        "300.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        2L,
                        List.of(expense),
                        List.of(
                                sreeSplit,
                                testSplit
                        ),
                        List.of(
                                settlement
                        )
                );

        assertMoney(
                "500.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertMoney(
                "-500.00",
                balanceOf(
                        response,
                        2L
                )
        );

        assertEquals(
                1,
                response
                        .settlements()
                        .size()
        );

        assertMoney(
                "500.00",
                response
                        .settlements()
                        .get(0)
                        .amount()
        );
    }

    @Test
    void shouldBecomeZeroAfterFullSettlement() {

        User sree =
                user(
                        1L,
                        "Sree"
                );

        User testUser =
                user(
                        2L,
                        "Test User"
                );

        SharedExpense expense =
                expense(
                        sree,
                        "1000.00"
                );

        ExpenseSplit sreeSplit =
                split(
                        sree,
                        "200.00"
                );

        ExpenseSplit testSplit =
                split(
                        testUser,
                        "800.00"
                );

        Settlement settlement =
                settlement(
                        testUser,
                        sree,
                        "800.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        2L,
                        List.of(expense),
                        List.of(
                                sreeSplit,
                                testSplit
                        ),
                        List.of(
                                settlement
                        )
                );

        assertMoney(
                "0.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertMoney(
                "0.00",
                balanceOf(
                        response,
                        2L
                )
        );

        assertTrue(
                response
                        .settlements()
                        .isEmpty()
        );
    }

    @Test
    void shouldCalculateMultipleExpenses() {

        User user1 =
                user(
                        1L,
                        "User 1"
                );

        User user2 =
                user(
                        2L,
                        "User 2"
                );

        User user3 =
                user(
                        3L,
                        "User 3"
                );

        SharedExpense expense1 =
                expense(
                        user1,
                        "900.00"
                );

        SharedExpense expense2 =
                expense(
                        user2,
                        "300.00"
                );

        List<ExpenseSplit> splits =
                List.of(
                        split(
                                user1,
                                "300.00"
                        ),
                        split(
                                user2,
                                "300.00"
                        ),
                        split(
                                user3,
                                "300.00"
                        ),
                        split(
                                user1,
                                "100.00"
                        ),
                        split(
                                user2,
                                "100.00"
                        ),
                        split(
                                user3,
                                "100.00"
                        )
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        1L,
                        List.of(
                                expense1,
                                expense2
                        ),
                        splits,
                        List.of()
                );

        assertMoney(
                "500.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertMoney(
                "-100.00",
                balanceOf(
                        response,
                        2L
                )
        );

        assertMoney(
                "-400.00",
                balanceOf(
                        response,
                        3L
                )
        );

        BigDecimal totalSettlement =
                response
                        .settlements()
                        .stream()
                        .map(
                                settlement ->
                                        settlement.amount()
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        assertMoney(
                "500.00",
                totalSettlement
        );
    }

    @Test
    void shouldReturnZeroWhenUserPaidExactlyOwnShare() {

        User user =
                user(
                        1L,
                        "User"
                );

        SharedExpense expense =
                expense(
                        user,
                        "500.00"
                );

        ExpenseSplit split =
                split(
                        user,
                        "500.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        1L,
                        List.of(
                                expense
                        ),
                        List.of(
                                split
                        ),
                        List.of()
                );

        assertMoney(
                "0.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertTrue(
                response
                        .settlements()
                        .isEmpty()
        );
    }

    @Test
    void shouldHandleEmptyData() {

        GroupBalanceResponse response =
                calculator.calculate(
                        1L,
                        List.of(),
                        List.of(),
                        List.of()
                );

        assertTrue(
                response
                        .memberBalances()
                        .isEmpty()
        );

        assertTrue(
                response
                        .settlements()
                        .isEmpty()
        );
    }

    @Test
    void shouldMatchRealM9Scenario() {

        User sree =
                user(
                        1L,
                        "Sree"
                );

        User testUser =
                user(
                        2L,
                        "Test User"
                );

        SharedExpense dinner =
                expense(
                        sree,
                        "100.00"
                );

        SharedExpense hotel =
                expense(
                        sree,
                        "1000.00"
                );

        SharedExpense cab =
                expense(
                        sree,
                        "1000.00"
                );

        List<ExpenseSplit> splits =
                List.of(
                        split(
                                sree,
                                "50.00"
                        ),
                        split(
                                testUser,
                                "50.00"
                        ),

                        split(
                                sree,
                                "600.00"
                        ),
                        split(
                                testUser,
                                "400.00"
                        ),

                        split(
                                sree,
                                "650.00"
                        ),
                        split(
                                testUser,
                                "350.00"
                        )
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        2L,
                        List.of(
                                dinner,
                                hotel,
                                cab
                        ),
                        splits,
                        List.of()
                );

        assertMoney(
                "800.00",
                balanceOf(
                        response,
                        1L
                )
        );

        assertMoney(
                "-800.00",
                balanceOf(
                        response,
                        2L
                )
        );

        assertEquals(
                1,
                response
                        .settlements()
                        .size()
        );

        assertMoney(
                "800.00",
                response
                        .settlements()
                        .get(0)
                        .amount()
        );
    }

    private User user(
            Long id,
            String name
    ) {

        User user =
                mock(
                        User.class
                );

        when(
                user.getId()
        ).thenReturn(
                id
        );

        when(
                user.getName()
        ).thenReturn(
                name
        );

        return user;
    }

    private SharedExpense expense(
            User payer,
            String amount
    ) {

        SharedExpense expense =
                mock(
                        SharedExpense.class
                );

        when(
                expense.getPaidBy()
        ).thenReturn(
                payer
        );

        when(
                expense.getAmount()
        ).thenReturn(
                new BigDecimal(
                        amount
                )
        );

        return expense;
    }

    private ExpenseSplit split(
            User user,
            String amount
    ) {

        ExpenseSplit split =
                mock(
                        ExpenseSplit.class
                );

        when(
                split.getUser()
        ).thenReturn(
                user
        );

        when(
                split.getShareAmount()
        ).thenReturn(
                new BigDecimal(
                        amount
                )
        );

        return split;
    }

    private Settlement settlement(
            User fromUser,
            User toUser,
            String amount
    ) {

        Settlement settlement =
                mock(
                        Settlement.class
                );

        when(
                settlement.getFromUser()
        ).thenReturn(
                fromUser
        );

        when(
                settlement.getToUser()
        ).thenReturn(
                toUser
        );

        when(
                settlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        amount
                )
        );

        return settlement;
    }

    private BigDecimal balanceOf(
            GroupBalanceResponse response,
            Long userId
    ) {

        return response
                .memberBalances()
                .stream()
                .filter(
                        balance ->
                                balance
                                        .userId()
                                        .equals(
                                                userId
                                        )
                )
                .findFirst()
                .orElseThrow()
                .netBalance();
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

        assertEquals(
                0,
                new BigDecimal(
                        expected
                ).compareTo(
                        actual
                )
        );
    }
}