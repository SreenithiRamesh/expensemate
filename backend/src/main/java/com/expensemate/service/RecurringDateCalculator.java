package com.expensemate.service;

import com.expensemate.entity.RecurringFrequency;
import com.expensemate.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RecurringDateCalculator {

    public LocalDate calculateNextDueDate(
            LocalDate currentDueDate,
            RecurringFrequency frequency
    ) {
        if (currentDueDate == null) {
            throw new InvalidRequestException(
                    "Current due date is required"
            );
        }

        if (frequency == null) {
            throw new InvalidRequestException(
                    "Recurring frequency is required"
            );
        }

        return switch (frequency) {
            case WEEKLY -> currentDueDate.plusWeeks(1);
            case MONTHLY -> currentDueDate.plusMonths(1);
            case YEARLY -> currentDueDate.plusYears(1);
        };
    }

    public LocalDate calculateNextDueDateAfterPayment(
            LocalDate currentDueDate,
            RecurringFrequency frequency,
            LocalDate paymentDate
    ) {
        if (paymentDate == null) {
            throw new InvalidRequestException(
                    "Payment date is required"
            );
        }

        LocalDate nextDueDate =
                calculateNextDueDate(
                        currentDueDate,
                        frequency
                );

        while (!nextDueDate.isAfter(paymentDate)) {
            nextDueDate =
                    calculateNextDueDate(
                            nextDueDate,
                            frequency
                    );
        }

        return nextDueDate;
    }
}