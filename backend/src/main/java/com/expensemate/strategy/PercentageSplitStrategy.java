package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

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

        BigDecimal percentageTotal = BigDecimal.ZERO;

        for (SplitInputRequest split : splits) {

            if (split.getValue() == null) {
                throw new InvalidRequestException(
                        "Percentage is required for all split members"
                );
            }

            percentageTotal =
                    percentageTotal.add(split.getValue());
        }

        if (percentageTotal.compareTo(ONE_HUNDRED) != 0) {
            throw new InvalidRequestException(
                    "Percentage split must total 100"
            );
        }

        List<SplitResult> results = new ArrayList<>();

        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 0; i < splits.size(); i++) {

            SplitInputRequest input = splits.get(i);

            BigDecimal share;

            if (i == splits.size() - 1) {

                share = totalAmount.subtract(allocated);

            } else {

                share = totalAmount
                        .multiply(input.getValue())
                        .divide(
                                ONE_HUNDRED,
                                2,
                                RoundingMode.HALF_UP
                        );

                allocated = allocated.add(share);
            }

            results.add(
                    new SplitResult(
                            input.getUserId(),
                            share,
                            input.getValue()
                    )
            );
        }

        return results;
    }
}