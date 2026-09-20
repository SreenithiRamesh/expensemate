package com.expensemate;

import com.expensemate.dto.debt.DebtSettlementSuggestion;
import com.expensemate.dto.debt.DebtSimplificationResponse;
import com.expensemate.dto.settlement.SettlementCreateRequest;
import com.expensemate.dto.settlement.SettlementResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.Settlement;
import com.expensemate.entity.User;
import com.expensemate.enums.SettlementMode;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.SettlementRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.DebtSimplificationService;
import com.expensemate.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class SettlementIdempotencyConcurrencyIntegrationTest {

    private static final String DEBTOR_EMAIL =
            "m25-settlement-debtor@example.com";

    private static final String IDEMPOTENCY_KEY =
            "m25-settlement-concurrency-key";

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * The debt calculation is mocked because this test is focused
     * specifically on settlement idempotency and database concurrency.
     *
     * Both concurrent requests must observe the same outstanding debt
     * so they reach the unique-key race deterministically.
     */
    @MockitoBean
    private DebtSimplificationService debtSimplificationService;

    private User debtor;
    private User creditor;
    private ExpenseGroup group;

    @BeforeEach
    void setUp() throws Exception {

        recreateRequiredSchema();

        LocalDateTime now =
                LocalDateTime.now();

        debtor =
                userRepository.saveAndFlush(
                        new User(
                                "M25 Debtor",
                                DEBTOR_EMAIL,
                                "encoded-password",
                                now,
                                now
                        )
                );

        creditor =
                userRepository.saveAndFlush(
                        new User(
                                "M25 Creditor",
                                "m25-settlement-creditor@example.com",
                                "encoded-password",
                                now,
                                now
                        )
                );

        group =
                expenseGroupRepository.saveAndFlush(
                        new ExpenseGroup(
                                "M25 Settlement Concurrency Group",
                                "Settlement concurrency integration testing",
                                debtor
                        )
                );

        groupMemberRepository.saveAndFlush(
                new GroupMember(
                        group,
                        debtor
                )
        );

        groupMemberRepository.saveAndFlush(
                new GroupMember(
                        group,
                        creditor
                )
        );

        when(
                debtSimplificationService
                        .simplifyGroupDebt(
                                group.getId(),
                                DEBTOR_EMAIL
                        )
        ).thenReturn(
                new DebtSimplificationResponse(
                        group.getId(),
                        List.of(
                                new DebtSettlementSuggestion(
                                        debtor.getId(),
                                        debtor.getName(),
                                        creditor.getId(),
                                        creditor.getName(),
                                        new BigDecimal(
                                                "800.00"
                                        )
                                )
                        ),
                        List.of(),
                        List.of()
                )
        );
    }

    @Test
    void concurrentIdenticalRequestsShouldCreateExactlyOneSettlement()
            throws Exception {

        SettlementCreateRequest request =
                new SettlementCreateRequest(
                        creditor.getId(),
                        SettlementMode.PARTIAL,
                        new BigDecimal(
                                "300.00"
                        )
                );

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
                    "Both settlement requests did not become ready in time"
            );

            /*
             * Release both threads as close together as possible.
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
                                    + "settlement. First failure="
                                    + describeFailure(
                                    firstResult
                            )
                                    + ", second failure="
                                    + describeFailure(
                                    secondResult
                            )
            );

            SettlementResponse firstResponse =
                    firstResult.response();

            SettlementResponse secondResponse =
                    secondResult.response();

            assertNotNull(
                    firstResponse
            );

            assertNotNull(
                    secondResponse
            );

            assertNotNull(
                    firstResponse.id()
            );

            assertNotNull(
                    secondResponse.id()
            );

            assertEquals(
                    firstResponse.id(),
                    secondResponse.id(),
                    "Concurrent duplicate requests must replay "
                            + "the same settlement"
            );

            assertEquals(
                    group.getId(),
                    firstResponse.groupId()
            );

            assertEquals(
                    debtor.getId(),
                    firstResponse.fromUserId()
            );

            assertEquals(
                    creditor.getId(),
                    firstResponse.toUserId()
            );

            assertEquals(
                    SettlementMode.PARTIAL,
                    firstResponse.mode()
            );

            assertMoney(
                    "300.00",
                    firstResponse.amount()
            );

            assertMoney(
                    "300.00",
                    secondResponse.amount()
            );

            List<Settlement> storedSettlements =
                    settlementRepository
                            .findByGroup_Id(
                                    group.getId()
                            );

            assertEquals(
                    1,
                    storedSettlements.size(),
                    "Exactly one settlement must be stored"
            );

            Settlement storedSettlement =
                    storedSettlements.getFirst();

            assertEquals(
                    firstResponse.id(),
                    storedSettlement.getId()
            );

            assertEquals(
                    IDEMPOTENCY_KEY,
                    storedSettlement.getIdempotencyKey()
            );

            assertEquals(
                    debtor.getId(),
                    storedSettlement
                            .getFromUser()
                            .getId()
            );

            assertEquals(
                    creditor.getId(),
                    storedSettlement
                            .getToUser()
                            .getId()
            );

            assertEquals(
                    debtor.getId(),
                    storedSettlement
                            .getCreatedBy()
                            .getId()
            );

            assertEquals(
                    SettlementMode.PARTIAL,
                    storedSettlement.getSettlementMode()
            );

            assertMoney(
                    "300.00",
                    storedSettlement.getAmount()
            );

            assertTrue(
                    settlementRepository
                            .findByIdempotencyKey(
                                    IDEMPOTENCY_KEY
                            )
                            .isPresent(),
                    "Settlement must be retrievable "
                            + "by idempotency key"
            );

            Integer settlementCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM settlements
                            WHERE idempotency_key = ?
                            """,
                            Integer.class,
                            IDEMPOTENCY_KEY
                    );

            assertNotNull(
                    settlementCount
            );

            assertEquals(
                    1,
                    settlementCount,
                    "The unique idempotency key must have "
                            + "exactly one settlement row"
            );

            Integer activityCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM group_activity
                            WHERE group_id = ?
                              AND activity_type = ?
                              AND reference_id = ?
                            """,
                            Integer.class,
                            group.getId(),
                            "SETTLEMENT_CREATED",
                            storedSettlement.getId()
                    );

            assertNotNull(
                    activityCount
            );

            assertEquals(
                    1,
                    activityCount,
                    "Exactly one settlement activity must be recorded"
            );

            Integer totalSettlementActivityCount =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM group_activity
                            WHERE group_id = ?
                              AND activity_type = ?
                            """,
                            Integer.class,
                            group.getId(),
                            "SETTLEMENT_CREATED"
                    );

            assertNotNull(
                    totalSettlementActivityCount
            );

            assertEquals(
                    1,
                    totalSettlementActivityCount,
                    "The losing transaction must not leave "
                            + "a duplicate activity record"
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
            SettlementCreateRequest request,
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

            SettlementResponse response =
                    settlementService.createSettlement(
                            group.getId(),
                            DEBTOR_EMAIL,
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
         * Flyway and Hibernate schema generation are disabled
         * in application-test.properties.
         *
         * Execute the production migrations needed by the
         * settlement workflow.
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
                        "db/migration/V7__create_settlements.sql"
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
             * Drop child tables before their parent tables.
             */
            statement.execute(
                    "DROP TABLE IF EXISTS group_activity"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS settlements"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS group_members"
            );

            statement.execute(
                    "DROP TABLE IF EXISTS expense_groups"
            );

            /*
             * V11 creates refresh_tokens with a foreign key to users.
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
            SettlementResponse response,
            Exception exception
    ) {
    }
}
