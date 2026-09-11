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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {

        String normalizedEmail = request.getEmail()
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
                passwordEncoder.encode(request.getPassword()),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        return userRepository.save(user);
    }

    public User authenticate(LoginRequest request) {

        String normalizedEmail = request.getEmail()
                .trim()
                .toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        return user;
    }
}