package com.expensemate.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "expense_splits",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_expense_split_expense_user",
                        columnNames = {"expense_id", "user_id"}
                )
        }
)
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private SharedExpense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "share_amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal shareAmount;

    @Column(
            name = "percentage",
            precision = 7,
            scale = 4
    )
    private BigDecimal percentage;

    protected ExpenseSplit() {
    }

    public ExpenseSplit(
            SharedExpense expense,
            User user,
            BigDecimal shareAmount,
            BigDecimal percentage
    ) {
        this.expense = expense;
        this.user = user;
        this.shareAmount = shareAmount;
        this.percentage = percentage;
    }

    public Long getId() {
        return id;
    }

    public SharedExpense getExpense() {
        return expense;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }
}