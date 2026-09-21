package com.expensemate.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Configuration
@SuppressWarnings({
        "rawtypes",
        "unchecked"
})
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME =
            "bearerAuth";

    public static final String PROBLEM_SCHEMA_NAME =
            "ProblemDetail";

    public static final String VALIDATION_PROBLEM_SCHEMA_NAME =
            "ValidationProblemDetail";

    public static final String CORRELATION_HEADER_NAME =
            "X-Correlation-ID";

    private static final String SCHEMA_REFERENCE_PREFIX =
            "#/components/schemas/";

    private static final String HEADER_REFERENCE_PREFIX =
            "#/components/headers/";

    @Bean
    public OpenAPI expenseMateOpenAPI() {

        Components components =
                new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_NAME,
                                bearerSecurityScheme()
                        )
                        .addSchemas(
                                PROBLEM_SCHEMA_NAME,
                                problemDetailSchema()
                        )
                        .addSchemas(
                                VALIDATION_PROBLEM_SCHEMA_NAME,
                                validationProblemDetailSchema()
                        )
                        .addHeaders(
                                "CorrelationId",
                                correlationIdHeader()
                        )
                        .addResponses(
                                "BadRequestProblem",
                                problemResponse(
                                        "The request is malformed or "
                                                + "contains invalid data.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "ValidationProblem",
                                problemResponse(
                                        "Request validation failed.",
                                        VALIDATION_PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "UnauthorizedProblem",
                                problemResponse(
                                        "Authentication is required or "
                                                + "the supplied access token "
                                                + "is invalid.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "ForbiddenProblem",
                                problemResponse(
                                        "The authenticated user is not "
                                                + "allowed to perform this "
                                                + "operation.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "NotFoundProblem",
                                problemResponse(
                                        "The requested resource was not found.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "ConflictProblem",
                                problemResponse(
                                        "The request conflicts with the "
                                                + "current state of the resource.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "UnprocessableEntityProblem",
                                problemResponse(
                                        "The request is syntactically valid "
                                                + "but cannot be processed.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        )
                        .addResponses(
                                "TooManyRequestsProblem",
                                rateLimitProblemResponse()
                        )
                        .addResponses(
                                "InternalServerErrorProblem",
                                problemResponse(
                                        "An unexpected server error occurred.",
                                        PROBLEM_SCHEMA_NAME
                                )
                        );

        return new OpenAPI()
                .info(
                        new Info()
                                .title("ExpenseMate API")
                                .version("v1")
                                .description(
                                        """
                                        REST API for personal expenses, budgets,
                                        recurring expenses, shared expenses,
                                        balances, settlements, dashboards and
                                        AI-assisted financial insights.

                                        Error responses follow RFC 7807 and use
                                        the application/problem+json media type.
                                        A correlation ID is returned through the
                                        X-Correlation-ID response header and the
                                        correlationId problem property.
                                        """
                                )
                )
                .components(components)
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(
                                        SECURITY_SCHEME_NAME
                                )
                );
    }

    private SecurityScheme bearerSecurityScheme() {

        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "Provide the access token using the format: "
                                + "Bearer <JWT access token>"
                );
    }

    private Schema<?> problemDetailSchema() {

        ObjectSchema problemSchema =
                new ObjectSchema();

        problemSchema
                .name(PROBLEM_SCHEMA_NAME)
                .description(
                        "RFC 7807 problem details returned by ExpenseMate."
                )
                .addProperty(
                        "type",
                        new StringSchema()
                                .format("uri")
                                .description(
                                        "URI identifying the problem type."
                                )
                                .example(
                                        "https://api.expensemate.com/"
                                                + "problems/resource-not-found"
                                )
                )
                .addProperty(
                        "title",
                        new StringSchema()
                                .description(
                                        "Short, human-readable problem title."
                                )
                                .example(
                                        "Resource not found"
                                )
                )
                .addProperty(
                        "status",
                        new IntegerSchema()
                                .format("int32")
                                .description(
                                        "HTTP response status code."
                                )
                                .example(404)
                )
                .addProperty(
                        "detail",
                        new StringSchema()
                                .description(
                                        "Human-readable explanation specific "
                                                + "to this occurrence."
                                )
                                .example(
                                        "The requested expense was not found."
                                )
                )
                .addProperty(
                        "instance",
                        new StringSchema()
                                .format("uri")
                                .description(
                                        "Request URI associated with the error."
                                )
                                .example(
                                        "/api/v1/expenses/999"
                                )
                )
                .addProperty(
                        "timestamp",
                        new StringSchema()
                                .format("date-time")
                                .description(
                                        "Time at which the error occurred."
                                )
                                .example(
                                        "2026-09-22T00:00:00Z"
                                )
                )
                .addProperty(
                        "correlationId",
                        new StringSchema()
                                .description(
                                        "Identifier used to correlate the "
                                                + "request with server logs."
                                )
                                .example(
                                        "8bbdd79a-7201-4d0b-a046-"
                                                + "2d463514d03b"
                                )
                );

        problemSchema.setRequired(
                List.of(
                        "type",
                        "title",
                        "status",
                        "detail",
                        "instance",
                        "timestamp"
                )
        );

        return problemSchema;
    }

    private Schema<?> validationProblemDetailSchema() {

        /*
         * validationErrors is represented as a free-form JSON
         * object because each key is the rejected request field
         * and each value is its validation message.
         *
         * Boolean.TRUE is intentionally used for
         * additionalProperties. It is compatible with the
         * SpringDoc/Swagger model version used by this project
         * and avoids schema serialization warnings.
         */
        MapSchema validationErrors =
                new MapSchema();

        validationErrors.setDescription(
                "Map containing a validation message for each "
                        + "invalid request field."
        );

        validationErrors.setAdditionalProperties(
                Boolean.TRUE
        );

        validationErrors.setExample(
                Map.of(
                        "title",
                        "must not be blank",
                        "amount",
                        "must be greater than zero"
                )
        );

        ObjectSchema validationExtension =
                new ObjectSchema();

        validationExtension.addProperty(
                "validationErrors",
                validationErrors
        );

        validationExtension.setRequired(
                List.of(
                        "validationErrors"
                )
        );

        ObjectSchema validationProblem =
                new ObjectSchema();

        validationProblem.setName(
                VALIDATION_PROBLEM_SCHEMA_NAME
        );

        validationProblem.setDescription(
                "RFC 7807 problem details containing field-level "
                        + "validation errors."
        );

        validationProblem.setAllOf(
                List.of(
                        new Schema<>()
                                .$ref(
                                        SCHEMA_REFERENCE_PREFIX
                                                + PROBLEM_SCHEMA_NAME
                                ),
                        validationExtension
                )
        );

        return validationProblem;
    }

    private Header correlationIdHeader() {

        return new Header()
                .description(
                        "Request correlation identifier. The server returns "
                                + "the accepted client identifier or generates "
                                + "a new identifier when none is supplied."
                )
                .schema(
                        new StringSchema()
                                .maxLength(64)
                                .example(
                                        "8bbdd79a-7201-4d0b-a046-"
                                                + "2d463514d03b"
                                )
                );
    }

    private ApiResponse problemResponse(
            String description,
            String schemaName
    ) {

        Header correlationHeaderReference =
                new Header();

        correlationHeaderReference.set$ref(
                HEADER_REFERENCE_PREFIX
                        + "CorrelationId"
        );

        return new ApiResponse()
                .description(description)
                .addHeaderObject(
                        CORRELATION_HEADER_NAME,
                        correlationHeaderReference
                )
                .content(
                        problemContent(
                                schemaName
                        )
                );
    }

    private ApiResponse rateLimitProblemResponse() {

        ApiResponse response =
                problemResponse(
                        "The request rate limit has been exceeded.",
                        PROBLEM_SCHEMA_NAME
                );

        response.addHeaderObject(
                "Retry-After",
                new Header()
                        .description(
                                "Number of seconds the client should wait "
                                        + "before retrying."
                        )
                        .schema(
                                new IntegerSchema()
                                        .format("int64")
                                        .minimum(
                                                BigDecimal.ZERO
                                        )
                                        .example(60)
                        )
        );

        return response;
    }

    private Content problemContent(
            String schemaName
    ) {

        Schema<?> problemReference =
                new Schema<>();

        problemReference.set$ref(
                SCHEMA_REFERENCE_PREFIX
                        + schemaName
        );

        io.swagger.v3.oas.models.media.MediaType problemMediaType =
                new io.swagger.v3.oas.models.media.MediaType();

        problemMediaType.setSchema(
                problemReference
        );

        return new Content()
                .addMediaType(
                        MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        problemMediaType
                );
    }
}