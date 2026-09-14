package com.expensemate.balance;

import com.expensemate.dto.balance.BalanceSettlementResponse;
import com.expensemate.dto.balance.GroupBalanceResponse;
import com.expensemate.dto.balance.MemberBalanceResponse;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.SharedExpense;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NetBalanceCalculator
        implements BalanceCalculator {

    @Override
    public GroupBalanceResponse calculate(
            Long groupId,
            List<SharedExpense> expenses,
            List<ExpenseSplit> splits,
            List<Settlement> settlements
    ) {

        Map<Long, BigDecimal> netBalances =
                new HashMap<>();

        Map<Long, String> userNames =
                new HashMap<>();

        /*
         * STEP 1
         *
         * Add the complete expense amount
         * to the person who paid.
         */
        for (SharedExpense expense : expenses) {

            Long payerId =
                    expense.getPaidBy().getId();

            String payerName =
                    expense.getPaidBy().getName();

            netBalances.putIfAbsent(
                    payerId,
                    BigDecimal.ZERO
            );

            userNames.putIfAbsent(
                    payerId,
                    payerName
            );

            netBalances.put(
                    payerId,
                    netBalances
                            .get(payerId)
                            .add(
                                    expense.getAmount()
                            )
            );
        }

        /*
         * STEP 2
         *
         * Subtract each person's own
         * expense share.
         */
        for (ExpenseSplit split : splits) {

            Long userId =
                    split.getUser().getId();

            String userName =
                    split.getUser().getName();

            netBalances.putIfAbsent(
                    userId,
                    BigDecimal.ZERO
            );

            userNames.putIfAbsent(
                    userId,
                    userName
            );

            netBalances.put(
                    userId,
                    netBalances
                            .get(userId)
                            .subtract(
                                    split.getShareAmount()
                            )
            );
        }

        /*
         * STEP 3
         *
         * Apply recorded settlements.
         *
         * If debtor pays creditor:
         *
         * debtor balance increases
         * toward zero.
         *
         * creditor balance decreases
         * toward zero.
         */
        for (Settlement settlement : settlements) {

            Long fromUserId =
                    settlement
                            .getFromUser()
                            .getId();

            String fromUserName =
                    settlement
                            .getFromUser()
                            .getName();

            Long toUserId =
                    settlement
                            .getToUser()
                            .getId();

            String toUserName =
                    settlement
                            .getToUser()
                            .getName();

            BigDecimal amount =
                    settlement.getAmount();

            netBalances.putIfAbsent(
                    fromUserId,
                    BigDecimal.ZERO
            );

            netBalances.putIfAbsent(
                    toUserId,
                    BigDecimal.ZERO
            );

            userNames.putIfAbsent(
                    fromUserId,
                    fromUserName
            );

            userNames.putIfAbsent(
                    toUserId,
                    toUserName
            );

            /*
             * Debtor paid money,
             * therefore debt decreases.
             */
            netBalances.put(
                    fromUserId,
                    netBalances
                            .get(fromUserId)
                            .add(amount)
            );

            /*
             * Creditor received money,
             * therefore receivable decreases.
             */
            netBalances.put(
                    toUserId,
                    netBalances
                            .get(toUserId)
                            .subtract(amount)
            );
        }

        List<MemberBalanceResponse> memberBalances =
                netBalances
                        .entrySet()
                        .stream()
                        .map(
                                entry ->
                                        new MemberBalanceResponse(
                                                entry.getKey(),
                                                userNames.get(
                                                        entry.getKey()
                                                ),
                                                normalize(
                                                        entry.getValue()
                                                )
                                        )
                        )
                        .toList();

        List<BalanceNode> creditors =
                new ArrayList<>();

        List<BalanceNode> debtors =
                new ArrayList<>();

        for (
                Map.Entry<Long, BigDecimal> entry
                : netBalances.entrySet()
        ) {

            BigDecimal balance =
                    normalize(
                            entry.getValue()
                    );

            if (
                    balance.compareTo(
                            BigDecimal.ZERO
                    ) > 0
            ) {

                creditors.add(
                        new BalanceNode(
                                entry.getKey(),
                                userNames.get(
                                        entry.getKey()
                                ),
                                balance
                        )
                );

            } else if (
                    balance.compareTo(
                            BigDecimal.ZERO
                    ) < 0
            ) {

                debtors.add(
                        new BalanceNode(
                                entry.getKey(),
                                userNames.get(
                                        entry.getKey()
                                ),
                                balance.abs()
                        )
                );
            }
        }

        List<BalanceSettlementResponse> balanceSettlements =
                calculateSettlements(
                        creditors,
                        debtors
                );

        return new GroupBalanceResponse(
                groupId,
                memberBalances,
                balanceSettlements
        );
    }

    private List<BalanceSettlementResponse>
    calculateSettlements(
            List<BalanceNode> creditors,
            List<BalanceNode> debtors
    ) {

        List<BalanceSettlementResponse> result =
                new ArrayList<>();

        int creditorIndex = 0;
        int debtorIndex = 0;

        while (
                creditorIndex < creditors.size()
                        &&
                        debtorIndex < debtors.size()
        ) {

            BalanceNode creditor =
                    creditors.get(
                            creditorIndex
                    );

            BalanceNode debtor =
                    debtors.get(
                            debtorIndex
                    );

            BigDecimal settlementAmount =
                    creditor.amount
                            .min(
                                    debtor.amount
                            );

            settlementAmount =
                    normalize(
                            settlementAmount
                    );

            result.add(
                    new BalanceSettlementResponse(
                            debtor.userId,
                            debtor.userName,
                            creditor.userId,
                            creditor.userName,
                            settlementAmount
                    )
            );

            creditor.amount =
                    creditor.amount
                            .subtract(
                                    settlementAmount
                            );

            debtor.amount =
                    debtor.amount
                            .subtract(
                                    settlementAmount
                            );

            if (
                    creditor.amount.compareTo(
                            BigDecimal.ZERO
                    ) == 0
            ) {
                creditorIndex++;
            }

            if (
                    debtor.amount.compareTo(
                            BigDecimal.ZERO
                    ) == 0
            ) {
                debtorIndex++;
            }
        }

        return result;
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

        private final String userName;

        private BigDecimal amount;

        private BalanceNode(
                Long userId,
                String userName,
                BigDecimal amount
        ) {

            this.userId = userId;
            this.userName = userName;
            this.amount = amount;
        }
    }
}