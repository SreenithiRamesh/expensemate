package com.expensemate.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @AfterEach
    void clearMdcAfterTest() {

        /*
         * Defensive cleanup so one failed test cannot
         * contaminate another test executing on the same
         * JUnit thread.
         */
        MDC.clear();
    }

    @Test
    void shouldPreserveValidClientSuppliedCorrelationId()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdConstants.HEADER_NAME,
                "expensemate-request-001"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> idSeenInsideChain =
                new AtomicReference<>();

        FilterChain filterChain =
                (currentRequest, currentResponse) ->
                        idSeenInsideChain.set(
                                MDC.get(
                                        CorrelationIdConstants.MDC_KEY
                                )
                        );

        filter.doFilterInternal(
                request,
                response,
                filterChain
        );

        assertEquals(
                "expensemate-request-001",
                idSeenInsideChain.get()
        );

        assertEquals(
                "expensemate-request-001",
                response.getHeader(
                        CorrelationIdConstants.HEADER_NAME
                )
        );

        assertNull(
                MDC.get(
                        CorrelationIdConstants.MDC_KEY
                )
        );
    }

    @Test
    void shouldGenerateUuidWhenHeaderIsMissing()
            throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        AtomicReference<String> idSeenInsideChain =
                new AtomicReference<>();

        filter.doFilterInternal(
                request,
                response,
                (currentRequest, currentResponse) ->
                        idSeenInsideChain.set(
                                MDC.get(
                                        CorrelationIdConstants.MDC_KEY
                                )
                        )
        );

        String generatedId =
                response.getHeader(
                        CorrelationIdConstants.HEADER_NAME
                );

        assertNotNull(generatedId);

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );

        assertEquals(
                generatedId,
                idSeenInsideChain.get()
        );

        assertNull(
                MDC.get(
                        CorrelationIdConstants.MDC_KEY
                )
        );
    }

    @Test
    void shouldReplaceBlankCorrelationId()
            throws ServletException, IOException {

        String suppliedId =
                " ";

        String generatedId =
                executeWithHeader(suppliedId);

        assertNotNull(generatedId);

        assertNotEquals(
                suppliedId,
                generatedId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );
    }

    @Test
    void shouldReplaceCorrelationIdContainingSpaces()
            throws ServletException, IOException {

        String suppliedId =
                "expensemate request 001";

        String generatedId =
                executeWithHeader(suppliedId);

        assertNotNull(generatedId);

        assertNotEquals(
                suppliedId,
                generatedId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );
    }

    @Test
    void shouldReplaceCorrelationIdLongerThanMaximumLength()
            throws ServletException, IOException {

        String suppliedId =
                "a".repeat(
                        CorrelationIdConstants.MAX_LENGTH + 1
                );

        String generatedId =
                executeWithHeader(suppliedId);

        assertNotNull(generatedId);

        assertNotEquals(
                suppliedId,
                generatedId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );
    }

    @Test
    void shouldAcceptCorrelationIdAtMaximumLength()
            throws ServletException, IOException {

        String suppliedId =
                "a".repeat(
                        CorrelationIdConstants.MAX_LENGTH
                );

        String returnedId =
                executeWithHeader(suppliedId);

        assertEquals(
                suppliedId,
                returnedId
        );
    }

    @Test
    void shouldReplaceCorrelationIdContainingNewline()
            throws ServletException, IOException {

        String suppliedId =
                "safe-prefix\nforged-log-entry";

        String generatedId =
                executeWithHeader(suppliedId);

        assertNotNull(generatedId);

        assertNotEquals(
                suppliedId,
                generatedId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );
    }

    @Test
    void shouldClearMdcWhenFilterChainThrowsException() {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdConstants.HEADER_NAME,
                "exception-test-001"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> filter.doFilterInternal(
                                request,
                                response,
                                (currentRequest, currentResponse) -> {

                                    assertEquals(
                                            "exception-test-001",
                                            MDC.get(
                                                    CorrelationIdConstants.MDC_KEY
                                            )
                                    );

                                    throw new RuntimeException(
                                            "Test downstream failure"
                                    );
                                }
                        )
                );

        assertEquals(
                "Test downstream failure",
                exception.getMessage()
        );

        assertNull(
                MDC.get(
                        CorrelationIdConstants.MDC_KEY
                )
        );

        assertEquals(
                "exception-test-001",
                response.getHeader(
                        CorrelationIdConstants.HEADER_NAME
                )
        );
    }

    @Test
    void shouldReplaceCorrelationIdContainingUnsupportedCharacters()
            throws ServletException, IOException {

        String suppliedId =
                "expensemate/request@001";

        String generatedId =
                executeWithHeader(suppliedId);

        assertNotNull(generatedId);

        assertNotEquals(
                suppliedId,
                generatedId
        );

        assertDoesNotThrow(
                () -> UUID.fromString(generatedId)
        );
    }

    private String executeWithHeader(
            String correlationId
    ) throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdConstants.HEADER_NAME,
                correlationId
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilterInternal(
                request,
                response,
                (currentRequest, currentResponse) -> {
                    // No downstream action is required.
                }
        );

        assertNull(
                MDC.get(
                        CorrelationIdConstants.MDC_KEY
                )
        );

        return response.getHeader(
                CorrelationIdConstants.HEADER_NAME
        );
    }
}