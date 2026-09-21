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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        /*
         * Collect the payer and every split participant before
         * querying the database.
         *
         * LinkedHashSet removes duplicates while preserving
         * predictable insertion order.
         */
        Set<Long> requiredUserIds =
                collectRequiredUserIds(request);

        /*
         * M27 performance optimization:
         *
         * Load all required memberships and their users using
         * one JOIN FETCH query instead of querying separately
         * for every participant.
         */
        Map<Long, User> memberUsersById =
                loadMemberUsers(
                        groupId,
                        requiredUserIds
                );

        User payer =
                requireMemberUser(
                        memberUsersById,
                        request.getPaidByUserId(),
                        "Payer must be a member of this group"
                );

        validateSplitParticipants(
                request.getSplits(),
                memberUsersById
        );

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
         * saveAndFlush deliberately forces the shared-expense
         * INSERT here.
         *
         * The database unique constraint on
         * (created_by, idempotency_key) remains the final
         * concurrency safeguard for M23.
         */
        SharedExpense savedExpense =
                sharedExpenseRepository.saveAndFlush(
                        expense
                );

        List<ExpenseSplit> expenseSplits =
                calculatedSplits.stream()
                        .map(result -> {

                            User splitUser =
                                    requireLoadedUser(
                                            memberUsersById,
                                            result.userId()
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

    private Set<Long> collectRequiredUserIds(
            SharedExpenseCreateRequest request
    ) {

        Set<Long> requiredUserIds =
                new LinkedHashSet<>();

        requiredUserIds.add(
                request.getPaidByUserId()
        );

        for (SplitInputRequest split
                : request.getSplits()) {

            requiredUserIds.add(
                    split.getUserId()
            );
        }

        return requiredUserIds;
    }

    private Map<Long, User> loadMemberUsers(
            Long groupId,
            Set<Long> requiredUserIds
    ) {

        List<GroupMember> memberships =
                groupMemberRepository.findMembersWithUsers(
                        groupId,
                        requiredUserIds
                );

        Map<Long, User> memberUsersById =
                new HashMap<>();

        for (GroupMember membership : memberships) {

            User user =
                    membership.getUser();

            memberUsersById.put(
                    user.getId(),
                    user
            );
        }

        return memberUsersById;
    }

    private User requireMemberUser(
            Map<Long, User> memberUsersById,
            Long userId,
            String errorMessage
    ) {

        User user =
                memberUsersById.get(userId);

        if (user == null) {
            throw new InvalidRequestException(
                    errorMessage
            );
        }

        return user;
    }

    private void validateSplitParticipants(
            List<SplitInputRequest> splits,
            Map<Long, User> memberUsersById
    ) {

        for (SplitInputRequest split : splits) {

            requireMemberUser(
                    memberUsersById,
                    split.getUserId(),
                    "All split participants must be members of this group"
            );
        }
    }

    private User requireLoadedUser(
            Map<Long, User> memberUsersById,
            Long userId
    ) {

        User user =
                memberUsersById.get(userId);

        /*
         * Strategies should return only the user IDs supplied
         * in the validated request. This guard prevents an
         * invalid strategy result from creating an expense
         * split for an unknown user.
         */
        if (user == null) {
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        return user;
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