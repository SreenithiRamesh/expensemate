package com.expensemate.service;

import com.expensemate.dto.debt.DebtSettlementSuggestion;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import com.expensemate.dto.settlement.SettlementCreateRequest;
import com.expensemate.dto.settlement.SettlementResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.SettlementMode;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SettlementRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private DebtSimplificationService debtSimplificationService;

    @Mock
    private SettlementWriteService settlementWriteService;

    private SettlementService service;

    @BeforeEach
    void setUp() {

        service = new SettlementService(
                settlementRepository,
                userRepository,
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldCreateFullSettlement() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "full-key-1"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "800.00"
                )
        );

        prepareSuccessfulWrite();

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.FULL,
                        null
                );

        SettlementResponse response =
                service.createSettlement(
                        2L,
                        "test@example.com",
                        "full-key-1",
                        request
                );

        assertMoney(
                "800.00",
                response.amount()
        );

        assertEquals(
                SettlementMode.FULL,
                response.mode()
        );

        assertEquals(
                2L,
                response.groupId()
        );

        assertEquals(
                2L,
                response.fromUserId()
        );

        assertEquals(
                1L,
                response.toUserId()
        );

        verify(
                settlementWriteService
        ).create(
                eq(group),
                eq(debtor),
                eq(creditor),
                eq(new BigDecimal("800.00")),
                eq(SettlementMode.FULL),
                eq("full-key-1")
        );
    }

    @Test
    void shouldCreatePartialSettlement() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "partial-key-1"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "800.00"
                )
        );

        prepareSuccessfulWrite();

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

        SettlementResponse response =
                service.createSettlement(
                        2L,
                        "test@example.com",
                        "partial-key-1",
                        request
                );

        assertMoney(
                "300.00",
                response.amount()
        );

        assertEquals(
                SettlementMode.PARTIAL,
                response.mode()
        );

        verify(
                settlementWriteService
        ).create(
                eq(group),
                eq(debtor),
                eq(creditor),
                eq(new BigDecimal("300.00")),
                eq(SettlementMode.PARTIAL),
                eq("partial-key-1")
        );
    }

    @Test
    void shouldRecoverWhenConcurrentRequestUsesSameIdempotencyKey() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        Settlement winningSettlement =
                settlement(
                        50L,
                        group,
                        debtor,
                        creditor,
                        "300.00",
                        SettlementMode.PARTIAL
                );

        when(
                userRepository.findByEmail(
                        "test@example.com"
                )
        ).thenReturn(
                Optional.of(
                        debtor
                )
        );

        /*
         * First query occurs before writing and returns empty.
         * Second query occurs after the unique-key collision
         * and returns the settlement committed by the winner.
         */
        when(
                settlementRepository
                        .findByIdempotencyKey(
                                "concurrent-key"
                        )
        ).thenReturn(
                Optional.empty(),
                Optional.of(
                        winningSettlement
                )
        );

        when(
                expenseGroupRepository.findById(
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
                                2L
                        )
        ).thenReturn(
                true
        );

        when(
                userRepository.findById(
                        1L
                )
        ).thenReturn(
                Optional.of(
                        creditor
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
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "800.00"
                )
        );

        when(
                settlementWriteService.create(
                        any(ExpenseGroup.class),
                        any(User.class),
                        any(User.class),
                        any(BigDecimal.class),
                        any(SettlementMode.class),
                        anyString()
                )
        ).thenThrow(
                new DataIntegrityViolationException(
                        "Duplicate idempotency key"
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

        SettlementResponse response =
                service.createSettlement(
                        2L,
                        "test@example.com",
                        "concurrent-key",
                        request
                );

        assertEquals(
                50L,
                response.id()
        );

        assertEquals(
                2L,
                response.groupId()
        );

        assertEquals(
                2L,
                response.fromUserId()
        );

        assertEquals(
                1L,
                response.toUserId()
        );

        assertMoney(
                "300.00",
                response.amount()
        );

        assertEquals(
                SettlementMode.PARTIAL,
                response.mode()
        );

        verify(
                settlementRepository,
                times(2)
        ).findByIdempotencyKey(
                "concurrent-key"
        );
    }

    @Test
    void shouldRethrowConstraintFailureWhenWinnerCannotBeFound() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "missing-winner-key"
        );

        /*
         * Override the helper's single empty result with two
         * empty results: one before writing and one during recovery.
         */
        when(
                settlementRepository
                        .findByIdempotencyKey(
                                "missing-winner-key"
                        )
        ).thenReturn(
                Optional.empty(),
                Optional.empty()
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "800.00"
                )
        );

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Unexpected database constraint failure"
                );

        when(
                settlementWriteService.create(
                        any(ExpenseGroup.class),
                        any(User.class),
                        any(User.class),
                        any(BigDecimal.class),
                        any(SettlementMode.class),
                        anyString()
                )
        ).thenThrow(
                databaseException
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

        DataIntegrityViolationException thrown =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "missing-winner-key",
                                        request
                                )
                );

        assertSame(
                databaseException,
                thrown
        );

        verify(
                settlementRepository,
                times(2)
        ).findByIdempotencyKey(
                "missing-winner-key"
        );
    }

    @Test
    void shouldRejectOverpayment() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "overpay-key"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "500.00"
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "700.00"
                        )
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "overpay-key",
                                        request
                                )
                );

        assertEquals(
                "Settlement amount cannot exceed outstanding debt",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementWriteService
        );
    }

    @Test
    void shouldRejectZeroAmount() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "zero-key"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "500.00"
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        BigDecimal.ZERO
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "zero-key",
                                        request
                                )
                );

        assertEquals(
                "Settlement amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementWriteService
        );
    }

    @Test
    void shouldRejectNegativeAmount() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "negative-key"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                debtResponse(
                        "500.00"
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "-10.00"
                        )
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "negative-key",
                                        request
                                )
                );

        assertEquals(
                "Settlement amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementWriteService
        );
    }

    @Test
    void shouldRejectSelfSettlement() {

        User currentUser =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                settlementRepository
                        .findByIdempotencyKey(
                                "self-key"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                expenseGroupRepository.findById(
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
                userRepository.findById(
                        1L
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.FULL,
                        null
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "sree@example.com",
                                        "self-key",
                                        request
                                )
                );

        assertEquals(
                "You cannot settle debt with yourself",
                exception.getMessage()
        );

        verifyNoInteractions(
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldRejectWhenNoOutstandingDebtExists() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        prepareValidGroup(
                debtor,
                creditor,
                group,
                "test@example.com",
                "no-debt-key"
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                2L,
                                "test@example.com"
                        )
        ).thenReturn(
                new DebtSimplificationResponse(
                        2L,
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.FULL,
                        null
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "no-debt-key",
                                        request
                                )
                );

        assertEquals(
                "No outstanding debt exists for this settlement",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementWriteService
        );
    }

    @Test
    void shouldReturnExistingSettlementForSameIdempotencyKey() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        Settlement existingSettlement =
                settlement(
                        10L,
                        group,
                        debtor,
                        creditor,
                        "300.00",
                        SettlementMode.PARTIAL
                );

        when(
                userRepository.findByEmail(
                        "test@example.com"
                )
        ).thenReturn(
                Optional.of(
                        debtor
                )
        );

        when(
                settlementRepository
                        .findByIdempotencyKey(
                                "retry-key"
                        )
        ).thenReturn(
                Optional.of(
                        existingSettlement
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

        SettlementResponse response =
                service.createSettlement(
                        2L,
                        "test@example.com",
                        "retry-key",
                        request
                );

        assertEquals(
                10L,
                response.id()
        );

        assertMoney(
                "300.00",
                response.amount()
        );

        verifyNoInteractions(
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldRejectIdempotencyKeyUsedByDifferentUser() {

        User currentUser =
                user(
                        3L,
                        "Another User"
                );

        User originalDebtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        Settlement existingSettlement =
                settlement(
                        10L,
                        group,
                        originalDebtor,
                        creditor,
                        "300.00",
                        SettlementMode.PARTIAL
                );

        when(
                userRepository.findByEmail(
                        "another@example.com"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                settlementRepository
                        .findByIdempotencyKey(
                                "used-key"
                        )
        ).thenReturn(
                Optional.of(
                        existingSettlement
                )
        );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "another@example.com",
                                        "used-key",
                                        request
                                )
                );

        assertEquals(
                "Idempotency key has already been used",
                exception.getMessage()
        );

        verifyNoInteractions(
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.FULL,
                        null
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        "   ",
                                        request
                                )
                );

        assertEquals(
                "Idempotency-Key header is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementRepository,
                userRepository,
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThanOneHundredCharacters() {

        String idempotencyKey =
                "a".repeat(
                        101
                );

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        1L,
                        SettlementMode.FULL,
                        null
                );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                service.createSettlement(
                                        2L,
                                        "test@example.com",
                                        idempotencyKey,
                                        request
                                )
                );

        assertEquals(
                "Idempotency-Key must not exceed 100 characters",
                exception.getMessage()
        );

        verifyNoInteractions(
                settlementRepository,
                userRepository,
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService,
                settlementWriteService
        );
    }

    @Test
    void shouldReturnSettlementHistory() {

        User debtor =
                user(
                        2L,
                        "Test User"
                );

        User creditor =
                user(
                        1L,
                        "Sree"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        when(
                userRepository.findByEmail(
                        "test@example.com"
                )
        ).thenReturn(
                Optional.of(
                        debtor
                )
        );

        when(
                expenseGroupRepository.findById(
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
                                2L
                        )
        ).thenReturn(
                true
        );

        Settlement settlement =
                settlement(
                        10L,
                        group,
                        debtor,
                        creditor,
                        "300.00",
                        SettlementMode.PARTIAL
                );

        when(
                settlementRepository
                        .findByGroup_IdOrderBySettledAtDesc(
                                2L
                        )
        ).thenReturn(
                List.of(
                        settlement
                )
        );

        List<SettlementResponse> history =
                service.getSettlementHistory(
                        2L,
                        "test@example.com"
                );

        assertEquals(
                1,
                history.size()
        );

        SettlementResponse response =
                history.getFirst();

        assertEquals(
                10L,
                response.id()
        );

        assertEquals(
                2L,
                response.groupId()
        );

        assertMoney(
                "300.00",
                response.amount()
        );

        assertEquals(
                SettlementMode.PARTIAL,
                response.mode()
        );

        verify(
                settlementRepository
        ).findByGroup_IdOrderBySettledAtDesc(
                2L
        );
    }

    private void prepareValidGroup(
            User debtor,
            User creditor,
            ExpenseGroup group,
            String email,
            String idempotencyKey
    ) {

        when(
                userRepository.findByEmail(
                        email
                )
        ).thenReturn(
                Optional.of(
                        debtor
                )
        );

        when(
                settlementRepository
                        .findByIdempotencyKey(
                                idempotencyKey
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                expenseGroupRepository.findById(
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
                                debtor.getId()
                        )
        ).thenReturn(
                true
        );

        when(
                userRepository.findById(
                        creditor.getId()
                )
        ).thenReturn(
                Optional.of(
                        creditor
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                creditor.getId()
                        )
        ).thenReturn(
                true
        );
    }

    private void prepareSuccessfulWrite() {

        when(
                settlementWriteService.create(
                        any(ExpenseGroup.class),
                        any(User.class),
                        any(User.class),
                        any(BigDecimal.class),
                        any(SettlementMode.class),
                        anyString()
                )
        ).thenAnswer(
                invocation ->
                        new Settlement(
                                invocation.getArgument(0),
                                invocation.getArgument(1),
                                invocation.getArgument(2),
                                invocation.getArgument(3),
                                invocation.getArgument(4),
                                invocation.getArgument(5),
                                invocation.getArgument(1)
                        )
        );
    }

    private DebtSimplificationResponse debtResponse(
            String amount
    ) {

        return new DebtSimplificationResponse(
                2L,
                List.of(
                        new DebtSettlementSuggestion(
                                2L,
                                "Test User",
                                1L,
                                "Sree",
                                new BigDecimal(
                                        amount
                                )
                        )
                ),
                List.of(),
                List.of()
        );
    }

    private Settlement settlement(
            Long id,
            ExpenseGroup group,
            User fromUser,
            User toUser,
            String amount,
            SettlementMode mode
    ) {

        Settlement settlement =
                mock(
                        Settlement.class
                );

        lenient()
                .when(
                        settlement.getId()
                )
                .thenReturn(
                        id
                );

        lenient()
                .when(
                        settlement.getGroup()
                )
                .thenReturn(
                        group
                );

        lenient()
                .when(
                        settlement.getFromUser()
                )
                .thenReturn(
                        fromUser
                );

        lenient()
                .when(
                        settlement.getToUser()
                )
                .thenReturn(
                        toUser
                );

        lenient()
                .when(
                        settlement.getAmount()
                )
                .thenReturn(
                        new BigDecimal(
                                amount
                        )
                );

        lenient()
                .when(
                        settlement.getSettlementMode()
                )
                .thenReturn(
                        mode
                );

        lenient()
                .when(
                        settlement.getSettledAt()
                )
                .thenReturn(
                        LocalDateTime.of(
                                2026,
                                9,
                                15,
                                0,
                                30
                        )
                );

        return settlement;
    }

    private User user(
            Long id,
            String name
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