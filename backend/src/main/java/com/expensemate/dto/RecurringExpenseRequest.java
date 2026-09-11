package com.expensemate.dto;

import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.RecurringFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringExpenseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must not exceed 120 characters")
    private String title;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Amount must be greater than zero"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Amount can have up to 10 integer digits and 2 decimal places"
    )
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Frequency is required")
    private RecurringFrequency frequency;

    @NotNull(message = "Next due date is required")
    private LocalDate nextDueDate;

    private Boolean active;

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

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public void setFrequency(RecurringFrequency frequency) {
        this.frequency = frequency;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}