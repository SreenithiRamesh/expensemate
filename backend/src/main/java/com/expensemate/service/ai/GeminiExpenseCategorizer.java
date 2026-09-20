package com.expensemate.service.ai;

import com.expensemate.dto.ai.ExpenseCategorizationResult;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.exception.AiServiceException;
import com.expensemate.service.ai.provider.GeminiProviderClient;
import com.expensemate.service.ai.resilience.GeminiNonRetryableException;
import com.expensemate.service.ai.resilience.GeminiProviderUnavailableException;
import com.expensemate.service.ai.resilience.GeminiResilienceExecutor;
import com.expensemate.service.ai.resilience.GeminiTimeoutException;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDate;

@Component
public class GeminiExpenseCategorizer
        implements ExpenseCategorizer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GeminiExpenseCategorizer.class
            );

    /*
     * Categorization should remain responsive.
     *
     * M22 deliberately does NOT retry a full timeout, so this
     * cannot become 15 seconds multiplied by the retry count.
     */
    private static final int GEMINI_TIMEOUT_MS = 15_000;

    private final String apiKey;
    private final String model;
    private final JsonMapper jsonMapper;
    private final GeminiResilienceExecutor resilienceExecutor;
    private final GeminiProviderClient providerClient;

    public GeminiExpenseCategorizer(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            JsonMapper jsonMapper,
            GeminiResilienceExecutor resilienceExecutor,
            GeminiProviderClient providerClient
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.jsonMapper = jsonMapper;
        this.resilienceExecutor = resilienceExecutor;
        this.providerClient = providerClient;
    }

    @Override
    public ExpenseCategorizationResult categorize(
            String text,
            LocalDate currentDate
    ) {

        /*
         * Missing local configuration is not a Gemini provider
         * outage and must not consume retry/circuit capacity.
         */
        if (apiKey == null || apiKey.isBlank()) {

            log.warn(
                    "Gemini expense categorization is unavailable because the API key is not configured"
            );

            throw new AiServiceException(
                    "AI categorization is temporarily unavailable"
            );
        }

        if (model == null || model.isBlank()) {

            log.warn(
                    "Gemini expense categorization is unavailable because the model is not configured"
            );

            throw new AiServiceException(
                    "AI categorization is temporarily unavailable"
            );
        }

        try {

            /*
             * Provider invocation, parsing and AI-output validation
             * are protected by the shared Gemini resilience layer.
             *
             * This allows malformed AI output to be classified as
             * non-retryable before it reaches the retry policy.
             */
            return resilienceExecutor.execute(
                    "expense-categorization",
                    () -> callGemini(
                            text,
                            currentDate
                    )
            );

        } catch (GeminiProviderUnavailableException
                 | GeminiNonRetryableException exception) {

            /*
             * Keep provider and resilience implementation details
             * inside the backend.
             */
            throw new AiServiceException(
                    "AI categorization is temporarily unavailable",
                    exception
            );
        }
    }

    private ExpenseCategorizationResult callGemini(
            String text,
            LocalDate currentDate
    ) {

        try {

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .temperature(0.1F)
                            .build();

            String responseText =
                    providerClient.generate(
                            model,
                            buildPrompt(
                                    text,
                                    currentDate
                            ),
                            config,
                            GEMINI_TIMEOUT_MS
                    );

            /*
             * Empty output is invalid AI output.
             *
             * It should not be retried and should not make the
             * provider circuit appear unhealthy.
             */
            if (responseText == null
                    || responseText.isBlank()) {

                throw new GeminiNonRetryableException(
                        "Gemini returned an empty categorization response"
                );
            }

            return parseResponse(
                    responseText
            );

            /*
             * IMPORTANT:
             *
             * These exceptions may already have been classified by
             * GeminiProviderClient or by validation logic below.
             *
             * Preserve their classification instead of allowing the
             * generic RuntimeException handler to convert them.
             */

        } catch (GeminiNonRetryableException exception) {

            throw exception;

        } catch (GeminiTimeoutException exception) {

            throw exception;

        } catch (GeminiProviderUnavailableException exception) {

            throw exception;

        } catch (ApiException exception) {

            /*
             * Temporary compatibility path while Google SDK
             * exception classification is still also supported at
             * the feature boundary.
             */
            throw classifyApiException(
                    exception
            );

        } catch (GenAiIOException exception) {

            if (isTimeout(exception)) {

                log.warn(
                        "Gemini expense categorization request timed out"
                );

                throw new GeminiTimeoutException(
                        "Gemini request timed out",
                        exception
                );
            }

            /*
             * Non-timeout provider I/O failures may be temporary
             * and are therefore eligible for bounded retry.
             */
            log.warn(
                    "Gemini expense categorization encountered a provider I/O failure"
            );

            throw new GeminiProviderUnavailableException(
                    "Gemini provider is temporarily unavailable",
                    exception
            );

        } catch (RuntimeException exception) {

            if (isTimeout(exception)) {

                log.warn(
                        "Gemini expense categorization request timed out"
                );

                throw new GeminiTimeoutException(
                        "Gemini request timed out",
                        exception
                );
            }

            /*
             * Unknown local runtime failures should not cause
             * repeated calls to Gemini.
             */
            log.warn(
                    "Gemini expense categorization failed locally. Cause={}",
                    exception.getClass().getSimpleName()
            );

            throw new GeminiNonRetryableException(
                    "Gemini categorization failed",
                    exception
            );
        }
    }

    private RuntimeException classifyApiException(
            ApiException exception
    ) {

        int statusCode =
                exception.code();

        /*
         * Provider-side timeout:
         *
         * - NO retry
         * - YES circuit-breaker failure
         */
        if (statusCode == 408) {

            log.warn(
                    "Gemini expense categorization timed out with provider status={}",
                    statusCode
            );

            return new GeminiTimeoutException(
                    "Gemini request timed out",
                    exception
            );
        }

        /*
         * Rate limiting and server-side failures are normally
         * transient provider conditions.
         */
        if (statusCode == 429
                || statusCode >= 500) {

            log.warn(
                    "Gemini expense categorization encountered a transient provider failure. Status={}",
                    statusCode
            );

            return new GeminiProviderUnavailableException(
                    "Gemini provider is temporarily unavailable",
                    exception
            );
        }

        /*
         * Other provider responses, especially non-transient 4xx
         * failures, must not trigger another Gemini request.
         */
        log.warn(
                "Gemini expense categorization encountered a non-retryable provider response. Status={}",
                statusCode
        );

        return new GeminiNonRetryableException(
                "Gemini request was rejected",
                exception
        );
    }

    private boolean isTimeout(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        while (current != null) {

            if (current instanceof SocketTimeoutException) {
                return true;
            }

            if (current instanceof java.io.InterruptedIOException) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    private String buildPrompt(
            String text,
            LocalDate currentDate
    ) {

        return """
                You are the expense categorization assistant for ExpenseMate.

                Convert the user's natural-language expense description into one JSON object.

                Current date: %s

                User input:
                %s

                Allowed categories:
                FOOD
                TRAVEL
                SHOPPING
                BILLS
                ENTERTAINMENT
                HEALTH
                EDUCATION
                RENT
                SUBSCRIPTION
                OTHER

                Rules:
                - Return JSON only.
                - Do not use markdown.
                - Do not add explanations.
                - amount must be a positive decimal number.
                - category must be exactly one of the allowed categories.
                - description must be short and useful.
                - expenseDate must use YYYY-MM-DD.
                - If the user does not specify a date, use the current date.
                - Do not invent an amount.
                - Do not invent financial information.

                Required JSON structure:
                {
                  "amount": 0.00,
                  "category": "FOOD",
                  "description": "Short description",
                  "expenseDate": "YYYY-MM-DD"
                }
                """.formatted(
                currentDate,
                text
        );
    }

    private ExpenseCategorizationResult parseResponse(
            String responseText
    ) {

        try {

            String cleanedResponse =
                    cleanJsonResponse(
                            responseText
                    );

            JsonNode root =
                    jsonMapper.readTree(
                            cleanedResponse
                    );

            if (root == null
                    || !root.isObject()) {

                throw new GeminiNonRetryableException(
                        "Gemini categorization response must be a JSON object"
                );
            }

            JsonNode amountNode =
                    root.get("amount");

            JsonNode categoryNode =
                    root.get("category");

            JsonNode descriptionNode =
                    root.get("description");

            JsonNode expenseDateNode =
                    root.get("expenseDate");

            if (amountNode == null
                    || categoryNode == null
                    || descriptionNode == null
                    || expenseDateNode == null) {

                throw new GeminiNonRetryableException(
                        "Gemini categorization response is missing required fields"
                );
            }

            BigDecimal amount =
                    parseAmount(
                            amountNode
                    );

            ExpenseCategory category =
                    parseCategory(
                            categoryNode
                    );

            String description =
                    parseDescription(
                            descriptionNode
                    );

            LocalDate expenseDate =
                    parseExpenseDate(
                            expenseDateNode
                    );

            return new ExpenseCategorizationResult(
                    amount,
                    category,
                    description,
                    expenseDate
            );

        } catch (GeminiNonRetryableException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an invalid categorization response",
                    exception
            );
        }
    }

    private String cleanJsonResponse(
            String responseText
    ) {

        String cleaned =
                responseText.trim();

        /*
         * Defensive cleanup in case the model wraps otherwise valid
         * JSON in a markdown code fence.
         */
        if (cleaned.startsWith("```")) {

            cleaned =
                    cleaned.replaceFirst(
                            "^```(?:json)?\\s*",
                            ""
                    );

            cleaned =
                    cleaned.replaceFirst(
                            "\\s*```$",
                            ""
                    );
        }

        return cleaned.trim();
    }

    private BigDecimal parseAmount(
            JsonNode amountNode
    ) {

        try {

            BigDecimal amount =
                    new BigDecimal(
                            amountNode.asText()
                    );

            if (amount.compareTo(
                    BigDecimal.ZERO
            ) <= 0) {

                throw new GeminiNonRetryableException(
                        "Gemini returned a non-positive expense amount"
                );
            }

            return amount;

        } catch (GeminiNonRetryableException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an invalid expense amount",
                    exception
            );
        }
    }

    private ExpenseCategory parseCategory(
            JsonNode categoryNode
    ) {

        String categoryValue =
                categoryNode.asText();

        if (categoryValue == null
                || categoryValue.isBlank()) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an empty expense category"
            );
        }

        try {

            return ExpenseCategory.valueOf(
                    categoryValue
                            .trim()
                            .toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an unsupported expense category",
                    exception
            );
        }
    }

    private String parseDescription(
            JsonNode descriptionNode
    ) {

        String description =
                descriptionNode.asText();

        if (description == null
                || description.isBlank()) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an empty expense description"
            );
        }

        return description.trim();
    }

    private LocalDate parseExpenseDate(
            JsonNode expenseDateNode
    ) {

        String expenseDateValue =
                expenseDateNode.asText();

        if (expenseDateValue == null
                || expenseDateValue.isBlank()) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an empty expense date"
            );
        }

        try {

            return LocalDate.parse(
                    expenseDateValue.trim()
            );

        } catch (RuntimeException exception) {

            throw new GeminiNonRetryableException(
                    "Gemini returned an invalid expense date",
                    exception
            );
        }
    }
}