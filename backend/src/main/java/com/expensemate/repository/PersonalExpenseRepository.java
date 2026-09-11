package com.expensemate.repository;

import com.expensemate.entity.PersonalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PersonalExpenseRepository
        extends JpaRepository<PersonalExpense, Long>,
        JpaSpecificationExecutor<PersonalExpense> {

    Optional<PersonalExpense> findByIdAndUserId(
            Long id,
            Long userId
    );
}