package com.expensemate.service.ai.resilience;

public class GeminiTimeoutException extends RuntimeException {

    public GeminiTimeoutException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}