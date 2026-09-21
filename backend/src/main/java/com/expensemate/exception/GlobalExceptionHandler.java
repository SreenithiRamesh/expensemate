package com.expensemate.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private final ApiProblemFactory problemFactory;

    public GlobalExceptionHandler(
            ApiProblemFactory problemFactory
    ) {
        this.problemFactory = problemFactory;
    }

    @ExceptionHandler(
            EmailAlreadyExistsException.class
    )
    public ResponseEntity<ProblemDetail>
    handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "email-already-exists",
                "Email already registered",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            InvalidCredentialsException.class
    )
    public ResponseEntity<ProblemDetail>
    handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "invalid-credentials",
                "Authentication failed",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            InvalidRefreshTokenException.class
    )
    public ResponseEntity<ProblemDetail>
    handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.UNAUTHORIZED,
                "invalid-refresh-token",
                "Invalid refresh token",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ProblemDetail>
    handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Resource not found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            InvalidRequestException.class
    )
    public ResponseEntity<ProblemDetail>
    handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "invalid-request",
                "Invalid request",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            BudgetAlreadyExistsException.class
    )
    public ResponseEntity<ProblemDetail>
    handleBudgetAlreadyExists(
            BudgetAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "budget-already-exists",
                "Budget already exists",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            ForbiddenOperationException.class
    )
    public ResponseEntity<ProblemDetail>
    handleForbiddenOperation(
            ForbiddenOperationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.FORBIDDEN,
                "forbidden-operation",
                "Operation forbidden",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            AiUsageLimitExceededException.class
    )
    public ResponseEntity<ProblemDetail>
    handleAiUsageLimitExceeded(
            AiUsageLimitExceededException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.TOO_MANY_REQUESTS,
                "ai-usage-limit-exceeded",
                "AI usage limit exceeded",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            AiServiceException.class
    )
    public ResponseEntity<ProblemDetail>
    handleAiService(
            AiServiceException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "ai-service-unavailable",
                "AI service unavailable",
                exception.getMessage(),
                request
        );
    }

    /*
     * Request-body validation failures produced by
     * Jakarta Bean Validation.
     */
    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ProblemDetail>
    handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.putIfAbsent(
                                error.getField(),
                                safeValidationMessage(
                                        error.getDefaultMessage()
                                )
                        )
                );

        return validationResponse(
                request,
                validationErrors
        );
    }

    /*
     * Controller-method parameter validation.
     *
     * Examples:
     * page must be zero or greater
     * size must be between 1 and 100
     */
    @ExceptionHandler(
            HandlerMethodValidationException.class
    )
    public ResponseEntity<ProblemDetail>
    handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        exception.getParameterValidationResults()
                .forEach(result -> {

                    String parameterName =
                            result.getMethodParameter()
                                    .getParameterName();

                    if (parameterName == null
                            || parameterName.isBlank()) {

                        parameterName = "parameter";
                    }

                    String finalParameterName =
                            parameterName;

                    result.getResolvableErrors()
                            .forEach(error ->
                                    validationErrors.putIfAbsent(
                                            finalParameterName,
                                            safeValidationMessage(
                                                    error.getDefaultMessage()
                                            )
                                    )
                            );
                });

        return validationResponse(
                request,
                validationErrors
        );
    }

    /*
     * Malformed JSON and unsupported enum values contained
     * inside a JSON request body.
     *
     * Never return Jackson's internal parsing message because
     * it may expose Java type and implementation details.
     */
    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<ProblemDetail>
    handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "malformed-request",
                "Malformed request",
                "The request body is malformed or contains an unsupported value",
                request
        );
    }

    /*
     * Invalid path-variable or query-parameter values.
     */
    @ExceptionHandler(
            MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<ProblemDetail>
    handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        String parameterName =
                exception.getName();

        if (parameterName == null
                || parameterName.isBlank()) {

            parameterName = "parameter";
        }

        return response(
                HttpStatus.BAD_REQUEST,
                "invalid-parameter",
                "Invalid parameter",
                "Invalid value for parameter '"
                        + parameterName
                        + "'",
                request
        );
    }

    /*
     * Unknown API routes and missing static resources.
     */
    @ExceptionHandler(
            NoResourceFoundException.class
    )
    public ResponseEntity<ProblemDetail>
    handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                "route-not-found",
                "Route not found",
                "The requested resource does not exist",
                request
        );
    }

    /*
     * Final safety boundary.
     *
     * The complete exception is logged server-side, while the
     * client receives only a stable and sanitized description.
     */
    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ProblemDetail>
    handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled exception while processing {} {}. Exception={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-server-error",
                "Internal server error",
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ProblemDetail>
    validationResponse(
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {

        ProblemDetail problem =
                problemFactory.create(
                        HttpStatus.BAD_REQUEST,
                        "validation-error",
                        "Validation failed",
                        "One or more request fields are invalid",
                        request,
                        validationErrors
                );

        return problemResponse(
                HttpStatus.BAD_REQUEST,
                problem
        );
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String problemType,
            String title,
            String detail,
            HttpServletRequest request
    ) {

        ProblemDetail problem =
                problemFactory.create(
                        status,
                        problemType,
                        title,
                        safeDetail(
                                detail,
                                status
                        ),
                        request
                );

        return problemResponse(
                status,
                problem
        );
    }

    private ResponseEntity<ProblemDetail>
    problemResponse(
            HttpStatus status,
            ProblemDetail problem
    ) {
        return ResponseEntity
                .status(status)
                .contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                )
                .body(problem);
    }

    private String safeDetail(
            String detail,
            HttpStatus status
    ) {

        if (detail != null
                && !detail.isBlank()) {

            return detail;
        }

        return status.getReasonPhrase();
    }

    private String safeValidationMessage(
            String message
    ) {

        if (message == null
                || message.isBlank()) {

            return "Invalid value";
        }

        return message;
    }
}