package com.expensemate.service.ai.resilience;

public class GeminiProviderUnavailableException
        extends RuntimeException {

    public GeminiProviderUnavailableException(
            String message
    ) {
        super(message);
    }

    public GeminiProviderUnavailableException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}