package com.expensemate.debt;

import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.dto.debt.DebtGraphEdge;
import com.expensemate.dto.debt.DebtGraphNode;
import com.expensemate.dto.debt.DebtSettlementSuggestion;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class GreedyDebtSimplifier implements DebtSimplifier {

    private static final BigDecimal ZERO =
            new BigDecimal("0.00");

    @Override
    public DebtSimplificationResponse simplify(
            Long groupId,
            List<MemberBalanceResponse> balances
    ) {

        validateBalances(balances);

        List<BalanceNode> creditors =
                new ArrayList<>();

        List<BalanceNode> debtors =
                new ArrayList<>();

        List<DebtGraphNode> graphNodes =
                new ArrayList<>();

        for (MemberBalanceResponse balance : balances) {

            BigDecimal amount =
                    normalize(balance.netBalance());

            String role;

            if (amount.compareTo(BigDecimal.ZERO) > 0) {

                role = "CREDITOR";

                creditors.add(
                        new BalanceNode(
                                balance.userId(),
                                balance.name(),
                                amount
                        )
                );

            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {

                role = "DEBTOR";

                debtors.add(
                        new BalanceNode(
                                balance.userId(),
                                balance.name(),
                                amount.abs()
                        )
                );

            } else {

                role = "SETTLED";
            }

            graphNodes.add(
                    new DebtGraphNode(
                            balance.userId(),
                            balance.name(),
                            amount,
                            role
                    )
            );
        }

        /*
         * Largest outstanding balances first.
         *
         * userId is used as a tie breaker so the
         * output remains deterministic.
         */
        Comparator<BalanceNode> comparator =
                Comparator
                        .comparing(
                                BalanceNode::amount,
                                Comparator.reverseOrder()
                        )
                        .thenComparing(
                                BalanceNode::userId
                        );

        creditors.sort(comparator);
        debtors.sort(comparator);

        List<DebtSettlementSuggestion> settlements =
                new ArrayList<>();

        List<DebtGraphEdge> edges =
                new ArrayList<>();

        int creditorIndex = 0;
        int debtorIndex = 0;

        while (
                creditorIndex < creditors.size()
                        &&
                        debtorIndex < debtors.size()
        ) {

            BalanceNode creditor =
                    creditors.get(creditorIndex);

            BalanceNode debtor =
                    debtors.get(debtorIndex);

            BigDecimal amount =
                    creditor.amount()
                            .min(debtor.amount());

            amount = normalize(amount);

            settlements.add(
                    new DebtSettlementSuggestion(
                            debtor.userId(),
                            debtor.name(),
                            creditor.userId(),
                            creditor.name(),
                            amount
                    )
            );

            edges.add(
                    new DebtGraphEdge(
                            debtor.userId(),
                            creditor.userId(),
                            amount
                    )
            );

            creditor.reduce(amount);
            debtor.reduce(amount);

            if (creditor.isSettled()) {
                creditorIndex++;
            }

            if (debtor.isSettled()) {
                debtorIndex++;
            }
        }

        return new DebtSimplificationResponse(
                groupId,
                List.copyOf(settlements),
                List.copyOf(graphNodes),
                List.copyOf(edges)
        );
    }

    private void validateBalances(
            List<MemberBalanceResponse> balances
    ) {

        BigDecimal total =
                balances.stream()
                        .map(
                                MemberBalanceResponse::netBalance
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        total = normalize(total);

        if (
                total.compareTo(ZERO) != 0
        ) {
            throw new IllegalArgumentException(
                    "Group balances must sum to zero"
            );
        }
    }

    private BigDecimal normalize(
            BigDecimal amount
    ) {

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private static class BalanceNode {

        private final Long userId;
        private final String name;

        private BigDecimal amount;

        private BalanceNode(
                Long userId,
                String name,
                BigDecimal amount
        ) {
            this.userId = userId;
            this.name = name;
            this.amount = amount;
        }

        private Long userId() {
            return userId;
        }

        private String name() {
            return name;
        }

        private BigDecimal amount() {
            return amount;
        }

        private void reduce(
                BigDecimal value
        ) {

            this.amount =
                    this.amount.subtract(value);
        }

        private boolean isSettled() {

            return amount.compareTo(
                    BigDecimal.ZERO
            ) == 0;
        }
    }
}