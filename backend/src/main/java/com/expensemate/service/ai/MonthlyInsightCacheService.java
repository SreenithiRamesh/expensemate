package com.expensemate.service.ai;

import java.time.LocalDate;
import java.time.YearMonth;

public interface MonthlyInsightCacheService {

    /*
     * Personal expense change in month M affects:
     *
     * M     -> current month's spending
     * M + 1 -> previous-month comparison
     */
    void invalidateForExpenseChange(
            Long userId,
            LocalDate expenseDate
    );

    /*
     * Used when an expense date changes.
     *
     * Both the old and new expense months,
     * including their following months,
     * must be invalidated.
     */
    void invalidateForExpenseChange(
            Long userId,
            LocalDate oldExpenseDate,
            LocalDate newExpenseDate
    );

    /*
     * A budget change only affects the insight
     * for the budget's own month.
     */
    void invalidateForBudgetChange(
            Long userId,
            YearMonth budgetMonth
    );

    /*
     * Used when a budget moves from one
     * month/year to another.
     *
     * Both the old and new months are invalidated.
     */
    void invalidateForBudgetChange(
            Long userId,
            YearMonth oldBudgetMonth,
            YearMonth newBudgetMonth
    );
}