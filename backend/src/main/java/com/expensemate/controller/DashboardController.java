package com.expensemate.controller;

import com.expensemate.dto.dashboard.DashboardResponse;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse getDashboard(
            @RequestParam(required = false)
            String month,
            Authentication authentication
    ) {

        YearMonth selectedMonth = null;

        if (month != null && !month.isBlank()) {
            try {
                selectedMonth = YearMonth.parse(month);
            } catch (DateTimeParseException exception) {
                throw new InvalidRequestException(
                        "Month must use YYYY-MM format"
                );
            }
        }

        return dashboardService.getDashboard(
                authentication.getName(),
                selectedMonth
        );
    }
}