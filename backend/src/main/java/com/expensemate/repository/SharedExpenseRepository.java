package com.expensemate.repository;

import com.expensemate.entity.SharedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SharedExpenseRepository
        extends JpaRepository<SharedExpense, Long> {

    List<SharedExpense>
    findByGroupIdOrderByExpenseDateDescCreatedAtDesc(Long groupId);

    List<SharedExpense> findByGroup_Id(Long groupId);

    /*
     * M23 — locate an already-created expense for the
     * authenticated creator and client supplied idempotency key.
     *
     * The database also enforces uniqueness for this same pair.
     */
    Optional<SharedExpense>
    findByCreatedBy_IdAndIdempotencyKey(
            Long createdByUserId,
            String idempotencyKey
    );
}