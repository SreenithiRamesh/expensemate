package com.expensemate.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class RecurringPaymentRequest {

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
}