package com.expensemate.service;

import com.expensemate.dto.PersonalExpenseRequest;
import com.expensemate.dto.PersonalExpenseResponse;
import com.expensemate.entity.ExpenseCategory;
import com.expensemate.entity.PersonalExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.PersonalExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.ai.MonthlyInsightCacheService;
import com.expensemate.specification.PersonalExpenseSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PersonalExpenseService {

    private final PersonalExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final MonthlyInsightCacheService monthlyInsightCacheService;

    public PersonalExpenseService(
            PersonalExpenseRepository expenseRepository,
            UserRepository userRepository,
            MonthlyInsightCacheService monthlyInsightCacheService
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.monthlyInsightCacheService =
                monthlyInsightCacheService;
    }

    @Transactional
    public PersonalExpenseResponse createExpense(
            String email,
            PersonalExpenseRequest request
    ) {

        User user = getUserByEmail(email);

        LocalDateTime now = LocalDateTime.now();

        PersonalExpense expense = new PersonalExpense(
                user,
                request.getAmount(),
                request.getCategory(),
                request.getExpenseDate(),
                normalizeDescription(request.getDescription()),
                now,
                now
        );

        PersonalExpense savedExpense =
                expenseRepository.save(expense);

        /*
         * An expense created in month M changes:
         *
         * 1. The insight for month M.
         * 2. The insight for month M + 1 because that insight
         *    uses month M as its previous-month spending.
         */
        monthlyInsightCacheService
                .invalidateForExpenseChange(
                        user.getId(),
                        savedExpense.getExpenseDate()
                );

        return toResponse(savedExpense);
    }

    @Transactional(readOnly = true)
    public PersonalExpenseResponse getExpenseById(
            String email,
            Long expenseId
    ) {

        User user = getUserByEmail(email);

        PersonalExpense expense =
                expenseRepository.findByIdAndUserId(
                                expenseId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Expense not found"
                                )
                        );

        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public Page<PersonalExpenseResponse> getExpenses(
            String email,
            ExpenseCategory category,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            Pageable pageable
    ) {

        User user = getUserByEmail(email);

        if (startDate != null
                && endDate != null
                && startDate.isAfter(endDate)) {

            throw new InvalidRequestException(
                    "Start date must not be after end date"
            );
        }

        Specification<PersonalExpense> specification =
                PersonalExpenseSpecification
                        .belongsToUser(user.getId())
                        .and(
                                PersonalExpenseSpecification
                                        .hasCategory(category)
                        )
                        .and(
                                PersonalExpenseSpecification
                                        .dateFrom(startDate)
                        )
                        .and(
                                PersonalExpenseSpecification
                                        .dateTo(endDate)
                        )
                        .and(
                                PersonalExpenseSpecification
                                        .descriptionContains(search)
                        );

        return expenseRepository
                .findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PersonalExpenseResponse updateExpense(
            String email,
            Long expenseId,
            PersonalExpenseRequest request
    ) {

        User user = getUserByEmail(email);

        PersonalExpense expense =
                expenseRepository.findByIdAndUserId(
                                expenseId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Expense not found"
                                )
                        );

        /*
         * Capture the old date BEFORE modifying the expense.
         *
         * This is required when an expense moves from one
         * month to another.
         *
         * Example:
         * August -> September
         *
         * We must invalidate:
         * August, September and October.
         */
        LocalDate oldExpenseDate =
                expense.getExpenseDate();

        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setDescription(
                normalizeDescription(
                        request.getDescription()
                )
        );
        expense.setUpdatedAt(
                LocalDateTime.now()
        );

        PersonalExpense updatedExpense =
                expenseRepository.save(expense);

        monthlyInsightCacheService
                .invalidateForExpenseChange(
                        user.getId(),
                        oldExpenseDate,
                        updatedExpense.getExpenseDate()
                );

        return toResponse(updatedExpense);
    }

    @Transactional
    public void deleteExpense(
            String email,
            Long expenseId
    ) {

        User user = getUserByEmail(email);

        PersonalExpense expense =
                expenseRepository.findByIdAndUserId(
                                expenseId,
                                user.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Expense not found"
                                )
                        );

        /*
         * Keep the affected date before deleting the entity.
         */
        LocalDate expenseDate =
                expense.getExpenseDate();

        expenseRepository.delete(expense);

        monthlyInsightCacheService
                .invalidateForExpenseChange(
                        user.getId(),
                        expenseDate
                );
    }

    private User getUserByEmail(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private PersonalExpenseResponse toResponse(
            PersonalExpense expense
    ) {

        return new PersonalExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getDescription(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }

    private String normalizeDescription(
            String description
    ) {

        if (description == null) {
            return null;
        }

        String trimmed =
                description.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}