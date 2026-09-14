package com.expensemate.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupDetailsResponse {

    private Long id;
    private String name;
    private String description;

    private Long createdByUserId;
    private String createdByName;
    private String createdByEmail;

    private List<GroupMemberResponse> members;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GroupDetailsResponse(
            Long id,
            String name,
            String description,
            Long createdByUserId,
            String createdByName,
            String createdByEmail,
            List<GroupMemberResponse> members,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
        this.createdByEmail = createdByEmail;
        this.members = members;
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

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public List<GroupMemberResponse> getMembers() {
        return members;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}