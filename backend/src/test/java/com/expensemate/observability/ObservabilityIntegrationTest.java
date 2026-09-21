package com.expensemate.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldBePublicAndGenerateCorrelationId()
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get("/actuator/health")
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                jsonPath("$.status")
                                        .value("UP")
                        )
                        .andExpect(
                                header().exists(
                                        CorrelationIdConstants.HEADER_NAME
                                )
                        )
                        .andExpect(
                                jsonPath("$.components")
                                        .doesNotExist()
                        )
                        .andExpect(
                                jsonPath("$.details")
                                        .doesNotExist()
                        )
                        .andReturn();

        String correlationId =
                result.getResponse()
                        .getHeader(
                                CorrelationIdConstants.HEADER_NAME
                        );

        assertNotNull(correlationId);

        assertDoesNotThrow(
                () -> UUID.fromString(correlationId)
        );
    }

    @Test
    void healthEndpointShouldPreserveValidSuppliedCorrelationId()
            throws Exception {

        String suppliedCorrelationId =
                "expensemate-observability-001";

        mockMvc.perform(
                        get("/actuator/health")
                                .header(
                                        CorrelationIdConstants.HEADER_NAME,
                                        suppliedCorrelationId
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header().string(
                                CorrelationIdConstants.HEADER_NAME,
                                suppliedCorrelationId
                        )
                );
    }

    @Test
    void healthEndpointShouldReplaceInvalidSuppliedCorrelationId()
            throws Exception {

        String invalidCorrelationId =
                "invalid correlation id";

        MvcResult result =
                mockMvc.perform(
                                get("/actuator/health")
                                        .header(
                                                CorrelationIdConstants.HEADER_NAME,
                                                invalidCorrelationId
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andExpect(
                                header().exists(
                                        CorrelationIdConstants.HEADER_NAME
                                )
                        )
                        .andReturn();

        String returnedCorrelationId =
                result.getResponse()
                        .getHeader(
                                CorrelationIdConstants.HEADER_NAME
                        );

        assertNotNull(returnedCorrelationId);

        assertDoesNotThrow(
                () -> UUID.fromString(
                        returnedCorrelationId
                )
        );
    }

    @Test
    void infoEndpointShouldRejectUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/info")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().exists(
                                CorrelationIdConstants.HEADER_NAME
                        )
                );
    }

    @Test
    void metricsEndpointShouldRejectUnauthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/metrics")
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        header().exists(
                                CorrelationIdConstants.HEADER_NAME
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "observability-test@expensemate.com"
    )
    void infoEndpointShouldAllowAuthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/info")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header().exists(
                                CorrelationIdConstants.HEADER_NAME
                        )
                )
                .andExpect(
                        jsonPath("$.app.name")
                                .value("ExpenseMate")
                )
                .andExpect(
                        jsonPath("$.app.description")
                                .value(
                                        "Personal and shared expense management API"
                                )
                )
                .andExpect(
                        jsonPath("$.app.version")
                                .value("test")
                );
    }

    @Test
    @WithMockUser(
            username = "observability-test@expensemate.com"
    )
    void metricsEndpointShouldAllowAuthenticatedRequest()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/metrics")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header().exists(
                                CorrelationIdConstants.HEADER_NAME
                        )
                )
                .andExpect(
                        jsonPath("$.names")
                                .isArray()
                );
    }

    @Test
    @WithMockUser(
            username = "observability-test@expensemate.com"
    )
    void unexposedEnvironmentEndpointShouldNotBeAvailable()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/env")
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        header().exists(
                                CorrelationIdConstants.HEADER_NAME
                        )
                );
    }

    @Test
    void mdcShouldBeClearedAfterCompletedHttpRequest()
            throws Exception {

        mockMvc.perform(
                        get("/actuator/health")
                                .header(
                                        CorrelationIdConstants.HEADER_NAME,
                                        "mdc-cleanup-test-001"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        header().string(
                                CorrelationIdConstants.HEADER_NAME,
                                "mdc-cleanup-test-001"
                        )
                );

        assertNull(
                org.slf4j.MDC.get(
                        CorrelationIdConstants.MDC_KEY
                )
        );
    }

    @Test
    void validationErrorShouldContainMatchingCorrelationId()
            throws Exception {

        String correlationId =
                "validation-error-test-001";

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .header(
                                        CorrelationIdConstants.HEADER_NAME,
                                        correlationId
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        header().string(
                                CorrelationIdConstants.HEADER_NAME,
                                correlationId
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "One or more request fields are invalid"
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/api/v1/auth/register")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(correlationId)
                )
                .andExpect(
                        jsonPath("$.validationErrors")
                                .isMap()
                );
    }

    @Test
    void malformedJsonErrorShouldContainMatchingCorrelationId()
            throws Exception {

        String correlationId =
                "malformed-json-test-001";

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .header(
                                        CorrelationIdConstants.HEADER_NAME,
                                        correlationId
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Sree",
                                          "email":
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        header().string(
                                CorrelationIdConstants.HEADER_NAME,
                                correlationId
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "The request body is malformed or contains an unsupported value"
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/api/v1/auth/register")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(correlationId)
                );
    }
}
