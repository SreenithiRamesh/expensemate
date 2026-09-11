package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.RecurringFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecurringExpenseResponse {

    private final Long id;
    private final String title;
    private final BigDecimal amount;
    private final ExpenseCategory category;
    private final RecurringFrequency frequency;
    private final LocalDate nextDueDate;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public RecurringExpenseResponse(
            Long id,
            String title,
            BigDecimal amount,
            ExpenseCategory category,
            RecurringFrequency frequency,
            LocalDate nextDueDate,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.frequency = frequency;
        this.nextDueDate = nextDueDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public RecurringFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public Boolean getActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}