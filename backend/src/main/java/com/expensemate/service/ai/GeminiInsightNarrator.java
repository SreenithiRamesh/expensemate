package com.expensemate.service.ai;

import com.expensemate.dto.ai.BudgetInsightData;
import com.expensemate.dto.ai.CategoryInsightData;
import com.expensemate.dto.ai.MonthlyInsightData;
import com.expensemate.exception.AiServiceException;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GeminiInsightNarrator implements InsightNarrator {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiInsightNarrator.class);

    private static final int GEMINI_TIMEOUT_MS = 60_000;

    private final String apiKey;
    private final String model;

    public GeminiInsightNarrator(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String narrate(MonthlyInsightData data) {

        if (data == null) {
            throw new AiServiceException(
                    "Monthly insight data is unavailable"
            );
        }

        if (apiKey == null || apiKey.isBlank()) {

            log.warn(
                    "Gemini monthly insight is unavailable because the API key is not configured"
            );

            throw new AiServiceException(
                    "AI monthly insight is temporarily unavailable"
            );
        }

        if (model == null || model.isBlank()) {

            log.warn(
                    "Gemini monthly insight is unavailable because the model is not configured"
            );

            throw new AiServiceException(
                    "AI monthly insight is temporarily unavailable"
            );
        }

        try {

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
                            .temperature(0.2F)
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            model,
                            buildPrompt(data),
                            config
                    );

            String responseText = response.text();

            if (responseText == null
                    || responseText.isBlank()) {

                throw new AiServiceException(
                        "AI monthly insight returned an empty response"
                );
            }

            return responseText.trim();

        } catch (AiServiceException exception) {

            throw exception;

        } catch (Exception exception) {

            log.warn(
                    "Gemini monthly insight failed. Exception={}",
                    exception.getClass().getSimpleName()
            );

            throw new AiServiceException(
                    "AI monthly insight is temporarily unavailable",
                    exception
            );
        }
    }

    private String buildPrompt(
            MonthlyInsightData data
    ) {

        String categories =
                data.categorySpending().isEmpty()
                        ? "No category spending recorded."
                        : data.categorySpending()
                        .stream()
                        .map(this::formatCategory)
                        .collect(
                                Collectors.joining("\n")
                        );

        String budgets =
                data.budgets().isEmpty()
                        ? "No budgets configured."
                        : data.budgets()
                        .stream()
                        .map(this::formatBudget)
                        .collect(
                                Collectors.joining("\n")
                        );

        return """
                You are the monthly spending insight narrator for ExpenseMate.

                Java has already calculated every financial value below.
                Treat these values as the source of truth.

                Your job is ONLY to explain the supplied facts clearly.
                Do not recalculate totals, percentages, differences, or budgets.
                Do not invent transactions, categories, reasons, goals, or financial facts.

                Month: %s
                Total spending: %s
                Previous month spending: %s
                Spending change: %s

                Category spending:
                %s

                Budget status:
                %s

                Rules:
                - Write a concise monthly spending insight.
                - Use approximately 2 to 4 short sentences.
                - Mention the most useful spending pattern supported by the supplied data.
                - If spendingChange is positive, spending increased by that supplied amount.
                - If spendingChange is negative, spending decreased by the absolute value of that supplied amount.
                - If spendingChange is zero, spending was unchanged.
                - Mention budget pressure only when supported by the supplied budget status.
                - If there is no spending, say so naturally.
                - Do not give investment, credit, loan, tax, or medical advice.
                - Do not shame or judge the user.
                - Do not perform calculations.
                - Return only the narration.
                """.formatted(
                data.month(),
                data.totalSpending(),
                data.previousMonthSpending(),
                data.spendingChange(),
                categories,
                budgets
        );
    }

    private String formatCategory(
            CategoryInsightData category
    ) {

        return "- %s: amount=%s, percentage=%s%%"
                .formatted(
                        category.category(),
                        category.amount(),
                        category.percentage()
                );
    }

    private String formatBudget(
            BudgetInsightData budget
    ) {

        return "- %s: limit=%s, spent=%s, remaining=%s, percentageUsed=%s%%, status=%s"
                .formatted(
                        budget.category(),
                        budget.monthlyLimit(),
                        budget.spent(),
                        budget.remaining(),
                        budget.percentageUsed(),
                        budget.status()
                );
    }
}