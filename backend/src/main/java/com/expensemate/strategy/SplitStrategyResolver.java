package com.expensemate.strategy;

import com.expensemate.enums.SplitType;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SplitStrategyResolver {

    private final Map<SplitType, SplitStrategy> strategies;

    public SplitStrategyResolver(
            EqualSplitStrategy equalSplitStrategy,
            PercentageSplitStrategy percentageSplitStrategy,
            ExactSplitStrategy exactSplitStrategy
    ) {
        this.strategies = Map.of(
                SplitType.EQUAL, equalSplitStrategy,
                SplitType.PERCENTAGE, percentageSplitStrategy,
                SplitType.EXACT, exactSplitStrategy
        );
    }

    public SplitStrategy resolve(SplitType splitType) {

        SplitStrategy strategy = strategies.get(splitType);

        if (strategy == null) {
            throw new InvalidRequestException(
                    "Unsupported split type"
            );
        }

        return strategy;
    }
}