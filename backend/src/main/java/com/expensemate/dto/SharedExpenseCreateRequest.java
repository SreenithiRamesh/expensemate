package com.expensemate.dto;

import com.expensemate.enums.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SharedExpenseCreateRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private Long paidByUserId;

    @NotNull
    private SplitType splitType;

    @NotNull
    private LocalDate expenseDate;

    @NotEmpty
    private List<@Valid SplitInputRequest> splits;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public List<SplitInputRequest> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitInputRequest> splits) {
        this.splits = splits;
    }
}