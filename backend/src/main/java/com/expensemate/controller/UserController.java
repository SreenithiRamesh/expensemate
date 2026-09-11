package com.expensemate.controller;

import com.expensemate.entity.User;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication
    ) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return ResponseEntity.ok(
                Map.of(
                        "userId", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()
                )
        );
    }
}