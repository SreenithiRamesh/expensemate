package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PersonalExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Amount must contain at most 2 decimal places"
    )
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @Size(
            max = 255,
            message = "Description must not exceed 255 characters"
    )
    private String description;

    public PersonalExpenseRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}