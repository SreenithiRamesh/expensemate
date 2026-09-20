package com.expensemate.security;

import com.expensemate.entity.RefreshToken;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRefreshTokenException;
import com.expensemate.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationDays;
    private final SecureRandom secureRandom;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${auth.refresh-token.expiration-days}") long expirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationDays = expirationDays;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public IssuedRefreshToken createToken(User user) {

        LocalDateTime now = LocalDateTime.now();

        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                now.plusDays(expirationDays),
                now
        );

        RefreshToken savedToken =
                refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(
                rawToken,
                savedToken
        );
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedRefreshToken rotateToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        /*
         * The refresh-token row is locked until this transaction
         * completes. This prevents two concurrent refresh requests
         * from successfully rotating the same token at the same time.
         */
        RefreshToken currentToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(this::invalidRefreshToken);

        LocalDateTime now = LocalDateTime.now();

        /*
         * A revoked token that already has a replacement indicates
         * that an old rotated token has been presented again.
         *
         * Revoke its descendant token chain so that an attacker
         * cannot continue using a replacement created from the
         * compromised token family.
         */
        if (currentToken.isRevoked()) {

            if (currentToken.getReplacedByToken() != null) {
                revokeTokenFamily(
                        currentToken.getReplacedByToken(),
                        now
                );
            }

            throw invalidRefreshToken();
        }

        if (currentToken.isExpired(now)) {

            currentToken.setRevokedAt(now);

            refreshTokenRepository.save(currentToken);

            throw invalidRefreshToken();
        }

        /*
         * Generate the replacement while the current token remains
         * locked inside this transaction.
         */
        IssuedRefreshToken replacement =
                createToken(currentToken.getUser());

        currentToken.setRevokedAt(now);
        currentToken.setReplacedByToken(
                replacement.entity()
        );

        refreshTokenRepository.save(currentToken);

        return replacement;
    }

    @Transactional
    public void revokeToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(token -> {

                    if (!token.isRevoked()) {

                        token.setRevokedAt(
                                LocalDateTime.now()
                        );

                        refreshTokenRepository.save(token);
                    }
                });
    }

    public String hashToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token must not be blank"
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String generateRawToken() {

        byte[] tokenBytes =
                new byte[TOKEN_BYTES];

        secureRandom.nextBytes(tokenBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private void revokeTokenFamily(
            RefreshToken token,
            LocalDateTime now
    ) {

        RefreshToken current = token;

        while (current != null) {

            if (!current.isRevoked()) {

                current.setRevokedAt(now);

                refreshTokenRepository.save(current);
            }

            current =
                    current.getReplacedByToken();
        }
    }

    private InvalidRefreshTokenException invalidRefreshToken() {

        return new InvalidRefreshTokenException(
                "Invalid or expired refresh token"
        );
    }

    public record IssuedRefreshToken(
            String rawToken,
            RefreshToken entity
    ) {
    }
}