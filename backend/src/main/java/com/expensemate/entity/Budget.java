package com.expensemate.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_budget_user_category_month_year",
                        columnNames = {
                                "user_id",
                                "category",
                                "month",
                                "year"
                        }
                )
        }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private ExpenseCategory category;

    @Column(
            name = "monthly_limit",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal monthlyLimit;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected Budget() {
    }

    public Budget(
            User user,
            ExpenseCategory category,
            BigDecimal monthlyLimit,
            Integer month,
            Integer year,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.user = user;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
        this.month = month;
        this.year = year;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(
            ExpenseCategory category
    ) {
        this.category = category;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(
            BigDecimal monthlyLimit
    ) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(
            Integer month
    ) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(
            Integer year
    ) {
        this.year = year;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}