package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public List<SplitResult> calculate(
            BigDecimal totalAmount,
            List<SplitInputRequest> splits
    ) {

        if (splits == null || splits.isEmpty()) {
            throw new InvalidRequestException(
                    "At least one split member is required"
            );
        }

        BigDecimal total = BigDecimal.ZERO;

        for (SplitInputRequest split : splits) {

            if (split.getValue() == null) {
                throw new InvalidRequestException(
                        "Exact amount is required for all split members"
                );
            }

            total = total.add(split.getValue());
        }

        if (total.compareTo(totalAmount) != 0) {
            throw new InvalidRequestException(
                    "Exact split amounts must equal the expense amount"
            );
        }

        return splits.stream()
                .map(split ->
                        new SplitResult(
                                split.getUserId(),
                                split.getValue(),
                                null
                        )
                )
                .toList();
    }
}