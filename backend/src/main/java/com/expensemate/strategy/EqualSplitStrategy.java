package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class EqualSplitStrategy implements SplitStrategy {

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

        int memberCount = splits.size();

        BigDecimal baseShare = totalAmount.divide(
                BigDecimal.valueOf(memberCount),
                2,
                RoundingMode.DOWN
        );

        BigDecimal distributed =
                baseShare.multiply(BigDecimal.valueOf(memberCount));

        BigDecimal remainder =
                totalAmount.subtract(distributed);

        int extraPaise =
                remainder.movePointRight(2).intValueExact();

        List<SplitResult> results = new ArrayList<>();

        for (int i = 0; i < memberCount; i++) {

            BigDecimal share = baseShare;

            if (i < extraPaise) {
                share = share.add(new BigDecimal("0.01"));
            }

            results.add(
                    new SplitResult(
                            splits.get(i).getUserId(),
                            share,
                            null
                    )
            );
        }

        return results;
    }
}