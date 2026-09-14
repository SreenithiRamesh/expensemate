package com.expensemate.service;

import com.expensemate.dto.activity.GroupActivityResponse;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupActivity;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupActivityRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private GroupActivityRepository groupActivityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {

        activityService =
                new ActivityService(
                        groupActivityRepository,
                        userRepository,
                        expenseGroupRepository,
                        groupMemberRepository
                );
    }

    // ---------------------------------------------------------
    // RECORD ACTIVITY
    // ---------------------------------------------------------

    @Test
    void shouldRecordActivitySuccessfully() {

        User actor =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        activityService.record(
                group,
                actor,
                ActivityType.GROUP_CREATED,
                "Sree created group \"Goa Trip\"",
                2L
        );

        ArgumentCaptor<GroupActivity> captor =
                ArgumentCaptor.forClass(
                        GroupActivity.class
                );

        verify(
                groupActivityRepository
        ).save(
                captor.capture()
        );

        GroupActivity savedActivity =
                captor.getValue();

        assertNotNull(
                savedActivity
        );

        assertSame(
                group,
                savedActivity.getGroup()
        );

        assertSame(
                actor,
                savedActivity.getActor()
        );

        assertEquals(
                ActivityType.GROUP_CREATED,
                savedActivity.getActivityType()
        );

        assertEquals(
                "Sree created group \"Goa Trip\"",
                savedActivity.getDescription()
        );

        assertEquals(
                2L,
                savedActivity.getReferenceId()
        );
    }

    // ---------------------------------------------------------
    // GET ACTIVITY HISTORY
    // ---------------------------------------------------------

    @Test
    void shouldReturnGroupActivityForMember() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        LocalDateTime firstTime =
                LocalDateTime.of(
                        2026,
                        9,
                        15,
                        1,
                        10
                );

        LocalDateTime secondTime =
                LocalDateTime.of(
                        2026,
                        9,
                        15,
                        1,
                        5
                );

        GroupActivity newestActivity =
                activity(
                        10L,
                        group,
                        currentUser,
                        ActivityType.GROUP_UPDATED,
                        "Sree updated group details",
                        2L,
                        firstTime
                );

        GroupActivity olderActivity =
                activity(
                        9L,
                        group,
                        currentUser,
                        ActivityType.GROUP_CREATED,
                        "Sree created group \"Goa Trip\"",
                        2L,
                        secondTime
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository.findById(
                        2L
                )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        /*
         * Repository contract already returns
         * newest activity first.
         */
        when(
                groupActivityRepository
                        .findByGroup_IdOrderByCreatedAtDesc(
                                2L
                        )
        ).thenReturn(
                List.of(
                        newestActivity,
                        olderActivity
                )
        );

        List<GroupActivityResponse> response =
                activityService.getGroupActivity(
                        2L,
                        "sree@example.com"
                );

        assertNotNull(
                response
        );

        assertEquals(
                2,
                response.size()
        );

        /*
         * First response = newest event.
         */
        GroupActivityResponse first =
                response.get(0);

        assertEquals(
                10L,
                first.id()
        );

        assertEquals(
                2L,
                first.groupId()
        );

        assertEquals(
                1L,
                first.actorUserId()
        );

        assertEquals(
                "Sree",
                first.actorName()
        );

        assertEquals(
                ActivityType.GROUP_UPDATED,
                first.activityType()
        );

        assertEquals(
                "Sree updated group details",
                first.description()
        );

        assertEquals(
                2L,
                first.referenceId()
        );

        assertEquals(
                firstTime,
                first.createdAt()
        );

        /*
         * Second response = older event.
         */
        GroupActivityResponse second =
                response.get(1);

        assertEquals(
                9L,
                second.id()
        );

        assertEquals(
                ActivityType.GROUP_CREATED,
                second.activityType()
        );

        verify(
                groupActivityRepository
        ).findByGroup_IdOrderByCreatedAtDesc(
                2L
        );
    }

    // ---------------------------------------------------------
    // NON MEMBER
    // ---------------------------------------------------------

    @Test
    void shouldRejectNonMemberFromViewingActivity() {

        User outsider =
                user(
                        99L,
                        "Outsider",
                        "outsider@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        when(
                userRepository.findByEmail(
                        "outsider@example.com"
                )
        ).thenReturn(
                Optional.of(
                        outsider
                )
        );

        when(
                expenseGroupRepository.findById(
                        2L
                )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                99L
                        )
        ).thenReturn(
                false
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                activityService
                                        .getGroupActivity(
                                                2L,
                                                "outsider@example.com"
                                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );

        verify(
                groupActivityRepository,
                never()
        ).findByGroup_IdOrderByCreatedAtDesc(
                anyLong()
        );
    }

    // ---------------------------------------------------------
    // GROUP DOES NOT EXIST
    // ---------------------------------------------------------

    @Test
    void shouldRejectMissingGroup() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository.findById(
                        999L
                )
        ).thenReturn(
                Optional.empty()
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                activityService
                                        .getGroupActivity(
                                                999L,
                                                "sree@example.com"
                                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );

        verify(
                groupMemberRepository,
                never()
        ).existsByGroupIdAndUserId(
                anyLong(),
                anyLong()
        );

        verify(
                groupActivityRepository,
                never()
        ).findByGroup_IdOrderByCreatedAtDesc(
                anyLong()
        );
    }

    // ---------------------------------------------------------
    // USER DOES NOT EXIST
    // ---------------------------------------------------------

    @Test
    void shouldRejectMissingCurrentUser() {

        when(
                userRepository.findByEmail(
                        "missing@example.com"
                )
        ).thenReturn(
                Optional.empty()
        );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () ->
                                activityService
                                        .getGroupActivity(
                                                2L,
                                                "missing@example.com"
                                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(
                expenseGroupRepository,
                never()
        ).findById(
                anyLong()
        );

        verify(
                groupActivityRepository,
                never()
        ).findByGroup_IdOrderByCreatedAtDesc(
                anyLong()
        );
    }

    // ---------------------------------------------------------
    // EMPTY ACTIVITY HISTORY
    // ---------------------------------------------------------

    @Test
    void shouldReturnEmptyListWhenGroupHasNoActivity() {

        User currentUser =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        currentUser
                )
        );

        when(
                expenseGroupRepository.findById(
                        2L
                )
        ).thenReturn(
                Optional.of(
                        group
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                1L
                        )
        ).thenReturn(
                true
        );

        when(
                groupActivityRepository
                        .findByGroup_IdOrderByCreatedAtDesc(
                                2L
                        )
        ).thenReturn(
                List.of()
        );

        List<GroupActivityResponse> response =
                activityService.getGroupActivity(
                        2L,
                        "sree@example.com"
                );

        assertNotNull(
                response
        );

        assertTrue(
                response.isEmpty()
        );
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private User user(
            Long id,
            String name,
            String email
    ) {

        User user =
                mock(
                        User.class
                );

        lenient()
                .when(
                        user.getId()
                )
                .thenReturn(
                        id
                );

        lenient()
                .when(
                        user.getName()
                )
                .thenReturn(
                        name
                );

        lenient()
                .when(
                        user.getEmail()
                )
                .thenReturn(
                        email
                );

        return user;
    }

    private ExpenseGroup group(
            Long id
    ) {

        ExpenseGroup group =
                mock(
                        ExpenseGroup.class
                );

        lenient()
                .when(
                        group.getId()
                )
                .thenReturn(
                        id
                );

        return group;
    }

    private GroupActivity activity(
            Long id,
            ExpenseGroup group,
            User actor,
            ActivityType activityType,
            String description,
            Long referenceId,
            LocalDateTime createdAt
    ) {

        GroupActivity activity =
                mock(
                        GroupActivity.class
                );

        lenient()
                .when(
                        activity.getId()
                )
                .thenReturn(
                        id
                );

        lenient()
                .when(
                        activity.getGroup()
                )
                .thenReturn(
                        group
                );

        lenient()
                .when(
                        activity.getActor()
                )
                .thenReturn(
                        actor
                );

        lenient()
                .when(
                        activity.getActivityType()
                )
                .thenReturn(
                        activityType
                );

        lenient()
                .when(
                        activity.getDescription()
                )
                .thenReturn(
                        description
                );

        lenient()
                .when(
                        activity.getReferenceId()
                )
                .thenReturn(
                        referenceId
                );

        lenient()
                .when(
                        activity.getCreatedAt()
                )
                .thenReturn(
                        createdAt
                );

        return activity;
    }
}