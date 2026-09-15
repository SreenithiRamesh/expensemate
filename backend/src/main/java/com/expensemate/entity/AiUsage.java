package com.expensemate.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_usage_user_date",
                        columnNames = {"user_id", "usage_date"}
                )
        }
)
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiUsage() {
    }

    public AiUsage(
            User user,
            LocalDate usageDate,
            int requestCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.user = user;
        this.usageDate = usageDate;
        this.requestCount = requestCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void increment() {
        this.requestCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}