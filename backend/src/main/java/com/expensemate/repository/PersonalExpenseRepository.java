package com.expensemate.repository;

import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.PersonalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface PersonalExpenseRepository
        extends JpaRepository<PersonalExpense, Long>,
        JpaSpecificationExecutor<PersonalExpense> {

    Optional<PersonalExpense> findByIdAndUserId(
            Long id,
            Long userId
    );

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM PersonalExpense e
            WHERE e.user.id = :userId
              AND e.category = :category
              AND e.expenseDate >= :startDate
              AND e.expenseDate <= :endDate
            """)
    BigDecimal calculateTotalSpent(
            @Param("userId")
            Long userId,

            @Param("category")
            ExpenseCategory category,

            @Param("startDate")
            LocalDate startDate,

            @Param("endDate")
            LocalDate endDate
    );
}