package com.expensemate.dto.settlement;

import com.expensemate.enums.SettlementMode;

import java.math.BigDecimal;

public record SettlementCreateRequest(

        Long toUserId,

        SettlementMode mode,

        BigDecimal amount

) {
}