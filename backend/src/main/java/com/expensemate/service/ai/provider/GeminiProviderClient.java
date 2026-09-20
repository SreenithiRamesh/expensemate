package com.expensemate.service.ai.provider;

import com.google.genai.types.GenerateContentConfig;

public interface GeminiProviderClient {

    String generate(
            String model,
            String prompt,
            GenerateContentConfig config,
            int timeoutMs
    );
}