package com.expensemate.dto.dashboard;

import com.expensemate.enums.ActivityType;

import java.time.LocalDateTime;

public record DashboardActivityResponse(
        Long id,
        Long groupId,
        String groupName,
        String actorName,
        ActivityType activityType,
        String description,
        Long referenceId,
        LocalDateTime createdAt
) {
}