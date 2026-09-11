package com.expensemate.controller;

import com.expensemate.dto.LoginRequest;
import com.expensemate.dto.LoginResponse;
import com.expensemate.dto.RegisterRequest;
import com.expensemate.entity.User;
import com.expensemate.security.JwtService;
import com.expensemate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService,
                          JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "User registered successfully",
                        "userId", user.getId(),
                        "name", user.getName(),
                        "email", user.getEmail()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        User user = authService.authenticate(request);

        String accessToken = jwtService.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}