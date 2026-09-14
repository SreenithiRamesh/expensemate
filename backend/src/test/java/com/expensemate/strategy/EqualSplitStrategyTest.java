package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EqualSplitStrategyTest {

    private final EqualSplitStrategy strategy =
            new EqualSplitStrategy();

    @Test
    void shouldSplitHundredAmongThreeWithDeterministicRounding() {

        List<SplitInputRequest> splits = List.of(
                split(1L),
                split(2L),
                split(3L)
        );

        List<SplitResult> results =
                strategy.calculate(
                        new BigDecimal("100.00"),
                        splits
                );

        assertEquals(
                new BigDecimal("33.34"),
                results.get(0).shareAmount()
        );

        assertEquals(
                new BigDecimal("33.33"),
                results.get(1).shareAmount()
        );

        assertEquals(
                new BigDecimal("33.33"),
                results.get(2).shareAmount()
        );

        BigDecimal total = results.stream()
                .map(SplitResult::shareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                new BigDecimal("100.00"),
                total
        );
    }

    @Test
    void shouldSplitEvenAmountEqually() {

        List<SplitResult> results =
                strategy.calculate(
                        new BigDecimal("100.00"),
                        List.of(split(1L), split(2L))
                );

        assertEquals(
                new BigDecimal("50.00"),
                results.get(0).shareAmount()
        );

        assertEquals(
                new BigDecimal("50.00"),
                results.get(1).shareAmount()
        );
    }

    private SplitInputRequest split(Long userId) {

        SplitInputRequest request =
                new SplitInputRequest();

        request.setUserId(userId);

        return request;
    }
}