package com.expensemate.repository;

import com.expensemate.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository
        extends JpaRepository<RecurringExpense, Long> {

    Optional<RecurringExpense> findByIdAndUserId(
            Long id,
            Long userId
    );

    List<RecurringExpense>
    findByUserIdOrderByNextDueDateAscIdAsc(
            Long userId
    );

    List<RecurringExpense>
    findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAscIdAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}