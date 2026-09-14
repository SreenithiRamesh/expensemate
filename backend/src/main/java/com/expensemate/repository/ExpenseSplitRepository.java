package com.expensemate.repository;

import com.expensemate.entity.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSplitRepository
        extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByExpenseId(Long expenseId);
    List<ExpenseSplit> findByExpense_Group_Id(Long groupId);
}