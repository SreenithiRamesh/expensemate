package com.expensemate.dto;

import java.time.LocalDateTime;

public class GroupSummaryResponse {

    private Long id;
    private String name;
    private String description;

    private Long createdByUserId;
    private String createdByName;

    private long memberCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GroupSummaryResponse(
            Long id,
            String name,
            String description,
            Long createdByUserId,
            String createdByName,
            long memberCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}