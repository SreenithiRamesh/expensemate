package com.expensemate.service;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.ExpenseSplit;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SharedExpenseServiceTest {

    private static final Long GROUP_ID = 2L;
    private static final Long CURRENT_USER_ID = 1L;
    private static final String CURRENT_USER_EMAIL = "sree@example.com";
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
    private SharedExpenseFingerprintService fingerprintService;

    @Mock
    private SharedExpenseWriteService writeService;

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
                        fingerprintService,
                        writeService
                );
    }

    @Test
    void shouldCreateSharedExpenseThroughWriter() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        User payer =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        SharedExpense savedExpense =
                sharedExpense(
                        group,
                        payer,
                        currentUser,
                        "Dinner",
                        "100.00",
                        IDEMPOTENCY_KEY,
                        FINGERPRINT
                );

        ExpenseSplit split =
                expenseSplit(
                        savedExpense,
                        currentUser,
                        "100.00"
                );

        when(
                userRepository.findByEmail(
                        CURRENT_USER_EMAIL
                )
        ).thenReturn(
                Optional.of(currentUser)
        );

        when(
                fingerprintService.fingerprint(
                        GROUP_ID,
                        request
                )
        ).thenReturn(
                FINGERPRINT
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                )
        ).thenReturn(
                savedExpense
        );

        when(
                expenseSplitRepository.findByExpenseId(
                        savedExpense.getId()
                )
        ).thenReturn(
                List.of(split)
        );

        SharedExpenseResponse response =
                sharedExpenseService
                        .createSharedExpense(
                                GROUP_ID,
                                CURRENT_USER_EMAIL,
                                IDEMPOTENCY_KEY,
                                request
                        );

        assertNotNull(response);
        assertEquals(
                "Dinner",
                response.getTitle()
        );

        assertMoney(
                "100.00",
                response.getAmount()
        );

        assertEquals(
                1,
                response.getSplits().size()
        );

        verify(
                writeService
        ).create(
                GROUP_ID,
                CURRENT_USER_ID,
                IDEMPOTENCY_KEY,
                FINGERPRINT,
                request
        );
    }

    @Test
    void shouldReplayExistingExpenseForSameKeyAndFingerprint() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        SharedExpense existingExpense =
                sharedExpense(
                        group,
                        currentUser,
                        currentUser,
                        "Dinner",
                        "100.00",
                        IDEMPOTENCY_KEY,
                        FINGERPRINT
                );

        ExpenseSplit existingSplit =
                expenseSplit(
                        existingExpense,
                        currentUser,
                        "100.00"
                );

        mockCurrentUserAndFingerprint(
                currentUser,
                request
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.of(existingExpense)
        );

        when(
                expenseSplitRepository.findByExpenseId(
                        existingExpense.getId()
                )
        ).thenReturn(
                List.of(existingSplit)
        );

        SharedExpenseResponse response =
                sharedExpenseService
                        .createSharedExpense(
                                GROUP_ID,
                                CURRENT_USER_EMAIL,
                                IDEMPOTENCY_KEY,
                                request
                        );

        assertNotNull(response);

        assertEquals(
                "Dinner",
                response.getTitle()
        );

        assertMoney(
                "100.00",
                response.getAmount()
        );

        verifyNoInteractions(
                writeService
        );
    }

    @Test
    void shouldRejectExistingKeyForDifferentFingerprint() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        SharedExpense existingExpense =
                sharedExpense(
                        group,
                        currentUser,
                        currentUser,
                        "Dinner",
                        "100.00",
                        IDEMPOTENCY_KEY,
                        "old-fingerprint"
                );

        mockCurrentUserAndFingerprint(
                currentUser,
                request
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.of(existingExpense)
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );

        assertEquals(
                "Idempotency key has already been used for a different request",
                exception.getMessage()
        );

        verifyNoInteractions(
                writeService
        );
    }

    @Test
    void shouldRecoverConcurrentDuplicateByReplayingWinner() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        SharedExpense winner =
                sharedExpense(
                        group,
                        currentUser,
                        currentUser,
                        "Dinner",
                        "100.00",
                        IDEMPOTENCY_KEY,
                        FINGERPRINT
                );

        ExpenseSplit winnerSplit =
                expenseSplit(
                        winner,
                        currentUser,
                        "100.00"
                );

        mockCurrentUserAndFingerprint(
                currentUser,
                request
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.empty(),
                Optional.of(winner)
        );

        when(
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                )
        ).thenThrow(
                new DataIntegrityViolationException(
                        "duplicate key"
                )
        );

        when(
                expenseSplitRepository.findByExpenseId(
                        winner.getId()
                )
        ).thenReturn(
                List.of(winnerSplit)
        );

        SharedExpenseResponse response =
                sharedExpenseService
                        .createSharedExpense(
                                GROUP_ID,
                                CURRENT_USER_EMAIL,
                                IDEMPOTENCY_KEY,
                                request
                        );

        assertNotNull(response);

        assertEquals(
                "Dinner",
                response.getTitle()
        );

        assertMoney(
                "100.00",
                response.getAmount()
        );

        verify(
                sharedExpenseRepository,
                times(2)
        ).findByCreatedBy_IdAndIdempotencyKey(
                CURRENT_USER_ID,
                IDEMPOTENCY_KEY
        );
    }

    @Test
    void shouldRejectConcurrentWinnerWhenFingerprintDiffers() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        ExpenseGroup group =
                group(GROUP_ID);

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        SharedExpense winner =
                sharedExpense(
                        group,
                        currentUser,
                        currentUser,
                        "Different Dinner",
                        "200.00",
                        IDEMPOTENCY_KEY,
                        "different-fingerprint"
                );

        mockCurrentUserAndFingerprint(
                currentUser,
                request
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.empty(),
                Optional.of(winner)
        );

        when(
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                )
        ).thenThrow(
                new DataIntegrityViolationException(
                        "duplicate key"
                )
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );

        assertEquals(
                "Idempotency key has already been used for a different request",
                exception.getMessage()
        );
    }

    @Test
    void shouldRethrowUnrelatedIntegrityViolationWhenNoWinnerExists() {

        User currentUser =
                user(
                        CURRENT_USER_ID,
                        "Sree",
                        CURRENT_USER_EMAIL
                );

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        mockCurrentUserAndFingerprint(
                currentUser,
                request
        );

        when(
                sharedExpenseRepository
                        .findByCreatedBy_IdAndIdempotencyKey(
                                CURRENT_USER_ID,
                                IDEMPOTENCY_KEY
                        )
        ).thenReturn(
                Optional.empty()
        );

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "some other database constraint"
                );

        when(
                writeService.create(
                        GROUP_ID,
                        CURRENT_USER_ID,
                        IDEMPOTENCY_KEY,
                        FINGERPRINT,
                        request
                )
        ).thenThrow(
                databaseException
        );

        DataIntegrityViolationException thrown =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );

        assertSame(
                databaseException,
                thrown
        );

        verify(
                sharedExpenseRepository,
                times(2)
        ).findByCreatedBy_IdAndIdempotencyKey(
                CURRENT_USER_ID,
                IDEMPOTENCY_KEY
        );
    }

    @Test
    void shouldRejectMissingIdempotencyKey() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                null,
                                                request
                                        )
                );

        assertEquals(
                "Idempotency-Key header is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                fingerprintService,
                writeService
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
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
                fingerprintService,
                writeService
        );
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThan100Characters() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        String longKey =
                "a".repeat(101);

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                longKey,
                                                request
                                        )
                );

        assertEquals(
                "Idempotency-Key must not exceed 100 characters",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                fingerprintService,
                writeService
        );
    }

    @Test
    void shouldRejectUnknownUserBeforeFingerprinting() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        when(
                userRepository.findByEmail(
                        CURRENT_USER_EMAIL
                )
        ).thenReturn(
                Optional.empty()
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                CURRENT_USER_EMAIL,
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                fingerprintService,
                writeService
        );
    }

    @Test
    void shouldRejectBlankUserEmailBeforeFingerprinting() {

        SharedExpenseCreateRequest request =
                mock(SharedExpenseCreateRequest.class);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                sharedExpenseService
                                        .createSharedExpense(
                                                GROUP_ID,
                                                "   ",
                                                IDEMPOTENCY_KEY,
                                                request
                                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                fingerprintService,
                writeService
        );
    }

    private void mockCurrentUserAndFingerprint(
            User currentUser,
            SharedExpenseCreateRequest request
    ) {

        when(
                userRepository.findByEmail(
                        CURRENT_USER_EMAIL
                )
        ).thenReturn(
                Optional.of(currentUser)
        );

        when(
                fingerprintService.fingerprint(
                        GROUP_ID,
                        request
                )
        ).thenReturn(
                FINGERPRINT
        );
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

    private SharedExpense sharedExpense(
            ExpenseGroup group,
            User paidBy,
            User createdBy,
            String title,
            String amount,
            String idempotencyKey,
            String requestFingerprint
    ) {

        return new SharedExpense(
                group,
                paidBy,
                createdBy,
                title,
                new BigDecimal(amount),
                SplitType.EQUAL,
                LocalDate.of(
                        2026,
                        9,
                        15
                ),
                idempotencyKey,
                requestFingerprint
        );
    }

    private ExpenseSplit expenseSplit(
            SharedExpense expense,
            User user,
            String amount
    ) {

        return new ExpenseSplit(
                expense,
                user,
                new BigDecimal(amount),
                null
        );
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