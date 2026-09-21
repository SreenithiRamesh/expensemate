package com.expensemate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentShouldExposeExpenseMateMetadata()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.info.title")
                                .value("ExpenseMate API")
                )
                .andExpect(
                        jsonPath("$.info.version")
                                .value("v1")
                );
    }

    @Test
    void openApiDocumentShouldExposeBearerAuthentication()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes."
                                        + "bearerAuth.type"
                        )
                                .value("http")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes."
                                        + "bearerAuth.scheme"
                        )
                                .value("bearer")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes."
                                        + "bearerAuth.bearerFormat"
                        )
                                .value("JWT")
                )
                .andExpect(
                        jsonPath(
                                "$.security[0].bearerAuth"
                        )
                                .isArray()
                );
    }

    @Test
    void openApiDocumentShouldExposeRfc7807ProblemSchema()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.type"
                        )
                                .value("object")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.type."
                                        + "format"
                        )
                                .value("uri")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.title."
                                        + "type"
                        )
                                .value("string")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.status."
                                        + "type"
                        )
                                .value("integer")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.detail."
                                        + "type"
                        )
                                .value("string")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.instance."
                                        + "format"
                        )
                                .value("uri")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.timestamp."
                                        + "type"
                        )
                                .value("string")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties.timestamp."
                                        + "format"
                        )
                                .value("date-time")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ProblemDetail.properties."
                                        + "correlationId.type"
                        )
                                .value("string")
                );
    }

    @Test
    void openApiDocumentShouldExposeValidationProblemSchema()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ValidationProblemDetail.allOf[0]"
                                        + "['$ref']"
                        )
                                .value(
                                        "#/components/schemas/ProblemDetail"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ValidationProblemDetail.allOf[1]."
                                        + "properties.validationErrors.type"
                        )
                                .value("object")
                )
                .andExpect(
                        jsonPath(
                                "$.components.schemas."
                                        + "ValidationProblemDetail.allOf[1]."
                                        + "properties.validationErrors."
                                        + "additionalProperties"
                        )
                                .value(true)
                );
    }

    @Test
    void openApiDocumentShouldExposeCorrelationIdHeader()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.headers."
                                        + "CorrelationId.schema.type"
                        )
                                .value("string")
                )
                .andExpect(
                        jsonPath(
                                "$.components.headers."
                                        + "CorrelationId.schema.maxLength"
                        )
                                .value(64)
                );
    }

    @Test
    void openApiDocumentShouldExposeReusableProblemResponses()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.BadRequestProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.ValidationProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.UnauthorizedProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.ForbiddenProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.NotFoundProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.ConflictProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses."
                                        + "UnprocessableEntityProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses."
                                        + "TooManyRequestsProblem"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses."
                                        + "InternalServerErrorProblem"
                        )
                                .exists()
                );
    }

    @Test
    void reusableProblemResponsesShouldUseProblemJson()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.BadRequestProblem."
                                        + "content['application/problem+json']."
                                        + "schema['$ref']"
                        )
                                .value(
                                        "#/components/schemas/ProblemDetail"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.ValidationProblem."
                                        + "content['application/problem+json']."
                                        + "schema['$ref']"
                        )
                                .value(
                                        "#/components/schemas/"
                                                + "ValidationProblemDetail"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.UnauthorizedProblem."
                                        + "content['application/problem+json']"
                        )
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses."
                                        + "InternalServerErrorProblem."
                                        + "content['application/problem+json']"
                        )
                                .exists()
                );
    }

    @Test
    void reusableProblemResponsesShouldExposeOperationalHeaders()
            throws Exception {

        mockMvc.perform(
                        get("/v3/api-docs")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses.BadRequestProblem."
                                        + "headers['X-Correlation-ID']"
                                        + "['$ref']"
                        )
                                .value(
                                        "#/components/headers/CorrelationId"
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.components.responses."
                                        + "TooManyRequestsProblem."
                                        + "headers['Retry-After'].schema.type"
                        )
                                .value("integer")
                );
    }
}