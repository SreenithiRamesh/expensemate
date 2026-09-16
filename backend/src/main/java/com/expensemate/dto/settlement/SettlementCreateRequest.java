package com.expensemate.dto.settlement;

import com.expensemate.enums.SettlementMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SettlementCreateRequest(

        @NotNull(message = "Receiver user ID is required")
        Long toUserId,

        @NotNull(message = "Settlement mode is required")
        SettlementMode mode,

        @Positive(message = "Settlement amount must be greater than zero")
        BigDecimal amount

) {
}