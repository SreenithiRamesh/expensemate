package com.expensemate.repository;

import com.expensemate.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseGroupRepository
        extends JpaRepository<ExpenseGroup, Long> {
}