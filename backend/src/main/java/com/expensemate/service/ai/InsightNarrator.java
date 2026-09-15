package com.expensemate.service.ai;

import com.expensemate.dto.ai.MonthlyInsightData;

public interface InsightNarrator {

    String narrate(MonthlyInsightData data);
}