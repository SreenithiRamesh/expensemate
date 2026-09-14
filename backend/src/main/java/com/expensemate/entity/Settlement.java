package com.expensemate.entity;

import com.expensemate.enums.SettlementMode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlement_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "group_id",
            nullable = false
    )
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "from_user_id",
            nullable = false
    )
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "to_user_id",
            nullable = false
    )
    private User toUser;

    @Column(
            name = "amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "settlement_mode",
            nullable = false,
            length = 20
    )
    private SettlementMode settlementMode;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by",
            nullable = false
    )
    private User createdBy;

    @Column(
            name = "settled_at",
            nullable = false
    )
    private LocalDateTime settledAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    protected Settlement() {
    }

    public Settlement(
            ExpenseGroup group,
            User fromUser,
            User toUser,
            BigDecimal amount,
            SettlementMode settlementMode,
            String idempotencyKey,
            User createdBy
    ) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.settlementMode = settlementMode;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
    }

    @PrePersist
    private void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (settledAt == null) {
            settledAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SettlementMode getSettlementMode() {
        return settlementMode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}