package com.expensemate.dto.activity;

import com.expensemate.enums.ActivityType;

import java.time.LocalDateTime;

public record GroupActivityResponse(

        Long id,

        Long groupId,

        Long actorUserId,
        String actorName,

        ActivityType activityType,

        String description,

        Long referenceId,

        LocalDateTime createdAt

) {
}