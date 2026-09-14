package com.expensemate.entity;

import com.expensemate.enums.ActivityType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_activity")
public class GroupActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GroupActivity() {
    }

    public GroupActivity(
            ExpenseGroup group,
            User actor,
            ActivityType activityType,
            String description,
            Long referenceId
    ) {
        this.group = group;
        this.actor = actor;
        this.activityType = activityType;
        this.description = description;
        this.referenceId = referenceId;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getActor() {
        return actor;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getDescription() {
        return description;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}