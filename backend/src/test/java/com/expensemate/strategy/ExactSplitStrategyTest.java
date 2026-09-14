package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExactSplitStrategyTest {

    private final ExactSplitStrategy strategy =
            new ExactSplitStrategy();

    @Test
    void shouldAcceptExactAmountsMatchingExpenseTotal() {

        List<SplitInputRequest> splits = List.of(
                split(1L, "400.00"),
                split(2L, "350.00"),
                split(3L, "250.00")
        );

        List<SplitResult> results =
                strategy.calculate(
                        new BigDecimal("1000.00"),
                        splits
                );

        assertEquals(3, results.size());

        assertEquals(
                new BigDecimal("400.00"),
                results.get(0).shareAmount()
        );

        assertEquals(
                new BigDecimal("350.00"),
                results.get(1).shareAmount()
        );

        assertEquals(
                new BigDecimal("250.00"),
                results.get(2).shareAmount()
        );
    }

    @Test
    void shouldRejectExactAmountsNotMatchingExpenseTotal() {

        List<SplitInputRequest> splits = List.of(
                split(1L, "400.00"),
                split(2L, "350.00"),
                split(3L, "200.00")
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> strategy.calculate(
                                new BigDecimal("1000.00"),
                                splits
                        )
                );

        assertEquals(
                "Exact split amounts must equal the expense amount",
                exception.getMessage()
        );
    }

    private SplitInputRequest split(
            Long userId,
            String amount
    ) {

        SplitInputRequest request =
                new SplitInputRequest();

        request.setUserId(userId);
        request.setValue(new BigDecimal(amount));

        return request;
    }
}