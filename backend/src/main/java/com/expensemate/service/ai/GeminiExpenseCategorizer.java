package com.expensemate.service.ai;

import com.expensemate.dto.ai.ExpenseCategorizationResult;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.exception.AiServiceException;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class GeminiExpenseCategorizer implements ExpenseCategorizer {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiExpenseCategorizer.class);

    private static final int GEMINI_TIMEOUT_MS = 15_000;

    private final String apiKey;
    private final String model;
    private final JsonMapper jsonMapper;

    public GeminiExpenseCategorizer(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            JsonMapper jsonMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ExpenseCategorizationResult categorize(
            String text,
            LocalDate currentDate
    ) {

        /*
         * Gemini is optional infrastructure.
         * A missing API key must not prevent ExpenseMate
         * from starting or affect non-AI features.
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
             * Apply a client-level timeout so a slow or unavailable
             * Gemini service cannot keep the ExpenseMate request
             * waiting indefinitely.
             */
            HttpOptions httpOptions =
                    HttpOptions.builder()
                            .timeout(GEMINI_TIMEOUT_MS)
                            .build();

            Client client =
                    Client.builder()
                            .apiKey(apiKey)
                            .httpOptions(httpOptions)
                            .build();

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .responseMimeType("application/json")
                            .responseJsonSchema(buildSchema())
                            .temperature(0.1F)
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            model,
                            buildPrompt(text, currentDate),
                            config
                    );

            String responseText = response.text();

            if (responseText == null || responseText.isBlank()) {
                throw new AiServiceException(
                        "AI categorization returned an empty response"
                );
            }

            return parseResponse(responseText);

        } catch (AiServiceException exception) {
            throw exception;

        } catch (Exception exception) {

            /*
             * Provider-specific errors, network failures,
             * timeouts and quota errors stay inside the server.
             *
             * The API client receives a stable application-level
             * error instead of Gemini implementation details.
             */
            log.warn(
                    "Gemini expense categorization failed. Cause: {}",
                    exception.getClass().getSimpleName()
            );

            throw new AiServiceException(
                    "AI categorization is temporarily unavailable",
                    exception
            );
        }
    }

    private Map<String, Object> buildSchema() {

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "amount", Map.of(
                                "type", "number",
                                "minimum", 0.01
                        ),
                        "category", Map.of(
                                "type", "string",
                                "enum", List.of(
                                        "FOOD",
                                        "TRAVEL",
                                        "SHOPPING",
                                        "BILLS",
                                        "ENTERTAINMENT",
                                        "HEALTH",
                                        "EDUCATION",
                                        "RENT",
                                        "SUBSCRIPTION",
                                        "OTHER"
                                )
                        ),
                        "description", Map.of(
                                "type", "string"
                        ),
                        "expenseDate", Map.of(
                                "type", "string",
                                "format", "date"
                        )
                ),
                "required", List.of(
                        "amount",
                        "category",
                        "description",
                        "expenseDate"
                ),
                "additionalProperties", false
        );
    }

    private String buildPrompt(
            String text,
            LocalDate currentDate
    ) {

        return """
                You are the expense categorization component of ExpenseMate.

                Convert the user's natural-language expense into structured data.

                Current date: %s

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
                - Extract only information supported by the user's text.
                - amount must be a positive monetary number.
                - Choose exactly one allowed category.
                - Keep description short and useful.
                - Resolve words such as "today" using the current date.
                - If no date is mentioned, use the current date.
                - Do not perform database operations.
                - Return only data matching the requested JSON schema.

                User expense:
                %s
                """.formatted(currentDate, text);
    }

    private ExpenseCategorizationResult parseResponse(
            String responseText
    ) {

        try {
            JsonNode json = jsonMapper.readTree(responseText);

            /*
             * Even though Gemini is asked to follow a JSON schema,
             * Java still validates all required fields before
             * accepting the AI-generated result.
             */
            if (json == null
                    || json.get("amount") == null
                    || json.get("category") == null
                    || json.get("description") == null
                    || json.get("expenseDate") == null) {

                throw new AiServiceException(
                        "AI returned an invalid categorization response"
                );
            }

            BigDecimal amount =
                    json.get("amount").decimalValue();

            ExpenseCategory category =
                    ExpenseCategory.valueOf(
                            json.get("category").asText()
                    );

            String description =
                    json.get("description")
                            .asText()
                            .trim();

            LocalDate expenseDate =
                    LocalDate.parse(
                            json.get("expenseDate").asText()
                    );

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AiServiceException(
                        "AI returned an invalid expense amount"
                );
            }

            /*
             * ExpenseMate stores monetary values with at most
             * two decimal places. Do not silently round an
             * unexpected AI-generated amount.
             */
            if (amount.scale() > 2) {
                throw new AiServiceException(
                        "AI returned an invalid expense amount"
                );
            }

            if (description.isBlank()) {
                throw new AiServiceException(
                        "AI returned an invalid expense description"
                );
            }

            return new ExpenseCategorizationResult(
                    amount.setScale(2),
                    category,
                    description,
                    expenseDate
            );

        } catch (AiServiceException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new AiServiceException(
                    "AI returned an invalid categorization response",
                    exception
            );
        }
    }
}