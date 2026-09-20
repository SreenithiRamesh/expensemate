package com.expensemate;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SharedExpenseResponse;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.enums.SplitType;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.ExpenseSplitRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SharedExpenseRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.SharedExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SharedExpenseIdempotencyConcurrencyIntegrationTest {

    private static final String CREATOR_EMAIL =
            "m25-creator@example.com";

    private static final String IDEMPOTENCY_KEY =
            "m25-shared-expense-concurrency-key";

    @Autowired
    private SharedExpenseService sharedExpenseService;

    @Autowired
    private SharedExpenseRepository sharedExpenseRepository;

    @Autowired
    private ExpenseSplitRepository expenseSplitRepository;

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    private User creator;
    private User secondMember;
    private ExpenseGroup group;

    @BeforeEach
    void setUp() throws Exception {

        recreateRequiredSchema();

        LocalDateTime now =
                LocalDateTime.now();

        creator =
                userRepository.saveAndFlush(
                        new User(
                                "M25 Creator",
                                CREATOR_EMAIL,
                                "encoded-password",
                                now,
                                now
                        )
                );

        secondMember =
                userRepository.saveAndFlush(
                        new User(
                                "M25 Member",
                                "m25-member@example.com",
                                "encoded-password",
                                now,
                                now
                        )
                );

        group =
                expenseGroupRepository.saveAndFlush(
                        new ExpenseGroup(
                                "M25 Concurrency Group",
                                "Concurrency integration testing",
                                creator
                        )
                );

        groupMemberRepository.saveAndFlush(
                new GroupMember(
                        group,
                        creator
                )
        );

        groupMemberRepository.saveAndFlush(
                new GroupMember(
                        group,
                        secondMember
                )
        );
    }

    @Test
    void concurrentIdenticalRequestsShouldCreateExactlyOneExpense()
            throws Exception {

        SharedExpenseCreateRequest request =
                createEqualSplitRequest();

        CountDownLatch ready =
                new CountDownLatch(2);

        CountDownLatch start =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {

            Future<CreationResult> first =
                    executor.submit(
                            () ->
                                    attemptCreation(
                                            request,
                                            ready,
                                            start
                                    )
                    );

            Future<CreationResult> second =
                    executor.submit(
                            () ->
                                    attemptCreation(
                                            request,
                                            ready,
                                            start
                                    )
                    );

            assertTrue(
                    ready.await(
                            5,
                            TimeUnit.SECONDS
                    ),
                    "Both shared-expense requests did not become ready in time"
            );

            /*
             * Release both requests at approximately the same time.
             */
            start.countDown();

            CreationResult firstResult =
                    first.get(
                            15,
                            TimeUnit.SECONDS
                    );

            CreationResult secondResult =
                    second.get(
                            15,
                            TimeUnit.SECONDS
                    );

            long successCount =
                    Stream.of(
                                    firstResult,
                                    secondResult
                            )
                            .filter(
                                    CreationResult::success
                            )
                            .count();

            assertEquals(
                    2,
                    successCount,
                    () ->
                            "Both requests should return the same logical "
                                    + "expense. First failure="
                                    + describeFailure(
                                    firstResult
                            )
                                    + ", second failure="
                                    + describeFailure(
                                    secondResult
                            )
            );

            SharedExpenseResponse firstResponse =
                    firstResult.response();

            SharedExpenseResponse secondResponse =
                    secondResult.response();

            assertNotNull(
                    firstResponse
            );

            assertNotNull(
                    secondResponse
            );

            assertNotNull(
                    firstResponse.getId()
            );

            assertNotNull(
                    secondResponse.getId()
            );

            assertEquals(
                    firstResponse.getId(),
                    secondResponse.getId(),
                    "Concurrent duplicate requests must replay "
                            + "the same expense"
            );

            List<SharedExpense> storedExpenses =
                    sharedExpenseRepository
                            .findByGroup_Id(
                                    group.getId()
                            );

            assertEquals(
                    1,
                    storedExpenses.size(),
                    "Exactly one shared expense must be stored"
            );

            SharedExpense storedExpense =
                    storedExpenses.getFirst();

            assertEquals(
                    IDEMPOTENCY_KEY,
                    storedExpense.getIdempotencyKey()
            );

            assertEquals(
                    creator.getId(),
                    storedExpense
                            .getCreatedBy()
                            .getId()
            );

            assertMoney(
                    "100.00",
                    storedExpense.getAmount()
            );

            assertTrue(
                    sharedExpenseRepository
                            .findByCreatedBy_IdAndIdempotencyKey(
                                    creator.getId(),
                                    IDEMPOTENCY_KEY
                            )
                            .isPresent(),
                    "Expense should be retrievable by creator "
                            + "and idempotency key"
            );

            assertEquals(
                    2,
                    expenseSplitRepository
                            .findByExpenseId(
                                    storedExpense.getId()
                            )
                            .size(),
                    "The winning expense must contain "
                            + "exactly two splits"
            );

            assertEquals(
                    2,
                    expenseSplitRepository
                            .findByExpense_Group_Id(
                                    group.getId()
                            )
                            .size(),
                    "The losing transaction must not leave "
                            + "duplicate splits"
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

    private CreationResult attemptCreation(
            SharedExpenseCreateRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) {

        ready.countDown();

        try {

            if (!start.await(
                    5,
                    TimeUnit.SECONDS
            )) {

                return new CreationResult(
                        false,
                        null,
                        new IllegalStateException(
                                "Timed out waiting for concurrent start"
                        )
                );
            }

            SharedExpenseResponse response =
                    sharedExpenseService.createSharedExpense(
                            group.getId(),
                            CREATOR_EMAIL,
                            IDEMPOTENCY_KEY,
                            request
                    );

            return new CreationResult(
                    true,
                    response,
                    null
            );

        } catch (Exception exception) {

            return new CreationResult(
                    false,
                    null,
                    exception
            );
        }
    }

    private SharedExpenseCreateRequest createEqualSplitRequest() {

        SplitInputRequest creatorSplit =
                new SplitInputRequest();

        creatorSplit.setUserId(
                creator.getId()
        );

        creatorSplit.setValue(
                null
        );

        SplitInputRequest memberSplit =
                new SplitInputRequest();

        memberSplit.setUserId(
                secondMember.getId()
        );

        memberSplit.setValue(
                null
        );

        SharedExpenseCreateRequest request =
                new SharedExpenseCreateRequest();

        request.setTitle(
                "M25 Concurrent Dinner"
        );

        request.setAmount(
                new BigDecimal(
                        "100.00"
                )
        );

        request.setPaidByUserId(
                creator.getId()
        );

        request.setSplitType(
                SplitType.EQUAL
        );

        request.setExpenseDate(
                LocalDate.of(
                        2026,
                        9,
                        21
                )
        );

        request.setSplits(
                List.of(
                        creatorSplit,
                        memberSplit
                )
        );

        return request;
    }

    private String describeFailure(
            CreationResult result
    ) {

        if (result.exception() == null) {
            return "none";
        }

        return result.exception()
                .getClass()
                .getSimpleName()
                + ": "
                + result.exception()
                .getMessage();
    }

    private void recreateRequiredSchema()
            throws Exception {

        dropRequiredTables();

        /*
         * Flyway and Hibernate schema creation are disabled
         * in application-test.properties.
         *
         * Run the production migrations that H2 supports,
         * then apply the V12-equivalent schema using separate
         * H2-compatible ALTER TABLE statements.
         */
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator();

        populator.addScript(
                new ClassPathResource(
                        "db/migration/V1__create_users_table.sql"
                )
        );

        populator.addScript(
                new ClassPathResource(
                        "db/migration/V5__create_expense_groups_and_members_tables.sql"
                )
        );

        populator.addScript(
                new ClassPathResource(
                        "db/migration/V6__create_shared_expenses_and_splits_tables.sql"
                )
        );

        populator.addScript(
                new ClassPathResource(
                        "db/migration/V8__create_group_activity.sql"
                )
        );

        populator.addScript(
                new ClassPathResource(
                        "db/migration/V11__harden_authentication.sql"
                )
        );

        populator.execute(
                dataSource
        );

        applySharedExpenseIdempotencySchemaForH2();
    }

    private void applySharedExpenseIdempotencySchemaForH2()
            throws Exception {

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            /*
             * Production migration V12 uses PostgreSQL syntax
             * that adds several columns in one ALTER TABLE.
             *
             * H2 requires individual statements.
             */

            statement.execute("""
                    ALTER TABLE shared_expenses
                    ADD COLUMN created_by BIGINT
                    """);

            statement.execute("""
                    ALTER TABLE shared_expenses
                    ADD COLUMN idempotency_key VARCHAR(100)
                    """);

            statement.execute("""
                    ALTER TABLE shared_expenses
                    ADD COLUMN request_fingerprint VARCHAR(64)
                    """);

            statement.execute("""
                    ALTER TABLE shared_expenses
                    ADD CONSTRAINT fk_shared_expenses_created_by
                    FOREIGN KEY (created_by)
                    REFERENCES users(id)
                    """);

            statement.execute("""
                    ALTER TABLE shared_expenses
                    ADD CONSTRAINT uk_shared_expense_user_idempotency
                    UNIQUE (created_by, idempotency_key)
                    """);

            statement.execute("""
                    CREATE INDEX idx_shared_expenses_created_by
                    ON shared_expenses(created_by)
                    """);
        }
    }

    private void dropRequiredTables()
            throws Exception {

        try (
                Connection connection =
                        dataSource.getConnection();

                Statement statement =
                        connection.createStatement()
        ) {

            /*
             * These integration tests share the same H2 database.
             *
             * Drop all possible child tables created by either
             * the shared-expense concurrency test or settlement
             * concurrency test before dropping their parent tables.
             *
             * This makes the test independent of Maven's test order.
             */

            statement.execute(
                    "DROP TABLE IF EXISTS group_activity"
            );

            /*
             * A settlement references expense_groups and users.
             * It must be removed before either parent table.
             */
            statement.execute(
                    "DROP TABLE IF EXISTS settlements"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS expense_splits"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS shared_expenses"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS group_members"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS expense_groups"
            );

            /*
             * V11 creates refresh_tokens with a foreign key
             * referencing users.
             */
            statement.execute(
                    "DROP TABLE IF EXISTS refresh_tokens"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS users"
            );
        }
    }

    private void assertMoney(
            String expected,
            BigDecimal actual
    ) {

        assertNotNull(
                actual
        );

        assertEquals(
                0,
                new BigDecimal(
                        expected
                ).compareTo(
                        actual
                )
        );
    }

    private record CreationResult(
            boolean success,
            SharedExpenseResponse response,
            Exception exception
    ) {
    }
}