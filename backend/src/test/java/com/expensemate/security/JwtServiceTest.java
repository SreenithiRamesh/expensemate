package com.expensemate.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String EMAIL = "sree@example.com";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {

        /*
         * JwtService expects a Base64-encoded HMAC secret.
         * This is test-only data, not the application's real JWT secret.
         */
        String rawSecret =
                "expensemate-test-secret-key-12345678901234567890";

        String encodedSecret =
                Base64.getEncoder()
                        .encodeToString(
                                rawSecret.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        jwtService =
                new JwtService(
                        encodedSecret,
                        60_000L
                );
    }

    @Test
    void shouldGenerateTokenAndExtractEmail() {

        String token =
                jwtService.generateToken(EMAIL);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                EMAIL,
                jwtService.extractEmail(token)
        );
    }

    @Test
    void shouldValidateTokenForCorrectEmail() {

        String token =
                jwtService.generateToken(EMAIL);

        assertTrue(
                jwtService.isTokenValid(
                        token,
                        EMAIL
                )
        );
    }

    @Test
    void shouldRejectTokenForDifferentEmail() {

        String token =
                jwtService.generateToken(EMAIL);

        assertFalse(
                jwtService.isTokenValid(
                        token,
                        "another@example.com"
                )
        );
    }

    @Test
    void shouldReturnConfiguredExpirationInSeconds() {

        assertEquals(
                60L,
                jwtService.getExpirationSeconds()
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {

        String otherSecret =
                Base64.getEncoder()
                        .encodeToString(
                                "another-test-secret-key-12345678901234567890"
                                        .getBytes(StandardCharsets.UTF_8)
                        );

        JwtService otherJwtService =
                new JwtService(
                        otherSecret,
                        60_000L
                );

        String token =
                otherJwtService.generateToken(EMAIL);

        assertThrows(
                JwtException.class,
                () -> jwtService.extractEmail(token)
        );
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {

        String rawSecret =
                "expensemate-expired-test-key-12345678901234567890";

        String encodedSecret =
                Base64.getEncoder()
                        .encodeToString(
                                rawSecret.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        JwtService shortLivedJwtService =
                new JwtService(
                        encodedSecret,
                        1L
                );

        String token =
                shortLivedJwtService.generateToken(EMAIL);

        Thread.sleep(20L);

        assertThrows(
                JwtException.class,
                () ->
                        shortLivedJwtService.isTokenValid(
                                token,
                                EMAIL
                        )
        );
    }
}