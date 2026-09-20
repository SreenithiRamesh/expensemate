package com.expensemate.service.ai.resilience;

public class GeminiNonRetryableException
        extends RuntimeException {

    public GeminiNonRetryableException(
            String message
    ) {
        super(message);
    }

    public GeminiNonRetryableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}