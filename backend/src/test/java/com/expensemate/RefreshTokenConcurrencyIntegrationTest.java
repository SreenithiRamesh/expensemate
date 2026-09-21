package com.expensemate;

import com.expensemate.entity.User;
import com.expensemate.exception.InvalidRefreshTokenException;
import com.expensemate.repository.RefreshTokenRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {

        createRequiredTables();

        refreshTokenRepository.deleteAll();

        userRepository
                .findByEmail("concurrency@example.com")
                .ifPresent(userRepository::delete);

        userRepository.flush();
    }

    @Test
    void concurrentRotationShouldAllowOnlyOneSuccessfulRotation()
            throws Exception {

        User user = new User(
                "Concurrency User",
                "concurrency@example.com",
                "encoded-password",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        user = userRepository.saveAndFlush(user);

        RefreshTokenService.IssuedRefreshToken original =
                refreshTokenService.createToken(user);

        String rawToken = original.rawToken();

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<RotationResult> first =
                    executor.submit(
                            () -> attemptRotation(
                                    rawToken,
                                    ready,
                                    start
                            )
                    );

            Future<RotationResult> second =
                    executor.submit(
                            () -> attemptRotation(
                                    rawToken,
                                    ready,
                                    start
                            )
                    );

            assertTrue(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    ),
                    "Both refresh requests did not become ready in time"
            );

            start.countDown();

            RotationResult firstResult =
                    first.get(
                            10,
                            TimeUnit.SECONDS
                    );

            RotationResult secondResult =
                    second.get(
                            10,
                            TimeUnit.SECONDS
                    );

            long successCount =
                    java.util.stream.Stream.of(
                                    firstResult,
                                    secondResult
                            )
                            .filter(RotationResult::success)
                            .count();

            long rejectedCount =
                    java.util.stream.Stream.of(
                                    firstResult,
                                    secondResult
                            )
                            .filter(result ->
                                    result.exception()
                                            instanceof InvalidRefreshTokenException
                            )
                            .count();

            assertEquals(
                    1,
                    successCount,
                    "Exactly one concurrent rotation should succeed"
            );

            assertEquals(
                    1,
                    rejectedCount,
                    "Exactly one concurrent rotation should be rejected"
            );

        } finally {

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    ),
                    "Executor did not terminate in time"
            );
        }
    }

    private RotationResult attemptRotation(
            String rawToken,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        ready.countDown();

        try {

            if (!start.await(
                    5,
                    TimeUnit.SECONDS
            )) {

                return new RotationResult(
                        false,
                        new IllegalStateException(
                                "Timed out waiting for concurrent start"
                        )
                );
            }

            refreshTokenService.rotateToken(
                    rawToken
            );

            return new RotationResult(
                    true,
                    null
            );

        } catch (Exception exception) {

            return new RotationResult(
                    false,
                    exception
            );
        }
    }

    private void createRequiredTables()
            throws Exception {

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            /*
             * Flyway and Hibernate schema generation are disabled
             * in the shared test profile, so this integration test
             * creates only the minimal tables required for the
             * refresh-token locking scenario.
             */

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(255) NOT NULL UNIQUE,
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

    private record RotationResult(
            boolean success,
            Exception exception
    ) {
    }
}