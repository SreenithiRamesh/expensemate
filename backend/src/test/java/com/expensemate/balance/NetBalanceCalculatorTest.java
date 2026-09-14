package com.expensemate.balance;

import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetBalanceCalculatorTest {

    private NetBalanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NetBalanceCalculator();
    }

    @Test
    void shouldCalculateSingleCreditorAndDebtor() {

        User sree = createUser(
                1L,
                "Sree"
        );

        User testUser = createUser(
                2L,
                "Test User"
        );

        SharedExpense expense =
                createExpense(
                        sree,
                        "1000.00"
                );

        ExpenseSplit sreeSplit =
                createSplit(
                        sree,
                        "600.00"
                );

        ExpenseSplit testUserSplit =
                createSplit(
                        testUser,
                        "400.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        2L,
                        List.of(expense),
                        List.of(
                                sreeSplit,
                                testUserSplit
                        )
                );

        assertEquals(
                2,
                response.memberBalances().size()
        );

        assertEquals(
                1,
                response.settlements().size()
        );

        var settlement =
                response.settlements().get(0);

        assertEquals(
                2L,
                settlement.fromUserId()
        );

        assertEquals(
                "Test User",
                settlement.fromUserName()
        );

        assertEquals(
                1L,
                settlement.toUserId()
        );

        assertEquals(
                "Sree",
                settlement.toUserName()
        );

        assertBigDecimalEquals(
                "400.00",
                settlement.amount()
        );
    }

    @Test
    void shouldCalculateBalancesAcrossMultipleExpenses() {

        User user1 = createUser(
                1L,
                "User 1"
        );

        User user2 = createUser(
                2L,
                "User 2"
        );

        User user3 = createUser(
                3L,
                "User 3"
        );

        SharedExpense hotel =
                createExpense(
                        user1,
                        "900.00"
                );

        SharedExpense food =
                createExpense(
                        user2,
                        "300.00"
                );

        ExpenseSplit hotelUser1 =
                createSplit(
                        user1,
                        "300.00"
                );

        ExpenseSplit hotelUser2 =
                createSplit(
                        user2,
                        "300.00"
                );

        ExpenseSplit hotelUser3 =
                createSplit(
                        user3,
                        "300.00"
                );

        ExpenseSplit foodUser1 =
                createSplit(
                        user1,
                        "100.00"
                );

        ExpenseSplit foodUser2 =
                createSplit(
                        user2,
                        "100.00"
                );

        ExpenseSplit foodUser3 =
                createSplit(
                        user3,
                        "100.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        1L,
                        List.of(
                                hotel,
                                food
                        ),
                        List.of(
                                hotelUser1,
                                hotelUser2,
                                hotelUser3,
                                foodUser1,
                                foodUser2,
                                foodUser3
                        )
                );

        assertEquals(
                3,
                response.memberBalances().size()
        );

        BigDecimal totalNetBalance =
                response.memberBalances()
                        .stream()
                        .map(balance ->
                                balance.netBalance()
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        assertBigDecimalEquals(
                "0.00",
                totalNetBalance
        );

        /*
         * User 1:
         * Paid = 900
         * Own shares = 300 + 100 = 400
         * Net = +500
         *
         * User 2:
         * Paid = 300
         * Own shares = 300 + 100 = 400
         * Net = -100
         *
         * User 3:
         * Paid = 0
         * Own shares = 300 + 100 = 400
         * Net = -400
         *
         * Therefore total debt = 500
         * and User 1 receives 500.
         */

        BigDecimal totalSettlements =
                response.settlements()
                        .stream()
                        .map(settlement ->
                                settlement.amount()
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        assertBigDecimalEquals(
                "500.00",
                totalSettlements
        );
    }

    @Test
    void shouldReturnZeroBalanceWhenUserPaidExactlyOwnShare() {

        User sree = createUser(
                1L,
                "Sree"
        );

        SharedExpense expense =
                createExpense(
                        sree,
                        "500.00"
                );

        ExpenseSplit split =
                createSplit(
                        sree,
                        "500.00"
                );

        GroupBalanceResponse response =
                calculator.calculate(
                        1L,
                        List.of(expense),
                        List.of(split)
                );

        assertEquals(
                1,
                response.memberBalances().size()
        );

        assertBigDecimalEquals(
                "0.00",
                response.memberBalances()
                        .get(0)
                        .netBalance()
        );

        assertEquals(
                0,
                response.settlements().size()
        );
    }

    @Test
    void shouldReturnEmptyBalancesForNoExpensesAndNoSplits() {

        GroupBalanceResponse response =
                calculator.calculate(
                        10L,
                        List.of(),
                        List.of()
                );

        assertEquals(
                10L,
                response.groupId()
        );

        assertEquals(
                0,
                response.memberBalances().size()
        );

        assertEquals(
                0,
                response.settlements().size()
        );
    }

    @Test
    void shouldCalculateExpectedBalanceFromM9Scenario() {

        User sree = createUser(
                1L,
                "Sree"
        );

        User testUser = createUser(
                2L,
                "Test User"
        );

        SharedExpense dinner =
                createExpense(
                        sree,
                        "100.00"
                );

        SharedExpense hotel =
                createExpense(
                        sree,
                        "1000.00"
                );

        SharedExpense cab =
                createExpense(
                        sree,
                        "1000.00"
                );

        List<ExpenseSplit> splits =
                List.of(
                        createSplit(
                                sree,
                                "50.00"
                        ),
                        createSplit(
                                testUser,
                                "50.00"
                        ),

                        createSplit(
                                sree,
                                "600.00"
                        ),
                        createSplit(
                                testUser,
                                "400.00"
                        ),

                        createSplit(
                                sree,
                                "650.00"
                        ),
                        createSplit(
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
                        splits
                );

        var sreeBalance =
                response.memberBalances()
                        .stream()
                        .filter(balance ->
                                balance.userId().equals(1L)
                        )
                        .findFirst()
                        .orElseThrow();

        var testUserBalance =
                response.memberBalances()
                        .stream()
                        .filter(balance ->
                                balance.userId().equals(2L)
                        )
                        .findFirst()
                        .orElseThrow();

        assertBigDecimalEquals(
                "800.00",
                sreeBalance.netBalance()
        );

        assertBigDecimalEquals(
                "-800.00",
                testUserBalance.netBalance()
        );

        assertEquals(
                1,
                response.settlements().size()
        );

        var settlement =
                response.settlements().get(0);

        assertEquals(
                2L,
                settlement.fromUserId()
        );

        assertEquals(
                1L,
                settlement.toUserId()
        );

        assertBigDecimalEquals(
                "800.00",
                settlement.amount()
        );
    }

    private User createUser(
            Long id,
            String name
    ) {

        User user = mock(User.class);

        when(
                user.getId()
        ).thenReturn(id);

        when(
                user.getName()
        ).thenReturn(name);

        return user;
    }

    private SharedExpense createExpense(
            User paidBy,
            String amount
    ) {

        SharedExpense expense =
                mock(SharedExpense.class);

        when(
                expense.getPaidBy()
        ).thenReturn(paidBy);

        when(
                expense.getAmount()
        ).thenReturn(
                new BigDecimal(amount)
        );

        return expense;
    }

    private ExpenseSplit createSplit(
            User user,
            String shareAmount
    ) {

        ExpenseSplit split =
                mock(ExpenseSplit.class);

        when(
                split.getUser()
        ).thenReturn(user);

        when(
                split.getShareAmount()
        ).thenReturn(
                new BigDecimal(shareAmount)
        );

        return split;
    }

    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual
    ) {

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}