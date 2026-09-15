package com.expensemate.controller;

import com.expensemate.dto.ai.MonthlyInsightResponse;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.service.ai.MonthlyInsightService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/ai/insights")
public class MonthlyInsightController {

    private final MonthlyInsightService monthlyInsightService;

    public MonthlyInsightController(
            MonthlyInsightService monthlyInsightService
    ) {
        this.monthlyInsightService =
                monthlyInsightService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyInsightResponse> getMonthlyInsight(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            YearMonth month
    ) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new InvalidRequestException(
                    "Authenticated user is required"
            );
        }

        MonthlyInsightResponse response =
                monthlyInsightService
                        .getMonthlyInsight(
                                authentication.getName(),
                                month
                        );

        return ResponseEntity.ok(response);
    }
}