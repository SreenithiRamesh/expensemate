package com.expensemate.repository;

import com.expensemate.entity.SharedExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SharedExpenseRepository
        extends JpaRepository<SharedExpense, Long> {

    List<SharedExpense>
    findByGroupIdOrderByExpenseDateDescCreatedAtDesc(
            Long groupId
    );

    List<SharedExpense> findByGroup_Id(
            Long groupId
    );

    /*
     * Fetch every relationship required while constructing the
     * response outside a surrounding service transaction.
     *
     * This is particularly important during concurrent
     * idempotency replay because the winning expense is loaded
     * after the losing write transaction has rolled back.
     */
    @Query("""
            SELECT expense
            FROM SharedExpense expense
            JOIN FETCH expense.group
            JOIN FETCH expense.paidBy
            LEFT JOIN FETCH expense.createdBy
            WHERE expense.createdBy.id = :createdByUserId
              AND expense.idempotencyKey = :idempotencyKey
            """)
    Optional<SharedExpense>
    findByCreatedBy_IdAndIdempotencyKey(
            @Param("createdByUserId")
            Long createdByUserId,

            @Param("idempotencyKey")
            String idempotencyKey
    );
}