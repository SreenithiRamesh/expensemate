package com.expensemate.service;

import com.expensemate.entity.User;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    private LoginAttemptService loginAttemptService;

    private User user;

    @BeforeEach
    void setUp() {

        loginAttemptService =
                new LoginAttemptService(
                        userRepository
                );

        user = new User(
                "Sree",
                "sree@example.com",
                "encoded-password",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        setUserId(user, 1L);
    }

    @Test
    void firstFailedAttemptShouldSetCounterToOne() {

        stubUser();

        loginAttemptService.recordFailedAttempt(1L);

        assertEquals(
                1,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void fourFailedAttemptsShouldNotLockAccount() {

        stubUser();

        for (int attempt = 0; attempt < 4; attempt++) {
            loginAttemptService.recordFailedAttempt(1L);
        }

        assertEquals(
                4,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );

        verify(
                userRepository,
                times(4)
        ).save(user);
    }

    @Test
    void fifthFailedAttemptShouldLockAccountForApproximatelyFifteenMinutes() {

        user.setFailedLoginAttempts(4);

        stubUser();

        LocalDateTime before =
                LocalDateTime.now();

        loginAttemptService.recordFailedAttempt(1L);

        LocalDateTime after =
                LocalDateTime.now();

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertNotNull(
                user.getLockedUntil()
        );

        assertFalse(
                user.getLockedUntil()
                        .isBefore(
                                before.plusMinutes(15)
                        )
        );

        assertFalse(
                user.getLockedUntil()
                        .isAfter(
                                after.plusMinutes(15)
                        )
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void failedAttemptAfterExpiredLockShouldStartFreshCounter() {

        user.setFailedLoginAttempts(5);

        user.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );

        stubUser();

        loginAttemptService.recordFailedAttempt(1L);

        assertEquals(
                1,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void resetFailedAttemptsShouldClearCounterAndLock() {

        user.setFailedLoginAttempts(5);

        user.setLockedUntil(
                LocalDateTime.now()
                        .plusMinutes(10)
        );

        stubUser();

        loginAttemptService.resetFailedAttempts(1L);

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void resetShouldNotSaveWhenStateIsAlreadyClean() {

        stubUser();

        loginAttemptService.resetFailedAttempts(1L);

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );

        verify(
                userRepository,
                never()
        ).save(any(User.class));
    }

    @Test
    void recordFailedAttemptShouldDoNothingWhenUserDoesNotExist() {

        when(
                userRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        loginAttemptService.recordFailedAttempt(999L);

        verify(
                userRepository,
                never()
        ).save(any(User.class));
    }

    @Test
    void resetShouldDoNothingWhenUserDoesNotExist() {

        when(
                userRepository.findById(999L)
        ).thenReturn(
                Optional.empty()
        );

        loginAttemptService.resetFailedAttempts(999L);

        verify(
                userRepository,
                never()
        ).save(any(User.class));
    }

    private void stubUser() {

        when(
                userRepository.findById(1L)
        ).thenReturn(
                Optional.of(user)
        );
    }

    private void setUserId(
            User user,
            Long id
    ) {

        try {

            java.lang.reflect.Field idField =
                    User.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(user, id);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException(
                    "Unable to set test user ID",
                    exception
            );
        }
    }
}