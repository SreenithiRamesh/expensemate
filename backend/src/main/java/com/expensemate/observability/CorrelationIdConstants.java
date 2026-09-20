package com.expensemate.observability;

public final class CorrelationIdConstants {

    public static final String HEADER_NAME =
            "X-Correlation-ID";

    public static final String MDC_KEY =
            "correlationId";

    public static final int MAX_LENGTH =
            64;

    private CorrelationIdConstants() {
        // Utility class.
    }
}