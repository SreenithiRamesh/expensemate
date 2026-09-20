package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedExpenseWriteServiceTest {

    private static final Long GROUP_ID = 2L;
    private static final Long CURRENT_USER_ID = 1L;
    private static final String IDEMPOTENCY_KEY = "test-key";
    private static final String FINGERPRINT = "test-fingerprint";

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

    private SharedExpenseWriteService writeService;

    @BeforeEach
    void setUp() {

        writeService =
                new SharedExpenseWriteService(
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

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        User secondUser =
                user(
                        2L,
                        "Test User",
                        "test@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        GroupMember creatorMembership =
                membership(creator);

        GroupMember secondMembership =
                membership(secondUser);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L),
                        splitRequest(2L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "  Dinner  ",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                1L
                        )
        ).thenReturn(
                Optional.of(creatorMembership)
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                2L
                        )
        ).thenReturn(
                Optional.of(secondMembership)
        );

        when(
                splitStrategyResolver.resolve(
                        SplitType.EQUAL
                )
        ).thenReturn(splitStrategy);

        when(
                splitStrategy.calculate(
                        eq(new BigDecimal("100.00")),
                        eq(splits)
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal("50.00"),
                                null
                        ),
                        new SplitResult(
                                2L,
                                new BigDecimal("50.00"),
                                null
                        )
                )
        );

        when(
                sharedExpenseRepository.saveAndFlush(
                        any(SharedExpense.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(creator)
        );

        when(
                userRepository.findById(2L)
        ).thenReturn(
                Optional.of(secondUser)
        );

        when(
                expenseSplitRepository.saveAll(
                        anyList()
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        SharedExpense result =
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                );

        assertNotNull(result);

        assertEquals(
                "Dinner",
                result.getTitle()
        );

        assertMoney(
                "100.00",
                result.getAmount()
        );

        assertSame(
                creator,
                result.getCreatedBy()
        );

        assertSame(
                creator,
                result.getPaidBy()
        );

        assertEquals(
                IDEMPOTENCY_KEY,
                result.getIdempotencyKey()
        );

        assertEquals(
                FINGERPRINT,
                result.getRequestFingerprint()
        );

        ArgumentCaptor<SharedExpense> expenseCaptor =
                ArgumentCaptor.forClass(
                        SharedExpense.class
                );

        verify(
                sharedExpenseRepository
        ).saveAndFlush(
                expenseCaptor.capture()
        );

        SharedExpense persistedExpense =
                expenseCaptor.getValue();

        assertSame(
                creator,
                persistedExpense.getCreatedBy()
        );

        assertEquals(
                "Dinner",
                persistedExpense.getTitle()
        );

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

        verify(
                activityService
        ).record(
                eq(group),
                eq(creator),
                eq(ActivityType.SHARED_EXPENSE_CREATED),
                contains("Dinner"),
                isNull()
        );
    }

    @Test
    void shouldKeepCreatorSeparateFromPayer() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        User payer =
                user(
                        2L,
                        "Payer",
                        "payer@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        GroupMember creatorMembership =
                membership(creator);

        GroupMember payerMembership =
                membership(payer);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L),
                        splitRequest(2L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "Lunch",
                        "100.00",
                        2L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                2L
                        )
        ).thenReturn(
                Optional.of(payerMembership)
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                1L
                        )
        ).thenReturn(
                Optional.of(creatorMembership)
        );

        when(
                splitStrategyResolver.resolve(
                        SplitType.EQUAL
                )
        ).thenReturn(splitStrategy);

        when(
                splitStrategy.calculate(
                        eq(new BigDecimal("100.00")),
                        eq(splits)
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal("50.00"),
                                null
                        ),
                        new SplitResult(
                                2L,
                                new BigDecimal("50.00"),
                                null
                        )
                )
        );

        when(
                sharedExpenseRepository.saveAndFlush(
                        any(SharedExpense.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(creator)
        );

        when(
                userRepository.findById(2L)
        ).thenReturn(
                Optional.of(payer)
        );

        SharedExpense result =
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                );

        assertNotNull(result);

        assertSame(
                creator,
                result.getCreatedBy()
        );

        assertSame(
                payer,
                result.getPaidBy()
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(creator),
                eq(ActivityType.SHARED_EXPENSE_CREATED),
                contains("Lunch"),
                isNull()
        );
    }

    @Test
    void shouldRejectUnknownCreator() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        when(
                userRepository.findById(
                        CURRENT_USER_ID
                )
        ).thenReturn(
                Optional.empty()
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
                                        request
                                )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                expenseGroupRepository,
                groupMemberRepository,
                splitStrategyResolver,
                sharedExpenseRepository,
                expenseSplitRepository,
                activityService
        );
    }

    @Test
    void shouldRejectRequesterWhoIsNotGroupMember() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        when(
                userRepository.findById(
                        CURRENT_USER_ID
                )
        ).thenReturn(
                Optional.of(creator)
        );

        when(
                expenseGroupRepository.findById(
                        GROUP_ID
                )
        ).thenReturn(
                Optional.of(group)
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                GROUP_ID,
                                CURRENT_USER_ID
                        )
        ).thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
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
        ).saveAndFlush(any());

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectDuplicateSplitMembers() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L),
                        splitRequest(1L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
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
        ).resolve(any());

        verify(
                sharedExpenseRepository,
                never()
        ).saveAndFlush(any());

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectPayerWhoIsNotGroupMember() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        99L,
                        SplitType.EQUAL,
                        List.of(
                                splitRequest(1L)
                        )
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                99L
                        )
        ).thenReturn(
                Optional.empty()
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
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
        ).saveAndFlush(any());

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectSplitParticipantWhoIsNotGroupMember() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        User payer =
                user(
                        2L,
                        "Payer",
                        "payer@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        GroupMember payerMembership =
                membership(payer);

        GroupMember creatorMembership =
                membership(creator);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L),
                        splitRequest(99L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        2L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                2L
                        )
        ).thenReturn(
                Optional.of(payerMembership)
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                1L
                        )
        ).thenReturn(
                Optional.of(creatorMembership)
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                99L
                        )
        ).thenReturn(
                Optional.empty()
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
                                        request
                                )
                );

        assertEquals(
                "All split participants must be members of this group",
                exception.getMessage()
        );

        verify(
                splitStrategyResolver,
                never()
        ).resolve(any());

        verify(
                sharedExpenseRepository,
                never()
        ).saveAndFlush(any());

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldRejectCalculatedSplitTotalMismatch() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        User secondUser =
                user(
                        2L,
                        "Test User",
                        "test@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        GroupMember creatorMembership =
                membership(creator);

        GroupMember secondMembership =
                membership(secondUser);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L),
                        splitRequest(2L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                1L
                        )
        ).thenReturn(
                Optional.of(creatorMembership)
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                2L
                        )
        ).thenReturn(
                Optional.of(secondMembership)
        );

        when(
                splitStrategyResolver.resolve(
                        SplitType.EQUAL
                )
        ).thenReturn(splitStrategy);

        when(
                splitStrategy.calculate(
                        eq(new BigDecimal("100.00")),
                        eq(splits)
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal("40.00"),
                                null
                        ),
                        new SplitResult(
                                2L,
                                new BigDecimal("40.00"),
                                null
                        )
                )
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
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
        ).saveAndFlush(any());

        verify(
                expenseSplitRepository,
                never()
        ).saveAll(anyList());

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldPropagateSaveAndFlushIntegrityViolationBeforeDependentWrites() {

        User creator =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(GROUP_ID);

        GroupMember creatorMembership =
                membership(creator);

        List<SplitInputRequest> splits =
                List.of(
                        splitRequest(1L)
                );

        SharedExpenseCreateRequest request =
                request(
                        "Dinner",
                        "100.00",
                        1L,
                        SplitType.EQUAL,
                        splits
                );

        mockCreatorAndGroup(
                creator,
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                GROUP_ID,
                                1L
                        )
        ).thenReturn(
                Optional.of(creatorMembership)
        );

        when(
                splitStrategyResolver.resolve(
                        SplitType.EQUAL
                )
        ).thenReturn(splitStrategy);

        when(
                splitStrategy.calculate(
                        eq(new BigDecimal("100.00")),
                        eq(splits)
                )
        ).thenReturn(
                List.of(
                        new SplitResult(
                                1L,
                                new BigDecimal("100.00"),
                                null
                        )
                )
        );

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "duplicate idempotency key"
                );

        when(
                sharedExpenseRepository.saveAndFlush(
                        any(SharedExpense.class)
                )
        ).thenThrow(
                databaseException
        );

        DataIntegrityViolationException thrown =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () ->
                                writeService.create(
                                        GROUP_ID,
                                        CURRENT_USER_ID,
                                        IDEMPOTENCY_KEY,
                                        FINGERPRINT,
                                        request
                                )
                );

        assertSame(
                databaseException,
                thrown
        );

        verify(
                expenseSplitRepository,
                never()
        ).saveAll(anyList());

        verifyNoInteractions(
                activityService
        );
    }

    private void mockCreatorAndGroup(
            User creator,
            ExpenseGroup group
    ) {

        when(
                userRepository.findById(
                        CURRENT_USER_ID
                )
        ).thenReturn(
                Optional.of(creator)
        );

        when(
                expenseGroupRepository.findById(
                        GROUP_ID
                )
        ).thenReturn(
                Optional.of(group)
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                GROUP_ID,
                                CURRENT_USER_ID
                        )
        ).thenReturn(true);
    }

    private SharedExpenseCreateRequest request(
            String title,
            String amount,
            Long paidByUserId,
            SplitType splitType,
            List<SplitInputRequest> splits
    ) {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        lenient()
                .when(request.getTitle())
                .thenReturn(title);

        lenient()
                .when(request.getAmount())
                .thenReturn(
                        new BigDecimal(amount)
                );

        lenient()
                .when(request.getPaidByUserId())
                .thenReturn(paidByUserId);

        lenient()
                .when(request.getSplitType())
                .thenReturn(splitType);

        lenient()
                .when(request.getExpenseDate())
                .thenReturn(
                        LocalDate.of(
                                2026,
                                9,
                                15
                        )
                );

        lenient()
                .when(request.getSplits())
                .thenReturn(splits);

        return request;
    }

    private SplitInputRequest splitRequest(
            Long userId
    ) {

        SplitInputRequest request =
                mock(SplitInputRequest.class);

        lenient()
                .when(request.getUserId())
                .thenReturn(userId);

        return request;
    }

    private User user(
            Long id,
            String name,
            String email
    ) {

        User user =
                mock(User.class);

        lenient()
                .when(user.getId())
                .thenReturn(id);

        lenient()
                .when(user.getName())
                .thenReturn(name);

        lenient()
                .when(user.getEmail())
                .thenReturn(email);

        return user;
    }

    private ExpenseGroup group(
            Long id
    ) {

        ExpenseGroup group =
                mock(ExpenseGroup.class);

        lenient()
                .when(group.getId())
                .thenReturn(id);

        return group;
    }

    private GroupMember membership(
            User user
    ) {

        GroupMember membership =
                mock(GroupMember.class);

        lenient()
                .when(membership.getUser())
                .thenReturn(user);

        return membership;
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

        assertNotNull(actual);

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}