package com.expensemate.dto;

import java.time.LocalDateTime;

public class GroupMemberResponse {

    private Long userId;
    private String name;
    private String email;
    private LocalDateTime joinedAt;

    public GroupMemberResponse(
            Long userId,
            String name,
            String email,
            LocalDateTime joinedAt
    ) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.joinedAt = joinedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}