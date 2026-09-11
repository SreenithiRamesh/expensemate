package com.expensemate.exception;

public class BudgetAlreadyExistsException
        extends RuntimeException {

    public BudgetAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}