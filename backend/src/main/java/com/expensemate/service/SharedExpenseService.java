package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.dto.SharedExpenseSplitResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.ExpenseSplitRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SharedExpenseRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SharedExpenseService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SharedExpenseFingerprintService fingerprintService;
    private final SharedExpenseWriteService writeService;

    public SharedExpenseService(
            SharedExpenseRepository sharedExpenseRepository,
            ExpenseSplitRepository expenseSplitRepository,
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            SharedExpenseFingerprintService fingerprintService,
            SharedExpenseWriteService writeService
    ) {
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.fingerprintService = fingerprintService;
        this.writeService = writeService;
    }

    /*
     * Intentionally NOT @Transactional.
     *
     * The actual creation runs inside SharedExpenseWriteService's
     * separate transaction. If a concurrent request loses the
     * database uniqueness race, that write transaction rolls back
     * completely before this method attempts replay recovery.
     */
    public SharedExpenseResponse createSharedExpense(
            Long groupId,
            String currentUserEmail,
            String idempotencyKey,
            SharedExpenseCreateRequest request
    ) {

        validateIdempotencyKey(idempotencyKey);

        User currentUser =
                getUserByEmail(currentUserEmail);

        String requestFingerprint =
                fingerprintService.fingerprint(
                        groupId,
                        request
                );

        SharedExpense existingExpense =
                findExistingExpense(
                        currentUser.getId(),
                        idempotencyKey
                );

        if (existingExpense != null) {
            return replayExisting(
                    existingExpense,
                    requestFingerprint
            );
        }

        try {

            SharedExpense savedExpense =
                    writeService.create(
                            groupId,
                            currentUser.getId(),
                            idempotencyKey,
                            requestFingerprint,
                            request
                    );

            List<ExpenseSplit> savedSplits =
                    expenseSplitRepository.findByExpenseId(
                            savedExpense.getId()
                    );

            return toResponse(
                    savedExpense,
                    savedSplits
            );

        } catch (DataIntegrityViolationException exception) {

            /*
             * The writer owns a separate transaction.
             *
             * Therefore, by the time control reaches here, the losing
             * transaction has already rolled back. We can safely query
             * for the concurrent winner.
             */
            SharedExpense concurrentWinner =
                    findExistingExpense(
                            currentUser.getId(),
                            idempotencyKey
                    );

            /*
             * Do not hide unrelated integrity failures.
             *
             * If no row exists for this creator/key, the exception was
             * not proven to be our idempotency uniqueness race.
             */
            if (concurrentWinner == null) {
                throw exception;
            }

            return replayExisting(
                    concurrentWinner,
                    requestFingerprint
            );
        }
    }

    @Transactional(readOnly = true)
    public List<SharedExpenseResponse> getGroupExpenses(
            Long groupId,
            String currentUserEmail
    ) {

        User currentUser =
                getUserByEmail(
                        currentUserEmail
                );

        getGroupForMember(
                groupId,
                currentUser.getId()
        );

        List<SharedExpense> expenses =
                sharedExpenseRepository
                        .findByGroupIdOrderByExpenseDateDescCreatedAtDesc(
                                groupId
                        );

        return expenses
                .stream()
                .map(expense -> {

                    List<ExpenseSplit> splits =
                            expenseSplitRepository
                                    .findByExpenseId(
                                            expense.getId()
                                    );

                    return toResponse(
                            expense,
                            splits
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SharedExpenseResponse getSharedExpense(
            Long groupId,
            Long expenseId,
            String currentUserEmail
    ) {

        User currentUser =
                getUserByEmail(
                        currentUserEmail
                );

        getGroupForMember(
                groupId,
                currentUser.getId()
        );

        SharedExpense expense =
                sharedExpenseRepository
                        .findById(
                                expenseId
                        )
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Shared expense not found"
                                        )
                        );

        if (!expense
                .getGroup()
                .getId()
                .equals(groupId)) {

            throw new ResourceNotFoundException(
                    "Shared expense not found"
            );
        }

        List<ExpenseSplit> splits =
                expenseSplitRepository
                        .findByExpenseId(
                                expenseId
                        );

        return toResponse(
                expense,
                splits
        );
    }

    private SharedExpense findExistingExpense(
            Long currentUserId,
            String idempotencyKey
    ) {

        return sharedExpenseRepository
                .findByCreatedBy_IdAndIdempotencyKey(
                        currentUserId,
                        idempotencyKey
                )
                .orElse(null);
    }

    private SharedExpenseResponse replayExisting(
            SharedExpense existingExpense,
            String requestFingerprint
    ) {

        if (existingExpense.getRequestFingerprint() == null
                || !existingExpense
                .getRequestFingerprint()
                .equals(requestFingerprint)) {

            throw new InvalidRequestException(
                    "Idempotency key has already been used for a different request"
            );
        }

        List<ExpenseSplit> existingSplits =
                expenseSplitRepository
                        .findByExpenseId(
                                existingExpense.getId()
                        );

        return toResponse(
                existingExpense,
                existingSplits
        );
    }

    private User getUserByEmail(
            String email
    ) {

        if (email == null
                || email.isBlank()) {

            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                );
    }

    private ExpenseGroup getGroupForMember(
            Long groupId,
            Long userId
    ) {

        ExpenseGroup group =
                expenseGroupRepository
                        .findById(groupId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Group not found"
                                        )
                        );

        boolean member =
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                userId
                        );

        if (!member) {
            throw new ResourceNotFoundException(
                    "Group not found"
            );
        }

        return group;
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            throw new InvalidRequestException(
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new InvalidRequestException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }

    private SharedExpenseResponse toResponse(
            SharedExpense expense,
            List<ExpenseSplit> splits
    ) {

        List<SharedExpenseSplitResponse> splitResponses =
                splits
                        .stream()
                        .map(
                                split ->
                                        new SharedExpenseSplitResponse(
                                                split.getUser().getId(),
                                                split.getUser().getName(),
                                                split.getUser().getEmail(),
                                                split.getShareAmount(),
                                                split.getPercentage()
                                        )
                        )
                        .toList();

        return new SharedExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getSplitType(),
                expense.getExpenseDate(),
                splitResponses,
                expense.getCreatedAt()
        );
    }
}