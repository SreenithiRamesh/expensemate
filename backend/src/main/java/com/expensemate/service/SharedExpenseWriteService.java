package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.ExpenseSplitRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SharedExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.strategy.SplitResult;
import com.expensemate.strategy.SplitStrategy;
import com.expensemate.strategy.SplitStrategyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SharedExpenseWriteService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SplitStrategyResolver splitStrategyResolver;
    private final ActivityService activityService;

    public SharedExpenseWriteService(
            SharedExpenseRepository sharedExpenseRepository,
            ExpenseSplitRepository expenseSplitRepository,
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            SplitStrategyResolver splitStrategyResolver,
            ActivityService activityService
    ) {
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.splitStrategyResolver = splitStrategyResolver;
        this.activityService = activityService;
    }

    @Transactional
    public SharedExpense create(
            Long groupId,
            Long currentUserId,
            String idempotencyKey,
            String requestFingerprint,
            SharedExpenseCreateRequest request
    ) {

        User currentUser =
                userRepository.findById(currentUserId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        ExpenseGroup group =
                getGroupForMember(
                        groupId,
                        currentUserId
                );

        validateDuplicateSplitUsers(
                request.getSplits()
        );

        User payer =
                getGroupMemberUser(
                        groupId,
                        request.getPaidByUserId(),
                        "Payer must be a member of this group"
                );

        for (SplitInputRequest split : request.getSplits()) {

            getGroupMemberUser(
                    groupId,
                    split.getUserId(),
                    "All split participants must be members of this group"
            );
        }

        SplitStrategy strategy =
                splitStrategyResolver.resolve(
                        request.getSplitType()
                );

        List<SplitResult> calculatedSplits =
                strategy.calculate(
                        request.getAmount(),
                        request.getSplits()
                );

        validateCalculatedTotal(
                request.getAmount(),
                calculatedSplits
        );

        SharedExpense expense =
                new SharedExpense(
                        group,
                        payer,
                        currentUser,
                        request.getTitle().trim(),
                        request.getAmount(),
                        request.getSplitType(),
                        request.getExpenseDate(),
                        idempotencyKey,
                        requestFingerprint
                );

        /*
         * IMPORTANT FOR M23 CONCURRENCY:
         *
         * saveAndFlush forces the INSERT here.
         *
         * If another concurrent transaction already won
         * the same (created_by, idempotency_key), the
         * database UNIQUE constraint fails inside THIS
         * transaction.
         *
         * This entire transaction then rolls back, including
         * any work performed here.
         */
        SharedExpense savedExpense =
                sharedExpenseRepository.saveAndFlush(
                        expense
                );

        List<ExpenseSplit> expenseSplits =
                calculatedSplits
                        .stream()
                        .map(result -> {

                            User splitUser =
                                    userRepository.findById(
                                                    result.userId()
                                            )
                                            .orElseThrow(
                                                    () ->
                                                            new ResourceNotFoundException(
                                                                    "User not found"
                                                            )
                                            );

                            return new ExpenseSplit(
                                    savedExpense,
                                    splitUser,
                                    result.shareAmount(),
                                    result.percentage()
                            );
                        })
                        .toList();

        expenseSplitRepository.saveAll(
                expenseSplits
        );

        activityService.record(
                group,
                currentUser,
                ActivityType.SHARED_EXPENSE_CREATED,
                currentUser.getName()
                        + " added expense \""
                        + savedExpense.getTitle()
                        + "\" for ₹"
                        + savedExpense.getAmount().toPlainString(),
                savedExpense.getId()
        );

        return savedExpense;
    }

    private ExpenseGroup getGroupForMember(
            Long groupId,
            Long userId
    ) {

        ExpenseGroup group =
                expenseGroupRepository.findById(groupId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
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

    private User getGroupMemberUser(
            Long groupId,
            Long userId,
            String errorMessage
    ) {

        GroupMember membership =
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                groupId,
                                userId
                        )
                        .orElseThrow(
                                () ->
                                        new InvalidRequestException(
                                                errorMessage
                                        )
                        );

        return membership.getUser();
    }

    private void validateDuplicateSplitUsers(
            List<SplitInputRequest> splits
    ) {

        Set<Long> uniqueUserIds =
                new HashSet<>();

        for (SplitInputRequest split : splits) {

            if (!uniqueUserIds.add(
                    split.getUserId()
            )) {
                throw new InvalidRequestException(
                        "Duplicate split members are not allowed"
                );
            }
        }
    }

    private void validateCalculatedTotal(
            BigDecimal expenseAmount,
            List<SplitResult> calculatedSplits
    ) {

        BigDecimal splitTotal =
                calculatedSplits.stream()
                        .map(SplitResult::shareAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (splitTotal.compareTo(expenseAmount) != 0) {
            throw new InvalidRequestException(
                    "Split amounts must equal the expense amount"
            );
        }
    }
}