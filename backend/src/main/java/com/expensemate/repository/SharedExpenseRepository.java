package com.expensemate.repository;

import com.expensemate.entity.SharedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SharedExpenseRepository
        extends JpaRepository<SharedExpense, Long> {

    List<SharedExpense>
    findByGroupIdOrderByExpenseDateDescCreatedAtDesc(Long groupId);
    List<SharedExpense> findByGroup_Id(Long groupId);
}