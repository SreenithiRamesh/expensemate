package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.enums.SplitType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedExpenseServiceTest {

    @Mock
    private SharedExpenseRepository sharedExpenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SplitStrategyResolver splitStrategyResolver;

    @Mock
    private SplitStrategy splitStrategy;

    @Mock
    private ActivityService activityService;

    private SharedExpenseService sharedExpenseService;

    @BeforeEach
    void setUp() {

        sharedExpenseService =
                new SharedExpenseService(
                        sharedExpenseRepository,
                        expenseSplitRepository,
                        expenseGroupRepository,
                        groupMemberRepository,
                        userRepository,
                        splitStrategyResolver,
                        activityService
                );
    }

    @Test
    void shouldCreateEqualSharedExpenseSuccessfully() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User secondUser =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        GroupMember currentUserMembership =
                membership(
                        currentUser
                );

        GroupMember secondUserMembership =
                membership(
                        secondUser
                );

        SplitInputRequest splitOne =
                splitRequest(
                        1L
                );

        SplitInputRequest splitTwo =
                splitRequest(
                        2L
                );

        /*
         * Store the list separately.
         *
         * This avoids calling request.getSplits()
         * inside Mockito eq(...), which caused the
         * InvalidUseOfMatchersException.
         */
        List<SplitInputRequest> splits =
                List.of(
                        splitOne,
                        splitTwo
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        /*
         * User 1 is both the payer
         * and one of the split participants.
         */
        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        currentUserMembership
                )
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        secondUserMembership
                )
        );

        when(
                splitStrategyResolver
                        .resolve(
                                SplitType.EQUAL
                        )
        ).thenReturn(
                splitStrategy
        );

        when(
                splitStrategy.calculate(
                        eq(
                                new BigDecimal(
                                        "100.00"
                                )
                        ),
                        eq(
                                splits
                        )
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal(
                                        "50.00"
                                ),
                                null
                        ),
                        new SplitResult(
                                2L,
                                new BigDecimal(
                                        "50.00"
                                ),
                                null
                        )
                )
        );

        when(
                sharedExpenseRepository
                        .save(
                                any(
                                        SharedExpense.class
                                )
                        )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(
                                0
                        )
        );

        when(
                userRepository
                        .findById(
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                userRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        secondUser
                )
        );

        when(
                expenseSplitRepository
                        .saveAll(
                                anyList()
                        )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(
                                0
                        )
        );

        SharedExpenseResponse response =
                sharedExpenseService
                        .createSharedExpense(
                                2L,
                                "sree@example.com",
                                request
                        );

        assertNotNull(
                response
        );

        assertEquals(
                "Dinner",
                response.getTitle()
        );

        assertMoney(
                "100.00",
                response.getAmount()
        );

        assertEquals(
                SplitType.EQUAL,
                response.getSplitType()
        );

        assertEquals(
                2,
                response.getSplits().size()
        );

        /*
         * Strategy resolver must select
         * the EQUAL strategy.
         */
        verify(
                splitStrategyResolver
        ).resolve(
                SplitType.EQUAL
        );

        /*
         * Corrected Mockito verification:
         * both arguments are matchers and
         * no mock getter is called inside eq(...).
         */
        verify(
                splitStrategy
        ).calculate(
                eq(
                        new BigDecimal(
                                "100.00"
                        )
                ),
                eq(
                        splits
                )
        );

        verify(
                sharedExpenseRepository
        ).save(
                any(
                        SharedExpense.class
                )
        );

        /*
         * Capture the saved ExpenseSplit list
         * and verify that the strategy output
         * was persisted correctly.
         */
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExpenseSplit>> splitCaptor =
                ArgumentCaptor.forClass(
                        List.class
                );

        verify(
                expenseSplitRepository
        ).saveAll(
                splitCaptor.capture()
        );

        List<ExpenseSplit> savedSplits =
                splitCaptor.getValue();

        assertEquals(
                2,
                savedSplits.size()
        );

        assertMoney(
                "50.00",
                savedSplits
                        .get(0)
                        .getShareAmount()
        );

        assertMoney(
                "50.00",
                savedSplits
                        .get(1)
                        .getShareAmount()
        );

        /*
         * M13:
         * successful shared expense creation
         * must create an activity record.
         *
         * savedExpense.getId() is null here because
         * repository.save() is mocked and no real DB
         * generates the ID.
         */
        verify(
                activityService
        ).record(
                eq(
                        group
                ),
                eq(
                        currentUser
                ),
                eq(
                        ActivityType.SHARED_EXPENSE_CREATED
                ),
                contains(
                        "Dinner"
                ),
                isNull()
        );
    }

    @Test
    void shouldRejectDuplicateSplitMembers() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        SplitInputRequest splitOne =
                splitRequest(
                        1L
                );

        SplitInputRequest duplicateSplit =
                splitRequest(
                        1L
                );

        List<SplitInputRequest> splits =
                List.of(
                        splitOne,
                        duplicateSplit
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                2L,
                                                "sree@example.com",
                                                request
                                        )
                );

        assertEquals(
                "Duplicate split members are not allowed",
                exception.getMessage()
        );

        verify(
                splitStrategyResolver,
                never()
        ).resolve(
                any()
        );

        verify(
                sharedExpenseRepository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectRequesterWhoIsNotGroupMember() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        List.of(
                                splitRequest(
                                        1L
                                )
                        )
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                false
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                2L,
                                                "sree@example.com",
                                                request
                                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );

        verify(
                sharedExpenseRepository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectPayerWhoIsNotGroupMember() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        99L,
                        SplitType.EQUAL,
                        List.of(
                                splitRequest(
                                        1L
                                )
                        )
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                99L
                        )
        ).thenReturn(
                Optional.empty()
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                2L,
                                                "sree@example.com",
                                                request
                                        )
                );

        assertEquals(
                "Payer must be a member of this group",
                exception.getMessage()
        );

        verify(
                sharedExpenseRepository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectSplitParticipantWhoIsNotGroupMember() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        GroupMember currentUserMembership =
                membership(
                        currentUser
                );

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(
                                1L
                        ),
                        splitRequest(
                                99L
                        )
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        currentUserMembership
                )
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                99L
                        )
        ).thenReturn(
                Optional.empty()
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                2L,
                                                "sree@example.com",
                                                request
                                        )
                );

        assertEquals(
                "All split participants must be members of this group",
                exception.getMessage()
        );

        verify(
                sharedExpenseRepository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectCalculatedSplitTotalMismatch() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User secondUser =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        GroupMember currentUserMembership =
                membership(
                        currentUser
                );

        GroupMember secondUserMembership =
                membership(
                        secondUser
                );

        SplitInputRequest splitOne =
                splitRequest(
                        1L
                );

        SplitInputRequest splitTwo =
                splitRequest(
                        2L
                );

        List<SplitInputRequest> splits =
                List.of(
                        splitOne,
                        splitTwo
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository
                        .findById(
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        currentUserMembership
                )
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        secondUserMembership
                )
        );

        when(
                splitStrategyResolver
                        .resolve(
                                SplitType.EQUAL
                        )
        ).thenReturn(
                splitStrategy
        );

        /*
         * Wrong total deliberately:
         * 40 + 40 = 80, but expense = 100.
         */
        when(
                splitStrategy.calculate(
                        eq(
                                new BigDecimal(
                                        "100.00"
                                )
                        ),
                        eq(
                                splits
                        )
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal(
                                        "40.00"
                                ),
                                null
                        ),
                        new SplitResult(
                                2L,
                                new BigDecimal(
                                        "40.00"
                                ),
                                null
                        )
                )
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                2L,
                                                "sree@example.com",
                                                request
                                        )
                );

        assertEquals(
                "Split amounts must equal the expense amount",
                exception.getMessage()
        );

        verify(
                sharedExpenseRepository,
                never()
        ).save(
                any()
        );

        verify(
                expenseSplitRepository,
                never()
        ).saveAll(
                anyList()
        );

        verifyNoInteractions(
                activityService
        );
    }

    private SharedExpenseCreateRequest request(
            String title,
            String amount,
            Long paidByUserId,
            SplitType splitType,
            List<SplitInputRequest> splits
    ) {

        SharedExpenseCreateRequest request =
                mock(
                        SharedExpenseCreateRequest.class
                );

        lenient()
                .when(
                        request.getTitle()
                )
                .thenReturn(
                        title
                );

        lenient()
                .when(
                        request.getAmount()
                )
                .thenReturn(
                        new BigDecimal(
                                amount
                        )
                );

        lenient()
                .when(
                        request.getPaidByUserId()
                )
                .thenReturn(
                        paidByUserId
                );

        lenient()
                .when(
                        request.getSplitType()
                )
                .thenReturn(
                        splitType
                );

        lenient()
                .when(
                        request.getExpenseDate()
                )
                .thenReturn(
                        LocalDate.of(
                                2026,
                                9,
                                15
                        )
                );

        lenient()
                .when(
                        request.getSplits()
                )
                .thenReturn(
                        splits
                );

        return request;
    }

    private SplitInputRequest splitRequest(
            Long userId
    ) {

        SplitInputRequest request =
                mock(
                        SplitInputRequest.class
                );

        lenient()
                .when(
                        request.getUserId()
                )
                .thenReturn(
                        userId
                );

        return request;
    }

    private User user(
            Long id,
            String name,
            String email
    ) {

        User user =
                mock(
                        User.class
                );

        lenient()
                .when(
                        user.getId()
                )
                .thenReturn(
                        id
                );

        lenient()
                .when(
                        user.getName()
                )
                .thenReturn(
                        name
                );

        lenient()
                .when(
                        user.getEmail()
                )
                .thenReturn(
                        email
                );

        return user;
    }

    private ExpenseGroup group(
            Long id
    ) {

        ExpenseGroup group =
                mock(
                        ExpenseGroup.class
                );

        lenient()
                .when(
                        group.getId()
                )
                .thenReturn(
                        id
                );

        return group;
    }

    private GroupMember membership(
            User user
    ) {

        GroupMember membership =
                mock(
                        GroupMember.class
                );

        lenient()
                .when(
                        membership.getUser()
                )
                .thenReturn(
                        user
                );

        return membership;
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

        assertNotNull(
                actual
        );

        assertEquals(
                0,
                new BigDecimal(
                        expected
                ).compareTo(
                        actual
                )
        );
    }
}