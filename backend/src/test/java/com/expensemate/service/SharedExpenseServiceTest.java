package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
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
public class SharedExpenseServiceTest {

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

    private SharedExpenseService sharedExpenseService;

    private User currentUser;
    private User payer;
    private User secondMember;
    private ExpenseGroup group;
    private GroupMember payerMembership;
    private GroupMember secondMemberMembership;

    @BeforeEach
    void setUp() {

        sharedExpenseService = new SharedExpenseService(
                sharedExpenseRepository,
                expenseSplitRepository,
                expenseGroupRepository,
                groupMemberRepository,
                userRepository,
                splitStrategyResolver
        );

        currentUser = mock(User.class);
        payer = mock(User.class);
        secondMember = mock(User.class);

        group = mock(ExpenseGroup.class);

        payerMembership = mock(GroupMember.class);
        secondMemberMembership = mock(GroupMember.class);
    }

    @Test
    void shouldRejectNonMemberCreatingSharedExpense() {

        SharedExpenseCreateRequest request = createEqualRequest();

        when(currentUser.getId()).thenReturn(1L);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(currentUser));

        when(expenseGroupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository
                .existsByGroupIdAndUserId(10L, 1L))
                .thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> sharedExpenseService.createSharedExpense(
                                10L,
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );

        verify(sharedExpenseRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectPayerWhoIsNotGroupMember() {

        SharedExpenseCreateRequest request = createEqualRequest();

        mockCurrentUserAsGroupMember();

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 2L))
                .thenReturn(Optional.empty());

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> sharedExpenseService.createSharedExpense(
                                10L,
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "Payer must be a member of this group",
                exception.getMessage()
        );

        verify(sharedExpenseRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectSplitParticipantWhoIsNotGroupMember() {

        SharedExpenseCreateRequest request = createEqualRequest();

        mockCurrentUserAsGroupMember();

        when(payerMembership.getUser())
                .thenReturn(payer);

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(payerMembership));

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 3L))
                .thenReturn(Optional.empty());

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> sharedExpenseService.createSharedExpense(
                                10L,
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "All split participants must be members of this group",
                exception.getMessage()
        );

        verify(sharedExpenseRepository, never())
                .save(any());
    }

    @Test
    void shouldRejectDuplicateSplitMembers() {

        SharedExpenseCreateRequest request =
                createDuplicateSplitRequest();

        mockCurrentUserAsGroupMember();

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> sharedExpenseService.createSharedExpense(
                                10L,
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "Duplicate split members are not allowed",
                exception.getMessage()
        );

        verify(splitStrategyResolver, never())
                .resolve(any());

        verify(sharedExpenseRepository, never())
                .save(any());
    }

    @Test
    void shouldUseCorrectStrategyAndPersistSharedExpenseWithSplits() {

        SharedExpenseCreateRequest request =
                createEqualRequest();

        mockCurrentUserAsGroupMember();

        when(payer.getId()).thenReturn(2L);
        when(payer.getName()).thenReturn("Test User");

        when(secondMember.getId()).thenReturn(3L);
        when(secondMember.getName()).thenReturn("Second Member");
        when(secondMember.getEmail()).thenReturn("second@example.com");

        when(payerMembership.getUser())
                .thenReturn(payer);

        when(secondMemberMembership.getUser())
                .thenReturn(secondMember);

        when(group.getId()).thenReturn(10L);

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(payerMembership));

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(secondMemberMembership));

        when(splitStrategyResolver.resolve(SplitType.EQUAL))
                .thenReturn(splitStrategy);

        List<SplitResult> calculatedSplits =
                List.of(
                        new SplitResult(
                                2L,
                                new BigDecimal("50.00"),
                                null
                        ),
                        new SplitResult(
                                3L,
                                new BigDecimal("50.00"),
                                null
                        )
                );

        when(splitStrategy.calculate(
                eq(new BigDecimal("100.00")),
                anyList()
        )).thenReturn(calculatedSplits);

        when(sharedExpenseRepository
                .save(any(SharedExpense.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(payer));

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(secondMember));

        SharedExpenseResponse response =
                sharedExpenseService.createSharedExpense(
                        10L,
                        "sree@example.com",
                        request
                );

        verify(splitStrategyResolver)
                .resolve(SplitType.EQUAL);

        verify(splitStrategy)
                .calculate(
                        new BigDecimal("100.00"),
                        request.getSplits()
                );

        verify(sharedExpenseRepository)
                .save(any(SharedExpense.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExpenseSplit>> splitCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(expenseSplitRepository)
                .saveAll(splitCaptor.capture());

        List<ExpenseSplit> savedSplits =
                splitCaptor.getValue();

        assertEquals(2, savedSplits.size());

        assertEquals(
                new BigDecimal("50.00"),
                savedSplits.get(0).getShareAmount()
        );

        assertEquals(
                new BigDecimal("50.00"),
                savedSplits.get(1).getShareAmount()
        );

        assertNotNull(response);

        assertEquals(
                new BigDecimal("100.00"),
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
    }

    @Test
    void shouldRejectCalculatedSplitsWhenTotalDoesNotMatchExpenseAmount() {

        SharedExpenseCreateRequest request =
                createEqualRequest();

        mockCurrentUserAsGroupMember();

        when(payerMembership.getUser())
                .thenReturn(payer);

        when(secondMemberMembership.getUser())
                .thenReturn(secondMember);

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(payerMembership));

        when(groupMemberRepository
                .findByGroupIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(secondMemberMembership));

        when(splitStrategyResolver.resolve(SplitType.EQUAL))
                .thenReturn(splitStrategy);

        when(splitStrategy.calculate(
                eq(new BigDecimal("100.00")),
                anyList()
        )).thenReturn(
                List.of(
                        new SplitResult(
                                2L,
                                new BigDecimal("40.00"),
                                null
                        ),
                        new SplitResult(
                                3L,
                                new BigDecimal("40.00"),
                                null
                        )
                )
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> sharedExpenseService.createSharedExpense(
                                10L,
                                "sree@example.com",
                                request
                        )
                );

        assertEquals(
                "Split amounts must equal the expense amount",
                exception.getMessage()
        );

        verify(sharedExpenseRepository, never())
                .save(any());
    }

    private void mockCurrentUserAsGroupMember() {

        when(currentUser.getId()).thenReturn(1L);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(currentUser));

        when(expenseGroupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository
                .existsByGroupIdAndUserId(10L, 1L))
                .thenReturn(true);
    }

    private SharedExpenseCreateRequest createEqualRequest() {

        SharedExpenseCreateRequest request =
                new SharedExpenseCreateRequest();

        request.setTitle("Dinner");

        request.setAmount(
                new BigDecimal("100.00")
        );

        request.setPaidByUserId(2L);

        request.setSplitType(
                SplitType.EQUAL
        );

        request.setExpenseDate(
                LocalDate.of(2026, 9, 14)
        );

        request.setSplits(
                List.of(
                        split(2L, null),
                        split(3L, null)
                )
        );

        return request;
    }

    private SharedExpenseCreateRequest createDuplicateSplitRequest() {

        SharedExpenseCreateRequest request =
                new SharedExpenseCreateRequest();

        request.setTitle("Dinner");

        request.setAmount(
                new BigDecimal("100.00")
        );

        request.setPaidByUserId(2L);

        request.setSplitType(
                SplitType.EQUAL
        );

        request.setExpenseDate(
                LocalDate.of(2026, 9, 14)
        );

        request.setSplits(
                List.of(
                        split(2L, null),
                        split(2L, null)
                )
        );

        return request;
    }

    private SplitInputRequest split(
            Long userId,
            BigDecimal value
    ) {

        SplitInputRequest split =
                new SplitInputRequest();

        split.setUserId(userId);
        split.setValue(value);

        return split;
    }
}