package com.expensemate.performance;

import com.expensemate.dto.activity.GroupActivityResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupActivity;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupActivityRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import com.expensemate.service.ActivityService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ActivityQueryPerformanceIntegrationTest
        extends HibernateQueryCountSupport {

    private static final int ACTIVITY_COUNT = 10;

    private static final long MAXIMUM_QUERY_COUNT = 4;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupActivityRepository groupActivityRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {

        createRequiredTables();
    }

    @Test
    void activityTimelineQueryCountShouldRemainBounded() {

        String uniqueSuffix =
                UUID.randomUUID().toString();

        User owner =
                createUser(
                        "Performance Owner",
                        "performance-owner-"
                                + uniqueSuffix
                                + "@example.com"
                );

        ExpenseGroup group =
                expenseGroupRepository.saveAndFlush(
                        new ExpenseGroup(
                                "Performance Group",
                                "M27 activity query measurement",
                                owner
                        )
                );

        groupMemberRepository.saveAndFlush(
                new GroupMember(
                        group,
                        owner
                )
        );

        List<GroupActivity> activities =
                new ArrayList<>();

        for (int index = 0;
             index < ACTIVITY_COUNT;
             index++) {

            User actor =
                    createUser(
                            "Performance Actor " + index,
                            "performance-actor-"
                                    + index
                                    + "-"
                                    + uniqueSuffix
                                    + "@example.com"
                    );

            groupMemberRepository.save(
                    new GroupMember(
                            group,
                            actor
                    )
            );

            activities.add(
                    new GroupActivity(
                            group,
                            actor,
                            ActivityType.SHARED_EXPENSE_CREATED,
                            "Performance activity " + index,
                            (long) index + 1
                    )
            );
        }

        groupMemberRepository.flush();

        groupActivityRepository.saveAllAndFlush(
                activities
        );

        /*
         * Clear the persistence context before measurement.
         *
         * Otherwise, Hibernate could reuse the users and group
         * created during setup and hide lazy-loading queries.
         */
        entityManager.clear();

        resetQueryStatistics();

        List<GroupActivityResponse> response =
                activityService.getGroupActivity(
                        group.getId(),
                        owner.getEmail()
                );

        long queryCount =
                preparedStatementCount();

        System.out.println(
                "M27_ACTIVITY_OPTIMIZED_QUERY_COUNT="
                        + queryCount
        );

        assertEquals(
                ACTIVITY_COUNT,
                response.size(),
                "The activity timeline must return every activity"
        );

        /*
         * Expected optimized query structure:
         *
         * 1 query - current user
         * 1 query - expense group
         * 1 query - membership check
         * 1 query - activities with group and actor fetch joins
         *
         * The number of statements must not grow with the
         * number of activity rows or distinct actors.
         */
        assertTrue(
                queryCount <= MAXIMUM_QUERY_COUNT,
                "Activity timeline query count must remain "
                        + "bounded at "
                        + MAXIMUM_QUERY_COUNT
                        + " statements or fewer, but it executed "
                        + queryCount
                        + " statements for "
                        + ACTIVITY_COUNT
                        + " activities"
        );
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