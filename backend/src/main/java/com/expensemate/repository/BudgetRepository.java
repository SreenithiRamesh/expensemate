package com.expensemate.repository;

import com.expensemate.entity.Budget;
import com.expensemate.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository
        extends JpaRepository<Budget, Long> {

    @Query("""
            SELECT b
            FROM Budget b
            WHERE b.id = :budgetId
              AND b.user.id = :userId
            """)
    Optional<Budget> findOwnedBudget(
            @Param("budgetId") Long budgetId,
            @Param("userId") Long userId
    );

    boolean existsByUserIdAndCategoryAndMonthAndYear(
            Long userId,
            ExpenseCategory category,
            Integer month,
            Integer year
    );

    @Query("""
            SELECT b
            FROM Budget b
            WHERE b.user.id = :userId
            ORDER BY b.year DESC, b.month DESC, b.category ASC
            """)
    List<Budget> findAllForUser(
            @Param("userId") Long userId
    );

    @Query("""
            SELECT b
            FROM Budget b
            WHERE b.user.id = :userId
              AND b.month = :month
              AND b.year = :year
            ORDER BY b.category ASC
            """)
    List<Budget> findAllForUserAndPeriod(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}