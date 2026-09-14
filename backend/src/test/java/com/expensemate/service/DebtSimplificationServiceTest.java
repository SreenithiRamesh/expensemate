package com.expensemate.service;

import com.expensemate.debt.DebtSimplifier;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtSimplificationServiceTest {

    @Mock
    private BalanceService balanceService;

    @Mock
    private DebtSimplifier debtSimplifier;

    private DebtSimplificationService service;

    @BeforeEach
    void setUp() {

        service =
                new DebtSimplificationService(
                        balanceService,
                        debtSimplifier
                );
    }

    @Test
    void shouldDelegateBalancesToDebtSimplifier() {

        Long groupId = 2L;
        String email =
                "sree@example.com";

        List<MemberBalanceResponse> balances =
                List.of(
                        new MemberBalanceResponse(
                                1L,
                                "Sree",
                                new BigDecimal(
                                        "800.00"
                                )
                        ),
                        new MemberBalanceResponse(
                                2L,
                                "Test User",
                                new BigDecimal(
                                        "-800.00"
                                )
                        )
                );

        GroupBalanceResponse balanceResponse =
                new GroupBalanceResponse(
                        groupId,
                        balances,
                        List.of()
                );

        DebtSimplificationResponse expected =
                new DebtSimplificationResponse(
                        groupId,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(
                balanceService.getGroupBalances(
                        groupId,
                        email
                )
        ).thenReturn(balanceResponse);

        when(
                debtSimplifier.simplify(
                        groupId,
                        balances
                )
        ).thenReturn(expected);

        DebtSimplificationResponse actual =
                service.simplifyGroupDebt(
                        groupId,
                        email
                );

        assertEquals(
                expected,
                actual
        );

        verify(
                balanceService
        ).getGroupBalances(
                groupId,
                email
        );

        verify(
                debtSimplifier
        ).simplify(
                groupId,
                balances
        );
    }

    @Test
    void shouldPassEmptyBalancesToSimplifier() {

        Long groupId = 5L;
        String email =
                "sree@example.com";

        List<MemberBalanceResponse> balances =
                List.of();

        GroupBalanceResponse balanceResponse =
                new GroupBalanceResponse(
                        groupId,
                        balances,
                        List.of()
                );

        DebtSimplificationResponse expected =
                new DebtSimplificationResponse(
                        groupId,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(
                balanceService.getGroupBalances(
                        groupId,
                        email
                )
        ).thenReturn(balanceResponse);

        when(
                debtSimplifier.simplify(
                        groupId,
                        balances
                )
        ).thenReturn(expected);

        DebtSimplificationResponse actual =
                service.simplifyGroupDebt(
                        groupId,
                        email
                );

        assertEquals(
                expected,
                actual
        );
    }
}