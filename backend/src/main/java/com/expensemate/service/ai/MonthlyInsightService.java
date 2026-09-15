package com.expensemate.service.ai;

import com.expensemate.dto.ai.MonthlyInsightResponse;

import java.time.YearMonth;

public interface MonthlyInsightService {

    MonthlyInsightResponse getMonthlyInsight(
            String currentUserEmail,
            YearMonth month
    );
}