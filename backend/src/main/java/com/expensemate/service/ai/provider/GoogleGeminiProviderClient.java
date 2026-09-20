package com.expensemate.service.ai.provider;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleGeminiProviderClient implements GeminiProviderClient {

    private final String apiKey;

    public GoogleGeminiProviderClient(
            @Value("${gemini.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;
    }

    @Override
    public String generate(
            String model,
            String prompt,
            GenerateContentConfig config,
            int timeoutMs
    ) {

        HttpOptions httpOptions =
                HttpOptions.builder()
                        .timeout(timeoutMs)
                        .build();

        Client client =
                Client.builder()
                        .apiKey(apiKey)
                        .httpOptions(httpOptions)
                        .build();

        GenerateContentResponse response =
                client.models.generateContent(
                        model,
                        prompt,
                        config
                );

        return response == null
                ? null
                : response.text();
    }
}