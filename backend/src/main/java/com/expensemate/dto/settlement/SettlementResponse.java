package com.expensemate.dto.settlement;

import com.expensemate.enums.SettlementMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(

        Long id,

        Long groupId,

        Long fromUserId,

        String fromUserName,

        Long toUserId,

        String toUserName,

        BigDecimal amount,

        SettlementMode mode,

        LocalDateTime settledAt

) {
}