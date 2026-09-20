package com.expensemate.service.ai;

import com.expensemate.dto.ai.BudgetInsightData;
import com.expensemate.dto.ai.CategoryInsightData;
import com.expensemate.dto.ai.MonthlyInsightData;
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

import java.net.SocketTimeoutException;
import java.util.stream.Collectors;

@Component
public class GeminiInsightNarrator implements InsightNarrator {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiInsightNarrator.class);

    /*
     * Keep the existing M16 timeout.
     *
     * M22 guarantees that a full timeout is NOT retried,
     * so this can no longer become 60s x 3 attempts.
     */
    private static final int GEMINI_TIMEOUT_MS = 60_000;

    private final String apiKey;
    private final String model;
    private final GeminiResilienceExecutor resilienceExecutor;
    private final GeminiProviderClient providerClient;

    public GeminiInsightNarrator(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            GeminiResilienceExecutor resilienceExecutor,
            GeminiProviderClient providerClient
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.resilienceExecutor = resilienceExecutor;
        this.providerClient = providerClient;
    }

    @Override
    public String narrate(
            MonthlyInsightData data
    ) {

        /*
         * MonthlyInsightData is calculated by Java before Gemini
         * becomes involved.
         *
         * Invalid local input is therefore not a Gemini provider
         * failure and must never consume retry/circuit capacity.
         */
        if (data == null) {

            throw new AiServiceException(
                    "Monthly insight data is unavailable"
            );
        }

        /*
         * Gemini is optional infrastructure.
         *
         * Missing local configuration must not trigger retries or
         * affect the circuit breaker.
         */
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

            /*
             * Both ExpenseMate Gemini features pass through the
             * same explicit resilience boundary.
             */
            return resilienceExecutor.execute(
                    "monthly-insight-narration",
                    () -> callGemini(data)
            );

        } catch (GeminiProviderUnavailableException
                 | GeminiNonRetryableException exception) {

            /*
             * Provider/Resilience4j implementation details remain
             * internal to the backend.
             */
            throw new AiServiceException(
                    "AI monthly insight is temporarily unavailable",
                    exception
            );
        }
    }

    private String callGemini(
            MonthlyInsightData data
    ) {

        try {

            /*
             * The low-level Gemini SDK call is delegated to
             * GeminiProviderClient.
             *
             * This class remains responsible only for the
             * monthly-insight prompt and output validation.
             */
            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .temperature(0.2F)
                            .build();

            String responseText =
                    providerClient.generate(
                            model,
                            buildPrompt(data),
                            config,
                            GEMINI_TIMEOUT_MS
                    );

            /*
             * The provider completed the request, but an empty
             * narration is invalid output rather than evidence that
             * Gemini itself is unavailable.
             *
             * Do not retry it and do not count it against the
             * provider circuit.
             */
            if (responseText == null
                    || responseText.isBlank()) {

                throw new GeminiNonRetryableException(
                        "Gemini returned an empty monthly insight"
                );
            }

            return responseText.trim();

            /*
             * Preserve already-classified resilience exceptions.
             *
             * These must reach GeminiResilienceExecutor unchanged so
             * it can apply the correct retry/circuit-breaker policy.
             */
        } catch (GeminiNonRetryableException exception) {

            throw exception;

        } catch (GeminiTimeoutException exception) {

            throw exception;

        } catch (GeminiProviderUnavailableException exception) {

            throw exception;

        } catch (ApiException exception) {

            throw classifyApiException(exception);

        } catch (GenAiIOException exception) {

            if (isTimeout(exception)) {

                log.warn(
                        "Gemini monthly insight request timed out"
                );

                throw new GeminiTimeoutException(
                        "Gemini request timed out",
                        exception
                );
            }

            /*
             * Non-timeout I/O failures can be temporary.
             * They are eligible for the bounded retry policy.
             */
            log.warn(
                    "Gemini monthly insight encountered a provider I/O failure"
            );

            throw new GeminiProviderUnavailableException(
                    "Gemini provider is temporarily unavailable",
                    exception
            );

        } catch (RuntimeException exception) {

            if (isTimeout(exception)) {

                log.warn(
                        "Gemini monthly insight request timed out"
                );

                throw new GeminiTimeoutException(
                        "Gemini request timed out",
                        exception
                );
            }

            /*
             * An unknown local runtime failure should not cause
             * repeated calls to Gemini.
             */
            log.warn(
                    "Gemini monthly insight failed locally. Cause={}",
                    exception.getClass().getSimpleName()
            );

            throw new GeminiNonRetryableException(
                    "Gemini monthly insight failed",
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
         * Provider-side request timeout:
         *
         * - counts toward circuit health
         * - does NOT retry
         */
        if (statusCode == 408) {

            log.warn(
                    "Gemini monthly insight timed out with provider status={}",
                    statusCode
            );

            return new GeminiTimeoutException(
                    "Gemini request timed out",
                    exception
            );
        }

        /*
         * 429 and 5xx responses are normally transient provider
         * conditions and may use the bounded retry policy.
         */
        if (statusCode == 429
                || statusCode >= 500) {

            log.warn(
                    "Gemini monthly insight encountered a transient provider failure. Status={}",
                    statusCode
            );

            return new GeminiProviderUnavailableException(
                    "Gemini provider is temporarily unavailable",
                    exception
            );
        }

        /*
         * Other provider responses, especially non-transient 4xx
         * failures, should not be retried.
         */
        log.warn(
                "Gemini monthly insight encountered a non-retryable provider response. Status={}",
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

            /*
             * HTTP clients may expose timeout/interruption through
             * InterruptedIOException rather than SocketTimeoutException.
             */
            if (current instanceof java.io.InterruptedIOException) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }

    /*
     * Everything below remains deterministic M16 prompt construction.
     *
     * Gemini narrates these Java-calculated facts.
     * It does not calculate the financial values.
     */
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