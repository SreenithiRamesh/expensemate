package com.expensemate.service;

import com.expensemate.dto.LoginRequest;
import com.expensemate.dto.RegisterRequest;
import com.expensemate.entity.User;
import com.expensemate.exception.EmailAlreadyExistsException;
import com.expensemate.exception.InvalidCredentialsException;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                loginAttemptService
        );
    }

    @Test
    void shouldRegisterUserWithNormalizedEmailAndEncodedPassword() {

        RegisterRequest request = new RegisterRequest();
        request.setName("  Sree  ");
        request.setEmail("  SREE@EXAMPLE.COM  ");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("sree@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("Password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        User result =
                authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User savedUser =
                userCaptor.getValue();

        assertEquals(
                "Sree",
                savedUser.getName()
        );

        assertEquals(
                "sree@example.com",
                savedUser.getEmail()
        );

        assertEquals(
                "encoded-password",
                savedUser.getPasswordHash()
        );

        assertNotNull(
                savedUser.getCreatedAt()
        );

        assertNotNull(
                savedUser.getUpdatedAt()
        );

        assertEquals(
                0,
                savedUser.getFailedLoginAttempts()
        );

        assertNull(
                savedUser.getLockedUntil()
        );

        assertSame(
                savedUser,
                result
        );

        verify(userRepository)
                .existsByEmail(
                        "sree@example.com"
                );

        verify(passwordEncoder)
                .encode("Password123");
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterRequest request =
                new RegisterRequest();

        request.setName("Sree");
        request.setEmail(
                "  SREE@EXAMPLE.COM "
        );
        request.setPassword(
                "Password123"
        );

        when(
                userRepository.existsByEmail(
                        "sree@example.com"
                )
        ).thenReturn(true);

        EmailAlreadyExistsException exception =
                assertThrows(
                        EmailAlreadyExistsException.class,
                        () ->
                                authService.register(
                                        request
                                )
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository)
                .existsByEmail(
                        "sree@example.com"
                );

        verify(
                userRepository,
                never()
        ).save(any(User.class));

        verify(
                passwordEncoder,
                never()
        ).encode(anyString());

        verifyNoInteractions(
                loginAttemptService
        );
    }

    @Test
    void shouldAuthenticateUserWithValidCredentials() {

        LoginRequest request =
                loginRequest(
                        "  SREE@EXAMPLE.COM ",
                        "Password123"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                passwordEncoder.matches(
                        "Password123",
                        "encoded-password"
                )
        ).thenReturn(true);

        User result =
                authService.authenticate(request);

        assertSame(
                user,
                result
        );

        verify(userRepository)
                .findByEmail(
                        "sree@example.com"
                );

        verify(passwordEncoder)
                .matches(
                        "Password123",
                        "encoded-password"
                );

        verifyNoInteractions(
                loginAttemptService
        );
    }

    @Test
    void shouldRejectAuthenticationWhenEmailDoesNotExist() {

        LoginRequest request =
                loginRequest(
                        "missing@example.com",
                        "Password123"
                );

        when(
                userRepository.findByEmail(
                        "missing@example.com"
                )
        ).thenReturn(
                Optional.empty()
        );

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () ->
                                authService.authenticate(
                                        request
                                )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(
                passwordEncoder,
                never()
        ).matches(
                anyString(),
                anyString()
        );

        /*
         * There is no known user ID against which an
         * account-level failed attempt can be recorded.
         */
        verifyNoInteractions(
                loginAttemptService
        );
    }

    @Test
    void shouldRecordFailedAttemptWhenPasswordIsIncorrect() {

        LoginRequest request =
                loginRequest(
                        "sree@example.com",
                        "WrongPassword"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                passwordEncoder.matches(
                        "WrongPassword",
                        "encoded-password"
                )
        ).thenReturn(false);

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () ->
                                authService.authenticate(
                                        request
                                )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(loginAttemptService)
                .recordFailedAttempt(1L);

        verify(
                loginAttemptService,
                never()
        ).resetFailedAttempts(
                anyLong()
        );
    }

    @Test
    void shouldRejectAuthenticationWhileAccountIsLocked() {

        LoginRequest request =
                loginRequest(
                        "sree@example.com",
                        "Password123"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        user.setFailedLoginAttempts(5);

        user.setLockedUntil(
                LocalDateTime.now()
                        .plusMinutes(10)
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () ->
                                authService.authenticate(
                                        request
                                )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        /*
         * Password verification must not happen while
         * the account is actively locked.
         */
        verify(
                passwordEncoder,
                never()
        ).matches(
                anyString(),
                anyString()
        );

        verifyNoInteractions(
                loginAttemptService
        );
    }

    @Test
    void shouldResetExpiredLockBeforeSuccessfulAuthentication() {

        LoginRequest request =
                loginRequest(
                        "sree@example.com",
                        "Password123"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        user.setFailedLoginAttempts(5);

        user.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                passwordEncoder.matches(
                        "Password123",
                        "encoded-password"
                )
        ).thenReturn(true);

        User result =
                authService.authenticate(request);

        assertSame(
                user,
                result
        );

        verify(loginAttemptService)
                .resetFailedAttempts(1L);

        verify(passwordEncoder)
                .matches(
                        "Password123",
                        "encoded-password"
                );

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );
    }

    @Test
    void shouldTreatWrongPasswordAfterExpiredLockAsFirstNewFailure() {

        LoginRequest request =
                loginRequest(
                        "sree@example.com",
                        "WrongPassword"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        user.setFailedLoginAttempts(5);

        user.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                passwordEncoder.matches(
                        "WrongPassword",
                        "encoded-password"
                )
        ).thenReturn(false);

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () ->
                                authService.authenticate(
                                        request
                                )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(loginAttemptService)
                .resetFailedAttempts(1L);

        verify(loginAttemptService)
                .recordFailedAttempt(1L);

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );
    }

    @Test
    void shouldResetPreviousFailuresAfterSuccessfulAuthentication() {

        LoginRequest request =
                loginRequest(
                        "sree@example.com",
                        "Password123"
                );

        User user =
                user(
                        1L,
                        "Sree",
                        "sree@example.com",
                        "encoded-password"
                );

        user.setFailedLoginAttempts(3);

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                passwordEncoder.matches(
                        "Password123",
                        "encoded-password"
                )
        ).thenReturn(true);

        User result =
                authService.authenticate(request);

        assertSame(
                user,
                result
        );

        verify(loginAttemptService)
                .resetFailedAttempts(1L);

        verify(
                loginAttemptService,
                never()
        ).recordFailedAttempt(
                anyLong()
        );

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertNull(
                user.getLockedUntil()
        );
    }

    private LoginRequest loginRequest(
            String email,
            String password
    ) {

        LoginRequest request =
                new LoginRequest();

        request.setEmail(email);
        request.setPassword(password);

        return request;
    }

    private User user(
            Long id,
            String name,
            String email,
            String passwordHash
    ) {

        User user =
                new User(
                        name,
                        email,
                        passwordHash,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );

        /*
         * User.id has no public setter because JPA owns it.
         * These AuthService unit tests need a stable ID because
         * LoginAttemptService works by user ID.
         */
        setUserId(user, id);

        return user;
    }

    private void setUserId(
            User user,
            Long id
    ) {

        try {

            java.lang.reflect.Field idField =
                    User.class
                            .getDeclaredField("id");

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