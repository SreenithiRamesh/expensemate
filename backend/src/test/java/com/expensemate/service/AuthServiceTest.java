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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder
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
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

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

        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());

        assertSame(savedUser, result);

        verify(userRepository)
                .existsByEmail("sree@example.com");

        verify(passwordEncoder)
                .encode("Password123");
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Sree");
        request.setEmail("  SREE@EXAMPLE.COM ");
        request.setPassword("Password123");

        when(userRepository.existsByEmail("sree@example.com"))
                .thenReturn(true);

        EmailAlreadyExistsException exception =
                assertThrows(
                        EmailAlreadyExistsException.class,
                        () -> authService.register(request)
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository)
                .existsByEmail("sree@example.com");

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());
    }

    @Test
    void shouldAuthenticateUserWithValidCredentials() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  SREE@EXAMPLE.COM ");
        request.setPassword("Password123");

        User user = user(
                "Sree",
                "sree@example.com",
                "encoded-password"
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password123",
                "encoded-password"
        )).thenReturn(true);

        User result = authService.authenticate(request);

        assertSame(user, result);

        verify(userRepository)
                .findByEmail("sree@example.com");

        verify(passwordEncoder)
                .matches(
                        "Password123",
                        "encoded-password"
                );
    }

    @Test
    void shouldRejectAuthenticationWhenEmailDoesNotExist() {

        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("Password123");

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.authenticate(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());
    }

    @Test
    void shouldRejectAuthenticationWhenPasswordIsIncorrect() {

        LoginRequest request = new LoginRequest();
        request.setEmail("sree@example.com");
        request.setPassword("WrongPassword");

        User user = user(
                "Sree",
                "sree@example.com",
                "encoded-password"
        );

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "encoded-password"
        )).thenReturn(false);

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.authenticate(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );
    }

    private User user(
            String name,
            String email,
            String passwordHash
    ) {
        return new User(
                name,
                email,
                passwordHash,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}