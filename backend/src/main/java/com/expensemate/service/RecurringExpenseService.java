package com.expensemate.service;

import com.expensemate.dto.PersonalExpenseRequest;
import com.expensemate.dto.RecurringExpenseRequest;
import com.expensemate.dto.RecurringExpenseResponse;
import com.expensemate.dto.RecurringPaymentRequest;
import com.expensemate.entity.RecurringExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.RecurringExpenseRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserRepository userRepository;
    private final PersonalExpenseService personalExpenseService;
    private final RecurringDateCalculator recurringDateCalculator;

    public RecurringExpenseService(
            RecurringExpenseRepository recurringExpenseRepository,
            UserRepository userRepository,
            PersonalExpenseService personalExpenseService,
            RecurringDateCalculator recurringDateCalculator
    ) {
        this.recurringExpenseRepository =
                recurringExpenseRepository;

        this.userRepository =
                userRepository;

        this.personalExpenseService =
                personalExpenseService;

        this.recurringDateCalculator =
                recurringDateCalculator;
    }

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(
            String email,
            RecurringExpenseRequest request
    ) {
        User user = getUserByEmail(email);

        LocalDateTime now =
                LocalDateTime.now();

        Boolean active =
                request.getActive() == null
                        ? Boolean.TRUE
                        : request.getActive();

        RecurringExpense recurringExpense =
                new RecurringExpense(
                        user,
                        request.getTitle().trim(),
                        request.getAmount(),
                        request.getCategory(),
                        request.getFrequency(),
                        request.getNextDueDate(),
                        active,
                        now,
                        now
                );

        RecurringExpense saved =
                recurringExpenseRepository.save(
                        recurringExpense
                );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RecurringExpenseResponse getRecurringExpenseById(
            String email,
            Long recurringExpenseId
    ) {
        User user =
                getUserByEmail(email);

        RecurringExpense recurringExpense =
                getOwnedRecurringExpense(
                        recurringExpenseId,
                        user.getId()
                );

        return toResponse(
                recurringExpense
        );
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getRecurringExpenses(
            String email
    ) {
        User user =
                getUserByEmail(email);

        return recurringExpenseRepository
                .findByUserIdOrderByNextDueDateAscIdAsc(
                        user.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getUpcomingBills(
            String email,
            Integer days
    ) {
        User user =
                getUserByEmail(email);

        int requestedDays =
                days == null
                        ? 30
                        : days;

        if (requestedDays < 1
                || requestedDays > 365) {

            throw new InvalidRequestException(
                    "Days must be between 1 and 365"
            );
        }

        LocalDate startDate =
                LocalDate.now();

        LocalDate endDate =
                startDate.plusDays(
                        requestedDays
                );

        return recurringExpenseRepository
                .findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAscIdAsc(
                        user.getId(),
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(
            String email,
            Long recurringExpenseId,
            RecurringExpenseRequest request
    ) {
        User user =
                getUserByEmail(email);

        RecurringExpense recurringExpense =
                getOwnedRecurringExpense(
                        recurringExpenseId,
                        user.getId()
                );

        recurringExpense.setTitle(
                request.getTitle().trim()
        );

        recurringExpense.setAmount(
                request.getAmount()
        );

        recurringExpense.setCategory(
                request.getCategory()
        );

        recurringExpense.setFrequency(
                request.getFrequency()
        );

        recurringExpense.setNextDueDate(
                request.getNextDueDate()
        );

        if (request.getActive() != null) {

            recurringExpense.setActive(
                    request.getActive()
            );
        }

        recurringExpense.setUpdatedAt(
                LocalDateTime.now()
        );

        RecurringExpense updated =
                recurringExpenseRepository.save(
                        recurringExpense
                );

        return toResponse(updated);
    }

    @Transactional
    public void deleteRecurringExpense(
            String email,
            Long recurringExpenseId
    ) {
        User user =
                getUserByEmail(email);

        RecurringExpense recurringExpense =
                getOwnedRecurringExpense(
                        recurringExpenseId,
                        user.getId()
                );

        recurringExpenseRepository.delete(
                recurringExpense
        );
    }

    @Transactional
    public RecurringExpenseResponse recordPayment(
            String email,
            Long recurringExpenseId,
            RecurringPaymentRequest request
    ) {
        User user =
                getUserByEmail(email);

        RecurringExpense recurringExpense =
                getOwnedRecurringExpense(
                        recurringExpenseId,
                        user.getId()
                );

        if (!Boolean.TRUE.equals(
                recurringExpense.getActive()
        )) {

            throw new InvalidRequestException(
                    "Inactive recurring expense cannot be recorded"
            );
        }

        PersonalExpenseRequest expenseRequest =
                new PersonalExpenseRequest();

        expenseRequest.setAmount(
                recurringExpense.getAmount()
        );

        expenseRequest.setCategory(
                recurringExpense.getCategory()
        );

        expenseRequest.setExpenseDate(
                request.getPaymentDate()
        );

        expenseRequest.setDescription(
                recurringExpense.getTitle()
        );

        personalExpenseService.createExpense(
                email,
                expenseRequest
        );

        LocalDate nextDueDate =
                recurringDateCalculator
                        .calculateNextDueDateAfterPayment(
                                recurringExpense
                                        .getNextDueDate(),

                                recurringExpense
                                        .getFrequency(),

                                request.getPaymentDate()
                        );

        recurringExpense.setNextDueDate(
                nextDueDate
        );

        recurringExpense.setUpdatedAt(
                LocalDateTime.now()
        );

        RecurringExpense updated =
                recurringExpenseRepository.save(
                        recurringExpense
                );

        return toResponse(updated);
    }

    private RecurringExpense getOwnedRecurringExpense(
            Long recurringExpenseId,
            Long userId
    ) {
        return recurringExpenseRepository
                .findByIdAndUserId(
                        recurringExpenseId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Recurring expense not found"
                        )
                );
    }

    private User getUserByEmail(
            String email
    ) {
        String normalizedEmail =
                email.trim()
                        .toLowerCase();

        return userRepository
                .findByEmail(
                        normalizedEmail
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private RecurringExpenseResponse toResponse(
            RecurringExpense recurringExpense
    ) {
        return new RecurringExpenseResponse(
                recurringExpense.getId(),
                recurringExpense.getTitle(),
                recurringExpense.getAmount(),
                recurringExpense.getCategory(),
                recurringExpense.getFrequency(),
                recurringExpense.getNextDueDate(),
                recurringExpense.getActive(),
                recurringExpense.getCreatedAt(),
                recurringExpense.getUpdatedAt()
        );
    }
}