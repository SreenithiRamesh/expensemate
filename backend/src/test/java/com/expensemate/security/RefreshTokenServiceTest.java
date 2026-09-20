package com.expensemate.security;

import com.expensemate.entity.RefreshToken;
import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRefreshTokenException;
import com.expensemate.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {

        refreshTokenService =
                new RefreshTokenService(
                        refreshTokenRepository,
                        7
                );

        user = new User(
                "Sree",
                "sree@example.com",
                "encoded-password",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createTokenShouldReturnRawTokenAndStoreOnlyHash() {

        stubSave();

        RefreshTokenService.IssuedRefreshToken issued =
                refreshTokenService.createToken(user);

        assertNotNull(issued.rawToken());
        assertFalse(issued.rawToken().isBlank());

        RefreshToken stored = issued.entity();

        assertNotNull(stored);
        assertNotNull(stored.getTokenHash());

        assertNotEquals(
                issued.rawToken(),
                stored.getTokenHash()
        );

        assertEquals(
                refreshTokenService.hashToken(
                        issued.rawToken()
                ),
                stored.getTokenHash()
        );

        assertEquals(
                64,
                stored.getTokenHash().length()
        );

        assertSame(
                user,
                stored.getUser()
        );

        assertNotNull(stored.getCreatedAt());
        assertNotNull(stored.getExpiresAt());

        assertTrue(
                stored.getExpiresAt()
                        .isAfter(stored.getCreatedAt())
        );

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    @Test
    void createTokenShouldCreateDifferentTokens() {

        stubSave();

        RefreshTokenService.IssuedRefreshToken first =
                refreshTokenService.createToken(user);

        RefreshTokenService.IssuedRefreshToken second =
                refreshTokenService.createToken(user);

        assertNotEquals(
                first.rawToken(),
                second.rawToken()
        );

        assertNotEquals(
                first.entity().getTokenHash(),
                second.entity().getTokenHash()
        );

        verify(
                refreshTokenRepository,
                times(2)
        ).save(any(RefreshToken.class));
    }

    @Test
    void hashTokenShouldBeDeterministic() {

        String rawToken =
                "test-refresh-token";

        String first =
                refreshTokenService.hashToken(rawToken);

        String second =
                refreshTokenService.hashToken(rawToken);

        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void hashTokenShouldRejectBlankToken() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        refreshTokenService.hashToken(" ")
        );

        verifyNoInteractions(
                refreshTokenRepository
        );
    }

    @Test
    void rotateTokenShouldRevokeOldTokenAndIssueReplacement() {

        stubSave();

        String rawToken =
                "valid-refresh-token";

        String tokenHash =
                refreshTokenService.hashToken(rawToken);

        RefreshToken current =
                new RefreshToken(
                        user,
                        tokenHash,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                );

        when(
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
        ).thenReturn(
                Optional.of(current)
        );

        RefreshTokenService.IssuedRefreshToken replacement =
                refreshTokenService.rotateToken(rawToken);

        assertNotNull(replacement);
        assertNotNull(replacement.rawToken());
        assertFalse(
                replacement.rawToken().isBlank()
        );

        assertTrue(current.isRevoked());

        assertSame(
                replacement.entity(),
                current.getReplacedByToken()
        );

        assertNotEquals(
                current.getTokenHash(),
                replacement.entity().getTokenHash()
        );

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(
                        tokenHash
                );

        verify(
                refreshTokenRepository,
                times(2)
        ).save(any(RefreshToken.class));
    }

    @Test
    void rotateTokenShouldRejectUnknownToken() {

        String rawToken =
                "unknown-refresh-token";

        String tokenHash =
                refreshTokenService.hashToken(rawToken);

        when(
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
        ).thenReturn(
                Optional.empty()
        );

        InvalidRefreshTokenException exception =
                assertThrows(
                        InvalidRefreshTokenException.class,
                        () ->
                                refreshTokenService
                                        .rotateToken(rawToken)
                );

        assertEquals(
                "Invalid or expired refresh token",
                exception.getMessage()
        );

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(
                        tokenHash
                );

        verify(
                refreshTokenRepository,
                never()
        ).save(any(RefreshToken.class));
    }

    @Test
    void rotateTokenShouldRejectExpiredTokenAndRevokeIt() {

        stubSave();

        String rawToken =
                "expired-refresh-token";

        String tokenHash =
                refreshTokenService.hashToken(rawToken);

        RefreshToken expired =
                new RefreshToken(
                        user,
                        tokenHash,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().minusDays(8)
                );

        when(
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                tokenHash
                        )
        ).thenReturn(
                Optional.of(expired)
        );

        assertThrows(
                InvalidRefreshTokenException.class,
                () ->
                        refreshTokenService
                                .rotateToken(rawToken)
        );

        assertTrue(expired.isRevoked());
        assertNotNull(
                expired.getRevokedAt()
        );

        verify(refreshTokenRepository)
                .save(expired);
    }

    @Test
    void revokeTokenShouldRevokeActiveToken() {

        stubSave();

        String rawToken =
                "logout-refresh-token";

        String tokenHash =
                refreshTokenService.hashToken(rawToken);

        RefreshToken token =
                new RefreshToken(
                        user,
                        tokenHash,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                );

        when(
                refreshTokenRepository
                        .findByTokenHash(
                                tokenHash
                        )
        ).thenReturn(
                Optional.of(token)
        );

        refreshTokenService.revokeToken(
                rawToken
        );

        assertTrue(token.isRevoked());
        assertNotNull(
                token.getRevokedAt()
        );

        verify(refreshTokenRepository)
                .findByTokenHash(tokenHash);

        verify(refreshTokenRepository)
                .save(token);
    }

    @Test
    void revokeTokenShouldBeIdempotentForAlreadyRevokedToken() {

        String rawToken =
                "already-revoked-token";

        String tokenHash =
                refreshTokenService.hashToken(rawToken);

        RefreshToken token =
                new RefreshToken(
                        user,
                        tokenHash,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                );

        token.setRevokedAt(
                LocalDateTime.now().minusMinutes(1)
        );

        LocalDateTime originalRevokedAt =
                token.getRevokedAt();

        when(
                refreshTokenRepository
                        .findByTokenHash(
                                tokenHash
                        )
        ).thenReturn(
                Optional.of(token)
        );

        refreshTokenService.revokeToken(
                rawToken
        );

        assertTrue(token.isRevoked());

        assertEquals(
                originalRevokedAt,
                token.getRevokedAt()
        );

        verify(refreshTokenRepository)
                .findByTokenHash(tokenHash);

        verify(
                refreshTokenRepository,
                never()
        ).save(any(RefreshToken.class));
    }

    @Test
    void reuseOfRotatedTokenShouldRevokeReplacement() {

        stubSave();

        String oldRawToken =
                "old-refresh-token";

        String oldTokenHash =
                refreshTokenService.hashToken(
                        oldRawToken
                );

        RefreshToken oldToken =
                new RefreshToken(
                        user,
                        oldTokenHash,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                );

        RefreshToken replacement =
                new RefreshToken(
                        user,
                        refreshTokenService.hashToken(
                                "replacement-refresh-token"
                        ),
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now()
                );

        oldToken.setRevokedAt(
                LocalDateTime.now().minusMinutes(1)
        );

        oldToken.setReplacedByToken(
                replacement
        );

        when(
                refreshTokenRepository
                        .findByTokenHashForUpdate(
                                oldTokenHash
                        )
        ).thenReturn(
                Optional.of(oldToken)
        );

        assertThrows(
                InvalidRefreshTokenException.class,
                () ->
                        refreshTokenService
                                .rotateToken(
                                        oldRawToken
                                )
        );

        assertTrue(oldToken.isRevoked());

        assertTrue(
                replacement.isRevoked()
        );

        assertNotNull(
                replacement.getRevokedAt()
        );

        verify(refreshTokenRepository)
                .findByTokenHashForUpdate(
                        oldTokenHash
                );

        verify(refreshTokenRepository)
                .save(replacement);
    }

    private void stubSave() {

        when(
                refreshTokenRepository.save(
                        any(RefreshToken.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );
    }
}