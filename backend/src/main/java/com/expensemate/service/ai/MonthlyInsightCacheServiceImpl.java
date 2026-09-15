package com.expensemate.service.ai;

import com.expensemate.repository.MonthlyInsightCacheRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class MonthlyInsightCacheServiceImpl
        implements MonthlyInsightCacheService {

    private final MonthlyInsightCacheRepository cacheRepository;

    public MonthlyInsightCacheServiceImpl(
            MonthlyInsightCacheRepository cacheRepository
    ) {
        this.cacheRepository = cacheRepository;
    }

    @Override
    @Transactional
    public void invalidateForExpenseChange(
            Long userId,
            LocalDate expenseDate
    ) {

        if (userId == null || expenseDate == null) {
            return;
        }

        YearMonth affectedMonth =
                YearMonth.from(expenseDate);

        Set<YearMonth> months =
                new LinkedHashSet<>();

        months.add(affectedMonth);
        months.add(affectedMonth.plusMonths(1));

        invalidateMonths(
                userId,
                months
        );
    }

    @Override
    @Transactional
    public void invalidateForExpenseChange(
            Long userId,
            LocalDate oldExpenseDate,
            LocalDate newExpenseDate
    ) {

        if (userId == null) {
            return;
        }

        Set<YearMonth> months =
                new LinkedHashSet<>();

        addExpenseAffectedMonths(
                months,
                oldExpenseDate
        );

        addExpenseAffectedMonths(
                months,
                newExpenseDate
        );

        invalidateMonths(
                userId,
                months
        );
    }

    @Override
    @Transactional
    public void invalidateForBudgetChange(
            Long userId,
            YearMonth budgetMonth
    ) {

        if (userId == null || budgetMonth == null) {
            return;
        }

        cacheRepository
                .deleteByUserIdAndInsightMonth(
                        userId,
                        budgetMonth.toString()
                );
    }

    @Override
    @Transactional
    public void invalidateForBudgetChange(
            Long userId,
            YearMonth oldBudgetMonth,
            YearMonth newBudgetMonth
    ) {

        if (userId == null) {
            return;
        }

        Set<YearMonth> months =
                new LinkedHashSet<>();

        if (oldBudgetMonth != null) {
            months.add(oldBudgetMonth);
        }

        if (newBudgetMonth != null) {
            months.add(newBudgetMonth);
        }

        invalidateMonths(
                userId,
                months
        );
    }

    private void addExpenseAffectedMonths(
            Set<YearMonth> months,
            LocalDate expenseDate
    ) {

        if (expenseDate == null) {
            return;
        }

        YearMonth affectedMonth =
                YearMonth.from(expenseDate);

        months.add(affectedMonth);
        months.add(
                affectedMonth.plusMonths(1)
        );
    }

    private void invalidateMonths(
            Long userId,
            Set<YearMonth> months
    ) {

        for (YearMonth month : months) {

            cacheRepository
                    .deleteByUserIdAndInsightMonth(
                            userId,
                            month.toString()
                    );
        }
    }
}