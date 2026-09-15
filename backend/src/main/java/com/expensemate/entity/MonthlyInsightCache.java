package com.expensemate.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "monthly_insight_cache",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_insight_user_month",
                        columnNames = {"user_id", "insight_month"}
                )
        }
)
public class MonthlyInsightCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "insight_month", nullable = false, length = 7)
    private String insightMonth;

    @Column(name = "insight", nullable = false, columnDefinition = "TEXT")
    private String insight;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MonthlyInsightCache() {
    }

    public MonthlyInsightCache(
            User user,
            String insightMonth,
            String insight
    ) {
        this.user = user;
        this.insightMonth = insightMonth;
        this.insight = insight;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getInsightMonth() {
        return insightMonth;
    }

    public String getInsight() {
        return insight;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}