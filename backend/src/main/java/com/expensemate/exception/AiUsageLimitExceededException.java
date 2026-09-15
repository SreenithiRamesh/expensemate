package com.expensemate.exception;

public class AiUsageLimitExceededException extends RuntimeException {

    public AiUsageLimitExceededException(String message) {
        super(message);
    }
}