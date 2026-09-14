package com.expensemate.service;

import com.expensemate.dto.debt.DebtSettlementSuggestion;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import com.expensemate.dto.settlement.SettlementCreateRequest;
import com.expensemate.dto.settlement.SettlementResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
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

    // M13
    @Mock
    private ActivityService activityService;

    private SettlementService service;

    @BeforeEach
    void setUp() {

        service = new SettlementService(
                settlementRepository,
                userRepository,
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService,
                activityService
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

        when(
                settlementRepository.save(
                        any(Settlement.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

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
                settlementRepository
        ).save(
                any(Settlement.class)
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                contains("settled"),
                isNull()
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

        when(
                settlementRepository.save(
                        any(Settlement.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
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
                settlementRepository
        ).save(
                any(Settlement.class)
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                contains("300.00"),
                isNull()
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );

        verifyNoInteractions(
                activityService
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );

        verifyNoInteractions(
                activityService
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );

        verifyNoInteractions(
                activityService
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
                debtSimplificationService
        );

        verifyNoInteractions(
                activityService
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );

        verifyNoInteractions(
                activityService
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
                mock(Settlement.class);

        when(
                existingSettlement
                        .getGroup()
        ).thenReturn(
                group
        );

        when(
                existingSettlement
                        .getFromUser()
        ).thenReturn(
                debtor
        );

        when(
                existingSettlement
                        .getToUser()
        ).thenReturn(
                creditor
        );

        when(
                existingSettlement
                        .getAmount()
        ).thenReturn(
                new BigDecimal(
                        "300.00"
                )
        );

        when(
                existingSettlement
                        .getSettlementMode()
        ).thenReturn(
                SettlementMode.PARTIAL
        );

        when(
                existingSettlement
                        .getId()
        ).thenReturn(
                10L
        );

        when(
                existingSettlement
                        .getSettledAt()
        ).thenReturn(
                LocalDateTime.of(
                        2026,
                        9,
                        15,
                        0,
                        30
                )
        );

        when(
                userRepository
                        .findByEmail(
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );

        verifyNoInteractions(
                debtSimplificationService
        );

        /*
         * Very important M13 test:
         * idempotent retry must NOT duplicate
         * the activity/audit record.
         */
        verifyNoInteractions(
                activityService
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
                activityService
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
                userRepository
                        .findByEmail(
                                "test@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        debtor
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
                                2L
                        )
        ).thenReturn(
                true
        );

        Settlement settlement =
                mock(Settlement.class);

        when(
                settlement.getId()
        ).thenReturn(
                10L
        );

        when(
                settlement.getGroup()
        ).thenReturn(
                group
        );

        when(
                settlement.getFromUser()
        ).thenReturn(
                debtor
        );

        when(
                settlement.getToUser()
        ).thenReturn(
                creditor
        );

        when(
                settlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        "300.00"
                )
        );

        when(
                settlement.getSettlementMode()
        ).thenReturn(
                SettlementMode.PARTIAL
        );

        when(
                settlement.getSettledAt()
        ).thenReturn(
                LocalDateTime.of(
                        2026,
                        9,
                        15,
                        0,
                        30
                )
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
                userRepository
                        .findByEmail(
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
                                debtor.getId()
                        )
        ).thenReturn(
                true
        );

        when(
                userRepository
                        .findById(
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

    private User user(
            Long id,
            String name
    ) {

        User user =
                mock(User.class);

        /*
         * lenient because helper-created mocks are reused
         * across tests and not every test reads every field.
         */
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
                mock(ExpenseGroup.class);

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