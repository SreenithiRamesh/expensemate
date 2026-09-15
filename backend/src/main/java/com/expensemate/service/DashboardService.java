package com.expensemate.service;

import com.expensemate.dto.dashboard.DashboardResponse;

import java.time.YearMonth;

public interface DashboardService {

    DashboardResponse getDashboard(
            String currentUserEmail,
            YearMonth month
    );
}