package com.expensemate.service;

import com.expensemate.entity.User;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginAttemptService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long userId) {

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * If an old lock has already expired, start a fresh
         * failed-attempt window.
         */
        if (user.getLockedUntil() != null
                && !user.getLockedUntil().isAfter(now)) {

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        int newFailedAttempts =
                user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(
                newFailedAttempts
        );

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {

            user.setLockedUntil(
                    now.plusMinutes(
                            LOCK_DURATION_MINUTES
                    )
            );
        }

        userRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(Long userId) {

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return;
        }

        if (user.getFailedLoginAttempts() != 0
                || user.getLockedUntil() != null) {

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);

            userRepository.save(user);
        }
    }
}