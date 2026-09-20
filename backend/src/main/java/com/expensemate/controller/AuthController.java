package com.expensemate.controller;

import com.expensemate.dto.LoginRequest;
import com.expensemate.dto.LoginResponse;
import com.expensemate.dto.RefreshTokenRequest;
import com.expensemate.dto.RefreshTokenResponse;
import com.expensemate.dto.RegisterRequest;
import com.expensemate.entity.RefreshToken;
import com.expensemate.entity.User;
import com.expensemate.security.JwtService;
import com.expensemate.security.RefreshTokenService;
import com.expensemate.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

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
            @Valid @RequestBody LoginRequest request
    ) {

        User user = authService.authenticate(request);

        String accessToken =
                jwtService.generateToken(user.getEmail());

        RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
                refreshTokenService.createToken(user);

        LoginResponse response = new LoginResponse(
                accessToken,
                issuedRefreshToken.rawToken(),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getName(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        RefreshTokenService.IssuedRefreshToken rotatedToken =
                refreshTokenService.rotateToken(
                        request.getRefreshToken()
                );

        RefreshToken refreshTokenEntity =
                rotatedToken.entity();

        User user =
                refreshTokenEntity.getUser();

        String accessToken =
                jwtService.generateToken(user.getEmail());

        RefreshTokenResponse response =
                new RefreshTokenResponse(
                        accessToken,
                        rotatedToken.rawToken(),
                        "Bearer",
                        jwtService.getExpirationSeconds()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        refreshTokenService.revokeToken(
                request.getRefreshToken()
        );

        return ResponseEntity.noContent().build();
    }
}