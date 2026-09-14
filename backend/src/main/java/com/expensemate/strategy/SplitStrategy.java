package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;

import java.math.BigDecimal;
import java.util.List;

public interface SplitStrategy {

    List<SplitResult> calculate(
            BigDecimal totalAmount,
            List<SplitInputRequest> splits
    );
}