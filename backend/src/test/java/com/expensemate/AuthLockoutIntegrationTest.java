package com.expensemate;

import com.expensemate.entity.User;
import com.expensemate.repository.RefreshTokenRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {

        createRequiredTables();

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        registerUser();
    }

    @Test
    void fiveFailedLoginsShouldLockAccount()
            throws Exception {

        for (int attempt = 1; attempt <= 5; attempt++) {

            wrongPasswordLogin()
                    .andExpect(status().isUnauthorized())
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "Invalid email or password"
                                    )
                    );
        }

        User user =
                userRepository
                        .findByEmail(
                                "lockout-test@example.com"
                        )
                        .orElseThrow();

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertNotNull(
                user.getLockedUntil()
        );

        assertTrue(
                user.getLockedUntil()
                        .isAfter(LocalDateTime.now())
        );
    }

    @Test
    void correctPasswordShouldStillBeRejectedWhileAccountIsLocked()
            throws Exception {

        lockAccount();

        correctPasswordLogin()
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                );

        User user =
                getTestUser();

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertNotNull(
                user.getLockedUntil()
        );
    }

    @Test
    void failedAttemptDuringActiveLockShouldNotIncreaseCounter()
            throws Exception {

        lockAccount();

        wrongPasswordLogin()
                .andExpect(status().isUnauthorized());

        User user =
                getTestUser();

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertNotNull(
                user.getLockedUntil()
        );
    }

    @Test
    void successfulLoginBeforeFifthFailureShouldResetCounter()
            throws Exception {

        for (int attempt = 0; attempt < 3; attempt++) {

            wrongPasswordLogin()
                    .andExpect(status().isUnauthorized());
        }

        User afterFailures =
                getTestUser();

        assertEquals(
                3,
                afterFailures.getFailedLoginAttempts()
        );

        assertNull(
                afterFailures.getLockedUntil()
        );

        correctPasswordLogin()
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .isNotEmpty()
                );

        User afterSuccess =
                getTestUser();

        assertEquals(
                0,
                afterSuccess.getFailedLoginAttempts()
        );

        assertNull(
                afterSuccess.getLockedUntil()
        );
    }

    @Test
    void expiredLockShouldAllowSuccessfulLoginAndResetState()
            throws Exception {

        lockAccount();

        User lockedUser =
                getTestUser();

        lockedUser.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );

        userRepository.saveAndFlush(
                lockedUser
        );

        correctPasswordLogin()
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .isNotEmpty()
                );

        User afterLogin =
                getTestUser();

        assertEquals(
                0,
                afterLogin.getFailedLoginAttempts()
        );

        assertNull(
                afterLogin.getLockedUntil()
        );
    }

    @Test
    void wrongPasswordAfterExpiredLockShouldStartFreshCounter()
            throws Exception {

        lockAccount();

        User lockedUser =
                getTestUser();

        lockedUser.setLockedUntil(
                LocalDateTime.now()
                        .minusMinutes(1)
        );

        userRepository.saveAndFlush(
                lockedUser
        );

        wrongPasswordLogin()
                .andExpect(status().isUnauthorized());

        User afterFailure =
                getTestUser();

        assertEquals(
                1,
                afterFailure.getFailedLoginAttempts()
        );

        assertNull(
                afterFailure.getLockedUntil()
        );
    }

    @Test
    void unknownEmailShouldReturnSameGenericAuthenticationError()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "unknown@example.com",
                                          "password": "WrongPassword123!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                );
    }

    private void lockAccount()
            throws Exception {

        for (int attempt = 0; attempt < 5; attempt++) {

            wrongPasswordLogin()
                    .andExpect(status().isUnauthorized());
        }

        User user =
                getTestUser();

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertNotNull(
                user.getLockedUntil()
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    wrongPasswordLogin() throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "email": "lockout-test@example.com",
                                  "password": "WrongPassword123!"
                                }
                                """
                        )
        );
    }

    private org.springframework.test.web.servlet.ResultActions
    correctPasswordLogin() throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "email": "lockout-test@example.com",
                                  "password": "CorrectPassword123!"
                                }
                                """
                        )
        );
    }

    private void registerUser()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Lockout Test User",
                                          "email": "lockout-test@example.com",
                                          "password": "CorrectPassword123!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());
    }

    private User getTestUser() {

        return userRepository
                .findByEmail(
                        "lockout-test@example.com"
                )
                .orElseThrow();
    }

    private void createRequiredTables()
            throws Exception {

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                        locked_until TIMESTAMP NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS refresh_tokens (
                        id BIGSERIAL PRIMARY KEY,
                        user_id BIGINT NOT NULL,
                        token_hash VARCHAR(64) NOT NULL UNIQUE,
                        expires_at TIMESTAMP NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        revoked_at TIMESTAMP NULL,
                        replaced_by_token_id BIGINT NULL,

                        CONSTRAINT fk_refresh_tokens_user
                            FOREIGN KEY (user_id)
                            REFERENCES users(id)
                            ON DELETE CASCADE,

                        CONSTRAINT fk_refresh_tokens_replaced_by
                            FOREIGN KEY (replaced_by_token_id)
                            REFERENCES refresh_tokens(id)
                            ON DELETE SET NULL
                    )
                    """);
        }
    }
}