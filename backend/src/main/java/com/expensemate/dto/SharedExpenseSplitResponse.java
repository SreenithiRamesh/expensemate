package com.expensemate.dto;

import java.math.BigDecimal;

public class SharedExpenseSplitResponse {

    private Long userId;
    private String name;
    private String email;
    private BigDecimal shareAmount;
    private BigDecimal percentage;

    public SharedExpenseSplitResponse(
            Long userId,
            String name,
            String email,
            BigDecimal shareAmount,
            BigDecimal percentage
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.shareAmount = shareAmount;
        this.percentage = percentage;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}