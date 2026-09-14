package com.expensemate.debt;

import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GreedyDebtSimplifierTest {

    private GreedyDebtSimplifier simplifier;

    @BeforeEach
    void setUp() {
        simplifier =
                new GreedyDebtSimplifier();
    }

    @Test
    void shouldSimplifySingleDebtorAndCreditor() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "Sree",
                                "800.00"
                        ),
                        balance(
                                2L,
                                "Test User",
                                "-800.00"
                        )
                );

        DebtSimplificationResponse response =
                simplifier.simplify(
                        2L,
                        balances
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

        assertMoney(
                "800.00",
                settlement.amount()
        );

        assertEquals(
                1,
                response.edges().size()
        );
    }

    @Test
    void shouldSimplifyMultipleDebtors() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "User 1",
                                "700.00"
                        ),
                        balance(
                                2L,
                                "User 2",
                                "-200.00"
                        ),
                        balance(
                                3L,
                                "User 3",
                                "-500.00"
                        )
                );

        DebtSimplificationResponse response =
                simplifier.simplify(
                        1L,
                        balances
                );

        assertEquals(
                2,
                response.settlements().size()
        );

        BigDecimal total =
                response.settlements()
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
                "700.00",
                total
        );
    }

    @Test
    void shouldHandleMultipleCreditorsAndDebtors() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "A",
                                "500.00"
                        ),
                        balance(
                                2L,
                                "B",
                                "200.00"
                        ),
                        balance(
                                3L,
                                "C",
                                "-400.00"
                        ),
                        balance(
                                4L,
                                "D",
                                "-300.00"
                        )
                );

        DebtSimplificationResponse response =
                simplifier.simplify(
                        10L,
                        balances
                );

        BigDecimal totalSettled =
                response.settlements()
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
                "700.00",
                totalSettled
        );

        assertTrue(
                response.settlements()
                        .size() <= 3
        );
    }

    @Test
    void shouldCreateGraphNodesWithCorrectRoles() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "Creditor",
                                "100.00"
                        ),
                        balance(
                                2L,
                                "Debtor",
                                "-100.00"
                        ),
                        balance(
                                3L,
                                "Settled",
                                "0.00"
                        )
                );

        DebtSimplificationResponse response =
                simplifier.simplify(
                        1L,
                        balances
                );

        assertEquals(
                3,
                response.nodes().size()
        );

        assertEquals(
                "CREDITOR",
                findRole(
                        response,
                        1L
                )
        );

        assertEquals(
                "DEBTOR",
                findRole(
                        response,
                        2L
                )
        );

        assertEquals(
                "SETTLED",
                findRole(
                        response,
                        3L
                )
        );
    }

    @Test
    void shouldReturnNoSettlementsWhenEveryoneIsSettled() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "A",
                                "0.00"
                        ),
                        balance(
                                2L,
                                "B",
                                "0.00"
                        )
                );

        DebtSimplificationResponse response =
                simplifier.simplify(
                        1L,
                        balances
                );

        assertTrue(
                response.settlements().isEmpty()
        );

        assertTrue(
                response.edges().isEmpty()
        );

        assertEquals(
                2,
                response.nodes().size()
        );
    }

    @Test
    void shouldRejectBalancesThatDoNotSumToZero() {

        List<MemberBalanceResponse> balances =
                List.of(
                        balance(
                                1L,
                                "A",
                                "500.00"
                        ),
                        balance(
                                2L,
                                "B",
                                "-400.00"
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                simplifier.simplify(
                                        1L,
                                        balances
                                )
                );

        assertEquals(
                "Group balances must sum to zero",
                exception.getMessage()
        );
    }

    private MemberBalanceResponse balance(
            Long id,
            String name,
            String amount
    ) {

        return new MemberBalanceResponse(
                id,
                name,
                new BigDecimal(amount)
        );
    }

    private String findRole(
            DebtSimplificationResponse response,
            Long userId
    ) {

        return response.nodes()
                .stream()
                .filter(
                        node ->
                                node.userId()
                                        .equals(userId)
                )
                .findFirst()
                .orElseThrow()
                .role();
    }

    private void assertMoney(
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