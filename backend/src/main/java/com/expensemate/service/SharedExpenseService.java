package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.dto.SharedExpenseSplitResponse;
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
public class SharedExpenseService {

    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SplitStrategyResolver splitStrategyResolver;

    // M13 — centralized activity/audit history
    private final ActivityService activityService;

    public SharedExpenseService(
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
    public SharedExpenseResponse createSharedExpense(
            Long groupId,
            String currentUserEmail,
            SharedExpenseCreateRequest request
    ) {

        User currentUser =
                getUserByEmail(currentUserEmail);

        ExpenseGroup group =
                getGroupForMember(
                        groupId,
                        currentUser.getId()
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

        /*
         * Every participant included in the split
         * must already belong to the group.
         */
        for (SplitInputRequest split : request.getSplits()) {

            getGroupMemberUser(
                    groupId,
                    split.getUserId(),
                    "All split participants must be members of this group"
            );
        }

        /*
         * Strategy Pattern:
         *
         * The service does not contain EQUAL,
         * PERCENTAGE or EXACT calculation logic.
         *
         * SplitStrategyResolver chooses the
         * correct implementation.
         */
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
                        request.getTitle().trim(),
                        request.getAmount(),
                        request.getSplitType(),
                        request.getExpenseDate()
                );

        SharedExpense savedExpense =
                sharedExpenseRepository.save(
                        expense
                );

        /*
         * Convert the calculated Strategy results
         * into persistent ExpenseSplit entities.
         */
        List<ExpenseSplit> expenseSplits =
                calculatedSplits
                        .stream()
                        .map(result -> {

                            User splitUser =
                                    userRepository
                                            .findById(
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

        /*
         * M13 — Activity / Audit History
         *
         * Record the activity only AFTER both the
         * SharedExpense and ExpenseSplit rows have
         * successfully been persisted.
         *
         * Because createSharedExpense() is transactional,
         * expense + splits + activity belong to the
         * same transaction.
         */
        activityService.record(
                group,
                currentUser,
                ActivityType.SHARED_EXPENSE_CREATED,
                currentUser.getName()
                        + " added expense \""
                        + savedExpense.getTitle()
                        + "\" for ₹"
                        + savedExpense
                        .getAmount()
                        .toPlainString(),
                savedExpense.getId()
        );

        return toResponse(
                savedExpense,
                expenseSplits
        );
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

        /*
         * Prevent a user from using an expense ID
         * belonging to another group.
         */
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
                .findByEmail(
                        email
                )
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
                        .findById(
                                groupId
                        )
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

        /*
         * We intentionally return "Group not found"
         * to a non-member instead of revealing that
         * the group exists.
         */
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
                calculatedSplits
                        .stream()
                        .map(
                                SplitResult::shareAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (splitTotal.compareTo(
                expenseAmount
        ) != 0) {

            throw new InvalidRequestException(
                    "Split amounts must equal the expense amount"
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