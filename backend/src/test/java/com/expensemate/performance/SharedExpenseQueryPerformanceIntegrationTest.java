package com.expensemate.performance;

import com.expensemate.dto.SharedExpenseCreateRequest;
import com.expensemate.dto.SplitInputRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.SharedExpense;
import com.expensemate.entity.User;
import com.expensemate.enums.SplitType;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.SharedExpenseWriteService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SharedExpenseQueryPerformanceIntegrationTest
        extends HibernateQueryCountSupport {

    private static final int PARTICIPANT_COUNT = 10;

    /*
     * The current implementation performs:
     *
     * - one membership query for the payer
     * - one membership query for every split participant
     * - one user query for every calculated split
     *
     * Together with the required expense/split INSERT
     * statements, this produces a query count that grows
     * substantially faster than the required financial
     * writes alone.
     */
    private static final long MAXIMUM_OPTIMIZED_STATEMENT_COUNT = 18L;

    @Autowired
    private SharedExpenseWriteService sharedExpenseWriteService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        createRequiredTables();
    }

    @Test
    void sharedExpenseCreationQueryCountShouldRemainBounded() {

        String uniqueSuffix =
                UUID.randomUUID().toString();

        User creator =
                createUser(
                        "Performance Creator",
                        "shared-performance-creator-"
                                + uniqueSuffix
                                + "@example.com"
                );

        ExpenseGroup group =
                expenseGroupRepository.saveAndFlush(
                        new ExpenseGroup(
                                "Shared Expense Performance Group",
                                "M27 shared-expense query measurement",
                                creator
                        )
                );

        List<User> participants =
                new ArrayList<>();

        participants.add(creator);

        groupMemberRepository.save(
                new GroupMember(
                        group,
                        creator
                )
        );

        for (int index = 1;
             index < PARTICIPANT_COUNT;
             index++) {

            User participant =
                    createUser(
                            "Performance Participant " + index,
                            "shared-performance-participant-"
                                    + index
                                    + "-"
                                    + uniqueSuffix
                                    + "@example.com"
                    );

            participants.add(participant);

            groupMemberRepository.save(
                    new GroupMember(
                            group,
                            participant
                    )
            );
        }

        groupMemberRepository.flush();

        SharedExpenseCreateRequest request =
                createEqualSplitRequest(
                        creator.getId(),
                        participants
                );

        /*
         * Make sure all setup statements have completed
         * before query statistics are reset.
         */
        entityManager.flush();

        /*
         * Remove setup entities from Hibernate's first-level
         * cache. Otherwise, findById calls could reuse managed
         * users and conceal the actual participant lookup
         * behaviour.
         */
        entityManager.clear();

        resetQueryStatistics();

        SharedExpense result =
                sharedExpenseWriteService.create(
                        group.getId(),
                        creator.getId(),
                        "m27-performance-" + uniqueSuffix,
                        createFingerprint(uniqueSuffix),
                        request
                );

        /*
         * Force pending expense-split and activity INSERT
         * statements to execute so the measurement represents
         * the complete write operation.
         */
        entityManager.flush();

        long statementCount =
                preparedStatementCount();

        System.out.println(
                "M27_SHARED_EXPENSE_OPTIMIZED_PARTICIPANTS="
                        + PARTICIPANT_COUNT
        );

        System.out.println(
                "M27_SHARED_EXPENSE_OPTIMIZED_STATEMENT_COUNT="
                        + statementCount
        );

        assertNotNull(result);
        assertNotNull(result.getId());

        assertEquals(
                PARTICIPANT_COUNT,
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM expense_splits
                        WHERE expense_id = ?
                        """,
                        Integer.class,
                        result.getId()
                )
        );

        /*
         * This assertion intentionally documents the current
         * inefficient baseline.
         *
         * It will be replaced with a bounded statement-count
         * assertion after member/user loading is consolidated
         * into one bulk query.
         */
        assertTrue(
                statementCount
                        <= MAXIMUM_OPTIMIZED_STATEMENT_COUNT,
                "Expected shared-expense creation with "
                        + PARTICIPANT_COUNT
                        + " participants to execute at most "
                        + MAXIMUM_OPTIMIZED_STATEMENT_COUNT
                        + " statements after bulk member loading, but it executed "
                        + statementCount
        );
    }

    private SharedExpenseCreateRequest createEqualSplitRequest(
            Long paidByUserId,
            List<User> participants
    ) {

        List<SplitInputRequest> splits =
                participants.stream()
                        .map(user -> {

                            SplitInputRequest split =
                                    new SplitInputRequest();

                            split.setUserId(
                                    user.getId()
                            );

                            /*
                             * EQUAL splitting does not require
                             * an explicit value.
                             */
                            split.setValue(null);

                            return split;
                        })
                        .toList();

        SharedExpenseCreateRequest request =
                new SharedExpenseCreateRequest();

        request.setTitle(
                "M27 performance dinner"
        );

        request.setAmount(
                new BigDecimal("1000.00")
        );

        request.setPaidByUserId(
                paidByUserId
        );

        request.setSplitType(
                SplitType.EQUAL
        );

        request.setExpenseDate(
                LocalDate.of(
                        2026,
                        9,
                        28
                )
        );

        request.setSplits(
                splits
        );

        return request;
    }

    private User createUser(
            String name,
            String email
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        return userRepository.saveAndFlush(
                new User(
                        name,
                        email,
                        "encoded-password",
                        now,
                        now
                )
        );
    }

    private String createFingerprint(
            String uniqueSuffix
    ) {

        /*
         * request_fingerprint allows up to 64 characters.
         * Removing UUID hyphens produces a deterministic
         * 32-character test value.
         */
        return uniqueSuffix.replace(
                "-",
                ""
        );
    }

    private void createRequiredTables() {

        jdbcTemplate.execute("""
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

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS expense_groups (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(120) NOT NULL,
                    description VARCHAR(255),
                    created_by BIGINT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,

                    CONSTRAINT fk_expense_groups_created_by
                        FOREIGN KEY (created_by)
                        REFERENCES users(id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS group_members (
                    id BIGSERIAL PRIMARY KEY,
                    group_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    joined_at TIMESTAMP NOT NULL,

                    CONSTRAINT fk_group_members_group
                        FOREIGN KEY (group_id)
                        REFERENCES expense_groups(id)
                        ON DELETE CASCADE,

                    CONSTRAINT fk_group_members_user
                        FOREIGN KEY (user_id)
                        REFERENCES users(id),

                    CONSTRAINT uq_group_members_group_user
                        UNIQUE (group_id, user_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS shared_expenses (
                    id BIGSERIAL PRIMARY KEY,
                    group_id BIGINT NOT NULL,
                    paid_by BIGINT NOT NULL,
                    created_by BIGINT,
                    idempotency_key VARCHAR(100),
                    request_fingerprint VARCHAR(64),
                    title VARCHAR(150) NOT NULL,
                    amount NUMERIC(12, 2) NOT NULL,
                    split_type VARCHAR(30) NOT NULL,
                    expense_date DATE NOT NULL,
                    created_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

                    CONSTRAINT fk_shared_expenses_group
                        FOREIGN KEY (group_id)
                        REFERENCES expense_groups(id)
                        ON DELETE CASCADE,

                    CONSTRAINT fk_shared_expenses_paid_by
                        FOREIGN KEY (paid_by)
                        REFERENCES users(id),

                    CONSTRAINT fk_shared_expenses_created_by
                        FOREIGN KEY (created_by)
                        REFERENCES users(id),

                    CONSTRAINT uk_shared_expense_user_idempotency
                        UNIQUE (created_by, idempotency_key),

                    CONSTRAINT chk_shared_expense_amount
                        CHECK (amount > 0)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS expense_splits (
                    id BIGSERIAL PRIMARY KEY,
                    expense_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    share_amount NUMERIC(12, 2) NOT NULL,
                    percentage NUMERIC(7, 4),

                    CONSTRAINT fk_expense_splits_expense
                        FOREIGN KEY (expense_id)
                        REFERENCES shared_expenses(id)
                        ON DELETE CASCADE,

                    CONSTRAINT fk_expense_splits_user
                        FOREIGN KEY (user_id)
                        REFERENCES users(id),

                    CONSTRAINT chk_expense_split_share_amount
                        CHECK (share_amount >= 0),

                    CONSTRAINT chk_expense_split_percentage
                        CHECK (
                            percentage IS NULL
                            OR (
                                percentage >= 0
                                AND percentage <= 100
                            )
                        ),

                    CONSTRAINT uq_expense_split_expense_user
                        UNIQUE (expense_id, user_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS group_activity (
                    id BIGSERIAL PRIMARY KEY,
                    group_id BIGINT NOT NULL,
                    actor_user_id BIGINT NOT NULL,
                    activity_type VARCHAR(50) NOT NULL,
                    description VARCHAR(500) NOT NULL,
                    reference_id BIGINT,
                    created_at TIMESTAMP NOT NULL,

                    CONSTRAINT fk_group_activity_group
                        FOREIGN KEY (group_id)
                        REFERENCES expense_groups(id),

                    CONSTRAINT fk_group_activity_actor
                        FOREIGN KEY (actor_user_id)
                        REFERENCES users(id)
                )
                """);
    }
}