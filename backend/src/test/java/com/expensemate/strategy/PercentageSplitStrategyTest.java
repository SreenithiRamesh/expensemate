package com.expensemate.strategy;

import com.expensemate.dto.SplitInputRequest;
import com.expensemate.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PercentageSplitStrategyTest {

    private final PercentageSplitStrategy strategy =
            new PercentageSplitStrategy();

    @Test
    void shouldCalculateValidPercentageSplit() {

        List<SplitInputRequest> splits = List.of(
                split(1L, "50"),
                split(2L, "30"),
                split(3L, "20")
        );

        List<SplitResult> results =
                strategy.calculate(
                        new BigDecimal("5000.00"),
                        splits
                );

        assertEquals(
                new BigDecimal("2500.00"),
                results.get(0).shareAmount()
        );

        assertEquals(
                new BigDecimal("1500.00"),
                results.get(1).shareAmount()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                results.get(2).shareAmount()
        );

        BigDecimal total = results.stream()
                .map(SplitResult::shareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
                new BigDecimal("5000.00"),
                total
        );
    }

    @Test
    void shouldRejectPercentageTotalThatIsNotHundred() {

        List<SplitInputRequest> splits = List.of(
                split(1L, "50"),
                split(2L, "30")
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
                "Percentage split must total 100",
                exception.getMessage()
        );
    }

    private SplitInputRequest split(
            Long userId,
            String percentage
    ) {

        SplitInputRequest request =
                new SplitInputRequest();

        request.setUserId(userId);
        request.setValue(new BigDecimal(percentage));

        return request;
    }
}