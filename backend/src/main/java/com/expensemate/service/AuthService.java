package com.expensemate.service;

import com.expensemate.dto.LoginRequest;
import com.expensemate.dto.RegisterRequest;
import com.expensemate.entity.User;
import com.expensemate.exception.EmailAlreadyExistsException;
import com.expensemate.exception.InvalidCredentialsException;
import com.expensemate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    public User register(RegisterRequest request) {

        String normalizedEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {

            throw new EmailAlreadyExistsException(
                    "Email already registered"
            );
        }

        User user = new User(
                request.getName().trim(),
                normalizedEmail,
                passwordEncoder.encode(
                        request.getPassword()
                ),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return userRepository.save(user);
    }

    public User authenticate(LoginRequest request) {

        String normalizedEmail =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(
                                this::invalidCredentials
                        );

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Reject authentication while the temporary
         * account lock is still active.
         *
         * We intentionally use the same generic authentication
         * error so the API does not reveal account state.
         */
        if (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(now)) {

            throw invalidCredentials();
        }

        /*
         * If a previous lock has expired, reset its old state
         * before evaluating the new password attempt.
         */
        if (user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(now)) {

            loginAttemptService.resetFailedAttempts(
                    user.getId()
            );

            /*
             * Keep the in-memory object consistent with the
             * state persisted by resetFailedAttempts().
             */
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {

            loginAttemptService.recordFailedAttempt(
                    user.getId()
            );

            throw invalidCredentials();
        }

        /*
         * A successful authentication clears any previous
         * failed-login state.
         */
        if (user.getFailedLoginAttempts() > 0
                || user.getLockedUntil() != null) {

            loginAttemptService.resetFailedAttempts(
                    user.getId()
            );

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        return user;
    }

    private InvalidCredentialsException invalidCredentials() {

        return new InvalidCredentialsException(
                INVALID_CREDENTIALS_MESSAGE
        );
    }
}