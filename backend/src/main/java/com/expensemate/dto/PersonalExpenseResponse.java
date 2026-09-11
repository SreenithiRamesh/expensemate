package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PersonalExpenseResponse {

    private Long id;
    private BigDecimal amount;
    private ExpenseCategory category;
    private LocalDate expenseDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PersonalExpenseResponse() {
    }

    public PersonalExpenseResponse(
            Long id,
            BigDecimal amount,
            ExpenseCategory category,
            LocalDate expenseDate,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}