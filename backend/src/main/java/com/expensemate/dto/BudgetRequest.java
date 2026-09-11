package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BudgetRequest {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Monthly limit is required")
    @DecimalMin(
            value = "0.01",
            message = "Monthly limit must be greater than zero"
    )
    private BigDecimal monthlyLimit;

    @NotNull(message = "Month is required")
    @Min(
            value = 1,
            message = "Month must be between 1 and 12"
    )
    @Max(
            value = 12,
            message = "Month must be between 1 and 12"
    )
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(
            value = 2000,
            message = "Year must be 2000 or later"
    )
    private Integer year;

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(
            ExpenseCategory category
    ) {
        this.category = category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(
            BigDecimal monthlyLimit
    ) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(
            Integer month
    ) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(
            Integer year
    ) {
        this.year = year;
    }
}