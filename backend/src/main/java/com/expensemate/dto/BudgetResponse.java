package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BudgetResponse {

    private final Long id;
    private final ExpenseCategory category;
    private final BigDecimal monthlyLimit;
    private final Integer month;
    private final Integer year;

    private final BigDecimal spent;
    private final BigDecimal remaining;
    private final BigDecimal percentageUsed;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BudgetResponse(
            Long id,
            ExpenseCategory category,
            BigDecimal monthlyLimit,
            Integer month,
            Integer year,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal percentageUsed,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.month = month;
        this.year = year;
        this.spent = spent;
        this.remaining = remaining;
        this.percentageUsed = percentageUsed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getYear() {
        return year;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public BigDecimal getPercentageUsed() {
        return percentageUsed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}