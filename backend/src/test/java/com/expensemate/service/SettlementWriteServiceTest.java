package com.expensemate.service;

import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.enums.SettlementMode;
import com.expensemate.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementWriteServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private ExpenseGroup group;

    @Mock
    private User debtor;

    @Mock
    private User creditor;

    private SettlementWriteService service;

    @BeforeEach
    void setUp() {

        service = new SettlementWriteService(
                settlementRepository,
                activityService
        );
    }

    @Test
    void shouldPersistSettlementAndRecordActivity() {

        when(
                debtor.getName()
        ).thenReturn(
                "Test User"
        );

        when(
                creditor.getName()
        ).thenReturn(
                "Sree"
        );

        when(
                settlementRepository.saveAndFlush(
                        any(Settlement.class)
                )
        ).thenAnswer(
                invocation -> {

                    Settlement settlement =
                            invocation.getArgument(0);

                    /*
                     * The entity has no generated ID in a unit test,
                     * but every other field can be verified directly.
                     */
                    return settlement;
                }
        );

        Settlement result =
                service.create(
                        group,
                        debtor,
                        creditor,
                        new BigDecimal(
                                "300.00"
                        ),
                        SettlementMode.PARTIAL,
                        "settlement-key-1"
                );

        assertNotNull(
                result
        );

        assertSame(
                group,
                result.getGroup()
        );

        assertSame(
                debtor,
                result.getFromUser()
        );

        assertSame(
                creditor,
                result.getToUser()
        );

        assertSame(
                debtor,
                result.getCreatedBy()
        );

        assertEquals(
                0,
                new BigDecimal(
                        "300.00"
                ).compareTo(
                        result.getAmount()
                )
        );

        assertEquals(
                SettlementMode.PARTIAL,
                result.getSettlementMode()
        );

        assertEquals(
                "settlement-key-1",
                result.getIdempotencyKey()
        );

        verify(
                settlementRepository
        ).saveAndFlush(
                any(Settlement.class)
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                eq("Test User settled ₹300.00 with Sree"),
                isNull()
        );
    }

    @Test
    void shouldUseGeneratedSettlementIdAsActivityReference() {

        Settlement savedSettlement =
                mock(
                        Settlement.class
                );

        when(
                savedSettlement.getId()
        ).thenReturn(
                25L
        );

        when(
                savedSettlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        "450.00"
                )
        );

        when(
                debtor.getName()
        ).thenReturn(
                "Test User"
        );

        when(
                creditor.getName()
        ).thenReturn(
                "Sree"
        );

        when(
                settlementRepository.saveAndFlush(
                        any(Settlement.class)
                )
        ).thenReturn(
                savedSettlement
        );

        Settlement result =
                service.create(
                        group,
                        debtor,
                        creditor,
                        new BigDecimal(
                                "450.00"
                        ),
                        SettlementMode.PARTIAL,
                        "settlement-key-2"
                );

        assertSame(
                savedSettlement,
                result
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                eq("Test User settled ₹450.00 with Sree"),
                eq(25L)
        );
    }

    @Test
    void shouldPersistSettlementBeforeRecordingActivity() {

        Settlement savedSettlement =
                mock(
                        Settlement.class
                );

        when(
                savedSettlement.getId()
        ).thenReturn(
                30L
        );

        when(
                savedSettlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        "200.00"
                )
        );

        when(
                debtor.getName()
        ).thenReturn(
                "Test User"
        );

        when(
                creditor.getName()
        ).thenReturn(
                "Sree"
        );

        when(
                settlementRepository.saveAndFlush(
                        any(Settlement.class)
                )
        ).thenReturn(
                savedSettlement
        );

        service.create(
                group,
                debtor,
                creditor,
                new BigDecimal(
                        "200.00"
                ),
                SettlementMode.PARTIAL,
                "settlement-key-3"
        );

        InOrder inOrder =
                inOrder(
                        settlementRepository,
                        activityService
                );

        inOrder.verify(
                settlementRepository
        ).saveAndFlush(
                any(Settlement.class)
        );

        inOrder.verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                anyString(),
                eq(30L)
        );
    }

    @Test
    void shouldNotRecordActivityWhenSettlementPersistenceFails() {

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Duplicate idempotency key"
                );

        when(
                settlementRepository.saveAndFlush(
                        any(Settlement.class)
                )
        ).thenThrow(
                databaseException
        );

        DataIntegrityViolationException thrown =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () ->
                                service.create(
                                        group,
                                        debtor,
                                        creditor,
                                        new BigDecimal(
                                                "300.00"
                                        ),
                                        SettlementMode.PARTIAL,
                                        "duplicate-key"
                                )
                );

        assertSame(
                databaseException,
                thrown
        );

        verify(
                settlementRepository
        ).saveAndFlush(
                any(Settlement.class)
        );

        verifyNoInteractions(
                activityService
        );
    }

    @Test
    void shouldPropagateActivityPersistenceFailure() {

        Settlement savedSettlement =
                mock(
                        Settlement.class
                );

        when(
                savedSettlement.getId()
        ).thenReturn(
                40L
        );

        when(
                savedSettlement.getAmount()
        ).thenReturn(
                new BigDecimal(
                        "125.00"
                )
        );

        when(
                debtor.getName()
        ).thenReturn(
                "Test User"
        );

        when(
                creditor.getName()
        ).thenReturn(
                "Sree"
        );

        when(
                settlementRepository.saveAndFlush(
                        any(Settlement.class)
                )
        ).thenReturn(
                savedSettlement
        );

        RuntimeException activityException =
                new RuntimeException(
                        "Activity persistence failed"
                );

        doThrow(
                activityException
        ).when(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                anyString(),
                eq(40L)
        );

        RuntimeException thrown =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                service.create(
                                        group,
                                        debtor,
                                        creditor,
                                        new BigDecimal(
                                                "125.00"
                                        ),
                                        SettlementMode.PARTIAL,
                                        "activity-failure-key"
                                )
                );

        assertSame(
                activityException,
                thrown
        );

        verify(
                settlementRepository
        ).saveAndFlush(
                any(Settlement.class)
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(debtor),
                eq(ActivityType.SETTLEMENT_CREATED),
                eq("Test User settled ₹125.00 with Sree"),
                eq(40L)
        );
    }
}