package com.expensemate.repository;

import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.PersonalExpense;
import com.expensemate.repository.projection.CategorySpendingProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.expensemate.repository.projection.MonthlyTrendProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM PersonalExpense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate < :endDate
            """)
    BigDecimal sumExpensesForPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT COUNT(e)
            FROM PersonalExpense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate < :endDate
            """)
    long countExpensesForPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT
                e.category AS category,
                SUM(e.amount) AS amount
            FROM PersonalExpense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate < :endDate
            GROUP BY e.category
            ORDER BY SUM(e.amount) DESC
            """)
    List<CategorySpendingProjection> findCategorySpending(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            SELECT
                YEAR(e.expenseDate) AS year,
                MONTH(e.expenseDate) AS month,
                SUM(e.amount) AS amount
            FROM PersonalExpense e
            WHERE e.user.id = :userId
              AND e.expenseDate >= :startDate
              AND e.expenseDate < :endDate
            GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate)
            ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)
            """)
    List<MonthlyTrendProjection> findMonthlyTrend(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}