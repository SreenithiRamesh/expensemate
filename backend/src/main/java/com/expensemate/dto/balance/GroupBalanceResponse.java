// GroupBalanceResponse.java
package com.expensemate.dto.balance;

import java.util.List;

public record GroupBalanceResponse(
        Long groupId,
        List<MemberBalanceResponse> memberBalances,
        List<BalanceSettlementResponse> settlements
) {
}