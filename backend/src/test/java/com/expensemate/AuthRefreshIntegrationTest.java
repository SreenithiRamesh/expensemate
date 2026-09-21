package com.expensemate;

import com.expensemate.repository.RefreshTokenRepository;
import com.expensemate.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    }

    @Test
    void loginShouldReturnAccessAndRefreshTokens()
            throws Exception {

        registerUser();

        MvcResult result =
                loginUser();

        JsonNode response =
                readResponse(result);

        assertFalse(
                response.get("accessToken")
                        .asText()
                        .isBlank()
        );

        assertFalse(
                response.get("refreshToken")
                        .asText()
                        .isBlank()
        );

        assertEquals(
                "Bearer",
                response.get("tokenType")
                        .asText()
        );

        assertEquals(
                900,
                response.get("expiresIn")
                        .asLong()
        );

        assertEquals(
                "auth-test@example.com",
                response.get("email")
                        .asText()
        );

        assertEquals(
                "Auth Test User",
                response.get("name")
                        .asText()
        );
    }

    @Test
    void refreshShouldRotateRefreshToken()
            throws Exception {

        registerUser();

        JsonNode loginResponse =
                readResponse(
                        loginUser()
                );

        String oldRefreshToken =
                loginResponse
                        .get("refreshToken")
                        .asText();

        MvcResult refreshResult =
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "refreshToken": "%s"
                                                }
                                                """.formatted(
                                                        oldRefreshToken
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.accessToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.refreshToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.tokenType")
                                        .value("Bearer")
                        )
                        .andExpect(
                                jsonPath("$.expiresIn")
                                        .value(900)
                        )
                        .andReturn();

        JsonNode refreshResponse =
                readResponse(refreshResult);

        String newRefreshToken =
                refreshResponse
                        .get("refreshToken")
                        .asText();

        assertNotEquals(
                oldRefreshToken,
                newRefreshToken
        );
    }

    @Test
    void reusedRotatedTokenShouldBeRejected()
            throws Exception {

        registerUser();

        JsonNode loginResponse =
                readResponse(
                        loginUser()
                );

        String oldRefreshToken =
                loginResponse
                        .get("refreshToken")
                        .asText();

        refresh(oldRefreshToken)
                .andExpect(status().isOk());

        /*
         * The same old token has already been rotated.
         * Reusing it must now fail.
         */
        refresh(oldRefreshToken)
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Invalid or expired refresh token"
                                )
                );
    }

    @Test
    void reuseDetectionShouldRevokeReplacementToken()
            throws Exception {

        registerUser();

        JsonNode loginResponse =
                readResponse(
                        loginUser()
                );

        String oldRefreshToken =
                loginResponse
                        .get("refreshToken")
                        .asText();

        MvcResult firstRefresh =
                refresh(oldRefreshToken)
                        .andExpect(status().isOk())
                        .andReturn();

        String replacementToken =
                readResponse(firstRefresh)
                        .get("refreshToken")
                        .asText();

        /*
         * Reuse old token A.
         *
         * This should trigger reuse detection and revoke
         * its replacement B.
         */
        refresh(oldRefreshToken)
                .andExpect(status().isUnauthorized());

        /*
         * B must now also be unusable.
         */
        refresh(replacementToken)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldRevokeRefreshToken()
            throws Exception {

        registerUser();

        JsonNode loginResponse =
                readResponse(
                        loginUser()
                );

        String refreshToken =
                loginResponse
                        .get("refreshToken")
                        .asText();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "refreshToken": "%s"
                                        }
                                        """.formatted(
                                                refreshToken
                                        )
                                )
                )
                .andExpect(status().isNoContent());

        /*
         * A logged-out refresh token must no longer
         * be accepted by /refresh.
         */
        refresh(refreshToken)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldBeIdempotent()
            throws Exception {

        registerUser();

        JsonNode loginResponse =
                readResponse(
                        loginUser()
                );

        String refreshToken =
                loginResponse
                        .get("refreshToken")
                        .asText();

        logout(refreshToken)
                .andExpect(status().isNoContent());

        logout(refreshToken)
                .andExpect(status().isNoContent());
    }

    @Test
    void refreshShouldRejectUnknownToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "refreshToken":
                                            "this-token-does-not-exist"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Invalid or expired refresh token"
                                )
                );
    }

    @Test
    void refreshShouldRejectBlankToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "refreshToken": ""
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Validation failed")
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
                                          "name": "Auth Test User",
                                          "email": "auth-test@example.com",
                                          "password": "TestPassword123!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());
    }

    private MvcResult loginUser()
            throws Exception {

        return mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "TestPassword123!"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .isNotEmpty()
                )
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions refresh(
            String refreshToken
    ) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/refresh")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(
                                        refreshToken
                                )
                        )
        );
    }

    private org.springframework.test.web.servlet.ResultActions logout(
            String refreshToken
    ) throws Exception {

        return mockMvc.perform(
                post("/api/v1/auth/logout")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(
                                        refreshToken
                                )
                        )
        );
    }

    private JsonNode readResponse(
            MvcResult result
    ) throws Exception {

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString()
        );
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
