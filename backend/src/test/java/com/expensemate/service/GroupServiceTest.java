package com.expensemate.service;

import com.expensemate.dto.GroupCreateRequest;
import com.expensemate.dto.GroupDetailsResponse;
import com.expensemate.dto.GroupMemberAddRequest;
import com.expensemate.dto.GroupMemberResponse;
import com.expensemate.dto.GroupUpdateRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.User;
import com.expensemate.enums.ActivityType;
import com.expensemate.exception.ForbiddenOperationException;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    // M13
    @Mock
    private ActivityService activityService;

    private GroupService groupService;

    @BeforeEach
    void setUp() {

        groupService =
                new GroupService(
                        expenseGroupRepository,
                        groupMemberRepository,
                        userRepository,
                        activityService
                );
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------

    @Test
    void shouldCreateGroupAndRecordActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        GroupCreateRequest request =
                mock(
                        GroupCreateRequest.class
                );

        when(
                request.getName()
        ).thenReturn(
                "Goa Trip"
        );

        when(
                request.getDescription()
        ).thenReturn(
                "Shared expenses for Goa trip"
        );

        ExpenseGroup savedGroup =
                group(
                        2L,
                        "Goa Trip",
                        "Shared expenses for Goa trip",
                        creator
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
                )
        );

        when(
                expenseGroupRepository.save(
                        any(ExpenseGroup.class)
                )
        ).thenReturn(
                savedGroup
        );

        when(
                groupMemberRepository.save(
                        any(GroupMember.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        when(
                groupMemberRepository
                        .findByGroupIdOrderByJoinedAtAsc(
                                2L
                        )
        ).thenReturn(
                List.of()
        );

        GroupDetailsResponse response =
                groupService.createGroup(
                        "sree@example.com",
                        request
                );

        assertNotNull(
                response
        );

        verify(
                expenseGroupRepository
        ).save(
                any(ExpenseGroup.class)
        );

        verify(
                groupMemberRepository
        ).save(
                any(GroupMember.class)
        );

        /*
         * M13
         */
        verify(
                activityService
        ).record(
                eq(savedGroup),
                eq(creator),
                eq(ActivityType.GROUP_CREATED),
                contains("created group"),
                eq(2L)
        );
    }

    // ---------------------------------------------------------
    // UPDATE GROUP
    // ---------------------------------------------------------

    @Test
    void shouldUpdateGroupAndRecordActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Old description",
                        creator
                );

        GroupUpdateRequest request =
                mock(
                        GroupUpdateRequest.class
                );

        when(
                request.getName()
        ).thenReturn(
                "Updated Goa Trip"
        );

        when(
                request.getDescription()
        ).thenReturn(
                "Updated description"
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
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
                expenseGroupRepository.save(
                        group
                )
        ).thenReturn(
                group
        );

        when(
                groupMemberRepository
                        .findByGroupIdOrderByJoinedAtAsc(
                                2L
                        )
        ).thenReturn(
                List.of()
        );

        GroupDetailsResponse response =
                groupService.updateGroup(
                        2L,
                        "sree@example.com",
                        request
                );

        assertNotNull(
                response
        );

        verify(
                group
        ).updateDetails(
                "Updated Goa Trip",
                "Updated description"
        );

        verify(
                expenseGroupRepository
        ).save(
                group
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(creator),
                eq(ActivityType.GROUP_UPDATED),
                contains("updated group details"),
                eq(2L)
        );
    }

    // ---------------------------------------------------------
    // ADD MEMBER
    // ---------------------------------------------------------

    @Test
    void shouldAddMemberAndRecordActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User memberToAdd =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Trip expenses",
                        creator
                );

        GroupMemberAddRequest request =
                mock(
                        GroupMemberAddRequest.class
                );

        when(
                request.getEmail()
        ).thenReturn(
                "testuser@example.com"
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
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
                userRepository.findByEmail(
                        "testuser@example.com"
                )
        ).thenReturn(
                Optional.of(
                        memberToAdd
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                2L
                        )
        ).thenReturn(
                false
        );

        GroupMember savedMembership =
                membership(
                        memberToAdd
                );

        when(
                groupMemberRepository.save(
                        any(GroupMember.class)
                )
        ).thenReturn(
                savedMembership
        );

        GroupMemberResponse response =
                groupService.addMember(
                        2L,
                        "sree@example.com",
                        request
                );

        assertNotNull(
                response
        );

        verify(
                groupMemberRepository
        ).save(
                any(GroupMember.class)
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(creator),
                eq(ActivityType.MEMBER_ADDED),
                contains("Test User"),
                eq(2L)
        );
    }

    // ---------------------------------------------------------
    // REMOVE MEMBER
    // ---------------------------------------------------------

    @Test
    void shouldRemoveMemberAndRecordActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User memberToRemove =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Trip expenses",
                        creator
                );

        GroupMember membership =
                membership(
                        memberToRemove
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
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
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                2L,
                                2L
                        )
        ).thenReturn(
                Optional.of(
                        membership
                )
        );

        groupService.removeMember(
                2L,
                2L,
                "sree@example.com"
        );

        verify(
                groupMemberRepository
        ).delete(
                membership
        );

        verify(
                activityService
        ).record(
                eq(group),
                eq(creator),
                eq(ActivityType.MEMBER_REMOVED),
                contains("Test User"),
                eq(2L)
        );
    }

    // ---------------------------------------------------------
    // DUPLICATE MEMBER
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicateMemberWithoutRecordingActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User existingMember =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Trip expenses",
                        creator
                );

        GroupMemberAddRequest request =
                mock(
                        GroupMemberAddRequest.class
                );

        when(
                request.getEmail()
        ).thenReturn(
                "testuser@example.com"
        );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
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
                userRepository.findByEmail(
                        "testuser@example.com"
                )
        ).thenReturn(
                Optional.of(
                        existingMember
                )
        );

        when(
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                2L,
                                2L
                        )
        ).thenReturn(
                true
        );

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                groupService.addMember(
                                        2L,
                                        "sree@example.com",
                                        request
                                )
                );

        assertEquals(
                "User is already a member of this group",
                exception.getMessage()
        );

        verify(
                groupMemberRepository,
                never()
        ).save(
                any(GroupMember.class)
        );

        verifyNoInteractions(
                activityService
        );
    }

    // ---------------------------------------------------------
    // NON-CREATOR CANNOT UPDATE
    // ---------------------------------------------------------

    @Test
    void shouldRejectUpdateByNonCreatorWithoutRecordingActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        User anotherMember =
                user(
                        2L,
                        "Test User",
                        "testuser@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Trip expenses",
                        creator
                );

        GroupUpdateRequest request =
                mock(
                        GroupUpdateRequest.class
                );

        when(
                userRepository.findByEmail(
                        "testuser@example.com"
                )
        ).thenReturn(
                Optional.of(
                        anotherMember
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
                                2L
                        )
        ).thenReturn(
                true
        );

        ForbiddenOperationException exception =
                assertThrows(
                        ForbiddenOperationException.class,
                        () ->
                                groupService.updateGroup(
                                        2L,
                                        "testuser@example.com",
                                        request
                                )
                );

        assertEquals(
                "Only the group creator can perform this action",
                exception.getMessage()
        );

        verify(
                expenseGroupRepository,
                never()
        ).save(
                any()
        );

        verifyNoInteractions(
                activityService
        );
    }

    // ---------------------------------------------------------
    // CREATOR CANNOT BE REMOVED
    // ---------------------------------------------------------

    @Test
    void shouldRejectRemovingGroupCreatorWithoutRecordingActivity() {

        User creator =
                user(
                        1L,
                        "Sree",
                        "sree@example.com"
                );

        ExpenseGroup group =
                group(
                        2L,
                        "Goa Trip",
                        "Trip expenses",
                        creator
                );

        when(
                userRepository.findByEmail(
                        "sree@example.com"
                )
        ).thenReturn(
                Optional.of(
                        creator
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

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () ->
                                groupService.removeMember(
                                        2L,
                                        1L,
                                        "sree@example.com"
                                )
                );

        assertEquals(
                "Group creator cannot be removed",
                exception.getMessage()
        );

        verify(
                groupMemberRepository,
                never()
        ).delete(
                any()
        );

        verifyNoInteractions(
                activityService
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
            Long id,
            String name,
            String description,
            User creator
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

        lenient()
                .when(
                        group.getName()
                )
                .thenReturn(
                        name
                );

        lenient()
                .when(
                        group.getDescription()
                )
                .thenReturn(
                        description
                );

        lenient()
                .when(
                        group.getCreatedBy()
                )
                .thenReturn(
                        creator
                );

        return group;
    }

    private GroupMember membership(
            User user
    ) {

        GroupMember membership =
                mock(
                        GroupMember.class
                );

        lenient()
                .when(
                        membership.getUser()
                )
                .thenReturn(
                        user
                );

        return membership;
    }
}