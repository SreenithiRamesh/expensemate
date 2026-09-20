package com.expensemate.repository;

import com.expensemate.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseSplitRepository
        extends JpaRepository<ExpenseSplit, Long> {

    /*
     * SharedExpenseService maps split user details after this
     * repository call returns.
     *
     * Fetch the user in the same query so response mapping does
     * not depend on Open Session in View or an outer transaction.
     */
    @Query("""
            SELECT split
            FROM ExpenseSplit split
            JOIN FETCH split.user
            WHERE split.expense.id = :expenseId
            """)
    List<ExpenseSplit> findByExpenseId(
            @Param("expenseId")
            Long expenseId
    );

    @Query("""
            SELECT split
            FROM ExpenseSplit split
            JOIN FETCH split.user
            WHERE split.expense.group.id = :groupId
            """)
    List<ExpenseSplit> findByExpense_Group_Id(
            @Param("groupId")
            Long groupId
    );
}