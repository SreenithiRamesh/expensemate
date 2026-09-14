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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private SettlementService service;

    @BeforeEach
    void setUp() {

        service = new SettlementService(
                settlementRepository,
                userRepository,
                expenseGroupRepository,
                groupMemberRepository,
                debtSimplificationService
        );
    }

    @Test
    void shouldCreateFullSettlement() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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

        verify(
                settlementRepository
        ).save(
                any(Settlement.class)
        );
    }

    @Test
    void shouldCreatePartialSettlement() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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
    }

    @Test
    void shouldRejectOverpayment() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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
                "Settlement amount exceeds outstanding debt",
                exception.getMessage()
        );

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );
    }

    @Test
    void shouldRejectZeroAmount() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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
    }

    @Test
    void shouldRejectNegativeAmount() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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
    }

    @Test
    void shouldRejectSelfSettlement() {

        User user = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        when(
                userRepository
                        .findByEmail(
                                "sree@example.com"
                        )
        ).thenReturn(
                Optional.of(
                        user
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
                userRepository
                        .findById(
                                1L
                        )
        ).thenReturn(
                Optional.of(
                        user
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

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );
    }

    @Test
    void shouldRejectWhenNoOutstandingDebtExists() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        prepareValidGroup(
                debtor,
                creditor,
                group
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
    }

    @Test
    void shouldReturnExistingSettlementForSameIdempotencyKey() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
        );

        Settlement existing =
                settlement(
                        10L,
                        group,
                        debtor,
                        creditor,
                        "300.00",
                        SettlementMode.PARTIAL
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
                                "duplicate-key"
                        )
        ).thenReturn(
                Optional.of(
                        existing
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
                        "duplicate-key",
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
                userRepository,
                debtSimplificationService
        );

        verify(
                settlementRepository,
                never()
        ).save(
                any(Settlement.class)
        );
    }

    @Test
    void shouldReturnSettlementHistory() {

        User debtor = user(
                2L,
                "Test User"
        );

        User creditor = user(
                1L,
                "Sree"
        );

        ExpenseGroup group = group(
                2L
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

        assertEquals(
                10L,
                history.get(0).id()
        );

        assertMoney(
                "300.00",
                history.get(0).amount()
        );

        assertEquals(
                SettlementMode.PARTIAL,
                history.get(0).mode()
        );
    }

    private void prepareValidGroup(
            User debtor,
            User creditor,
            ExpenseGroup group
    ) {

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
                                anyString()
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
                                2L
                        )
        ).thenReturn(
                true
        );

        when(
                userRepository
                        .findById(
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
                mock(
                        User.class
                );

        /*
         * These helper stubs are intentionally lenient.
         *
         * Some negative-path tests stop before getId()
         * or getName() is used, while successful paths
         * need both values.
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
                mock(
                        ExpenseGroup.class
                );

        /*
         * Some validation tests fail before the group ID
         * is required by the response mapper.
         */
        lenient()
                .when(
                        group.getId()
                )
                .thenReturn(
                        id
                );

        return group;
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

        when(
                settlement.getId()
        ).thenReturn(
                id
        );

        when(
                settlement.getGroup()
        ).thenReturn(
                group
        );

        when(
                settlement.getFromUser()
        ).thenReturn(
                fromUser
        );

        when(
                settlement.getToUser()
        ).thenReturn(
                toUser
        );

        when(
                settlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        amount
                )
        );

        when(
                settlement.getSettlementMode()
        ).thenReturn(
                mode
        );

        when(
                settlement.getSettledAt()
        ).thenReturn(
                LocalDateTime.now()
        );

        return settlement;
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

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