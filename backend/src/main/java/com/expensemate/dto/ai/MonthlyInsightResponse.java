package com.expensemate.dto.ai;

import java.time.YearMonth;

public record MonthlyInsightResponse(

        YearMonth month,

        String insight,

        boolean cached

) {
}