package com.expensemate.entity;

import com.expensemate.enums.SplitType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_expenses")
public class SharedExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    /*
     * M23 — authenticated user who actually created
     * the shared expense.
     *
     * This is intentionally separate from paidBy because
     * a group member may record an expense paid by another
     * member.
     *
     * Nullable at the database/entity level so historical
     * pre-M23 rows remain valid.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /*
     * M23 — client supplied idempotency key.
     *
     * Together with createdBy, the database UNIQUE
     * constraint prevents duplicate financial writes
     * for the same authenticated user.
     */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    /*
     * SHA-256 fingerprint of the logical create request.
     *
     * It allows us to distinguish:
     *
     * same key + same request      -> safe replay
     * same key + different request -> reject
     */
    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 30)
    private SplitType splitType;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SharedExpense() {
    }

    public SharedExpense(
            ExpenseGroup group,
            User paidBy,
            User createdBy,
            String title,
            BigDecimal amount,
            SplitType splitType,
            LocalDate expenseDate,
            String idempotencyKey,
            String requestFingerprint
    ) {
        this.group = group;
        this.paidBy = paidBy;
        this.createdBy = createdBy;
        this.title = title;
        this.amount = amount;
        this.splitType = splitType;
        this.expenseDate = expenseDate;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getTitle() {
        return title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}