package com.expensemate.dto;

import com.expensemate.enums.SplitType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SharedExpenseResponse {

    private Long id;
    private Long groupId;

    private String title;
    private BigDecimal amount;

    private Long paidByUserId;
    private String paidByName;

    private SplitType splitType;
    private LocalDate expenseDate;

    private List<SharedExpenseSplitResponse> splits;

    private LocalDateTime createdAt;

    public SharedExpenseResponse(
            Long id,
            Long groupId,
            String title,
            BigDecimal amount,
            Long paidByUserId,
            String paidByName,
            SplitType splitType,
            LocalDate expenseDate,
            List<SharedExpenseSplitResponse> splits,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.amount = amount;
        this.paidByUserId = paidByUserId;
        this.paidByName = paidByName;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.splits = splits;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public List<SharedExpenseSplitResponse> getSplits() {
        return splits;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}