package com.expensemate.service;

import com.expensemate.dto.GroupCreateRequest;
import com.expensemate.dto.GroupMemberAddRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.User;
import com.expensemate.exception.ForbiddenOperationException;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private ExpenseGroupRepository expenseGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    private GroupService groupService;

    private User creator;
    private User secondUser;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(
                expenseGroupRepository,
                groupMemberRepository,
                userRepository
        );

        creator = mock(User.class);
        secondUser = mock(User.class);

        lenient().when(creator.getId()).thenReturn(1L);
        lenient().when(creator.getName()).thenReturn("Sree");
        lenient().when(creator.getEmail()).thenReturn("sree@example.com");

        lenient().when(secondUser.getId()).thenReturn(2L);
        lenient().when(secondUser.getName()).thenReturn("Test User");
        lenient().when(secondUser.getEmail()).thenReturn("testuser@example.com");
    }

    @Test
    void createGroup_shouldAutomaticallyAddCreatorAsMember() {
        GroupCreateRequest request = new GroupCreateRequest();
        request.setName("Goa Trip");
        request.setDescription("Shared expenses");

        ExpenseGroup savedGroup =
                new ExpenseGroup("Goa Trip", "Shared expenses", creator);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(creator));

        when(expenseGroupRepository.save(any(ExpenseGroup.class)))
                .thenReturn(savedGroup);

        when(groupMemberRepository.save(any(GroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(any()))
                .thenReturn(List.of());

        groupService.createGroup(
                "sree@example.com",
                request
        );

        ArgumentCaptor<GroupMember> memberCaptor =
                ArgumentCaptor.forClass(GroupMember.class);

        verify(groupMemberRepository).save(memberCaptor.capture());

        GroupMember savedMembership = memberCaptor.getValue();

        assertSame(creator, savedMembership.getUser());
        assertSame(savedGroup, savedMembership.getGroup());
    }

    @Test
    void addMember_shouldRejectDuplicateMember() {
        ExpenseGroup group =
                new ExpenseGroup("Goa Trip", null, creator);

        GroupMemberAddRequest request = new GroupMemberAddRequest();
        request.setEmail("testuser@example.com");

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(creator));

        when(expenseGroupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(true);

        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.of(secondUser));

        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L))
                .thenReturn(true);

        assertThrows(
                InvalidRequestException.class,
                () -> groupService.addMember(
                        1L,
                        "sree@example.com",
                        request
                )
        );

        verify(groupMemberRepository, never())
                .save(any(GroupMember.class));
    }

    @Test
    void updateGroup_shouldRejectNonCreator() {
        ExpenseGroup group =
                new ExpenseGroup("Goa Trip", null, creator);

        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.of(secondUser));

        when(expenseGroupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L))
                .thenReturn(true);

        com.expensemate.dto.GroupUpdateRequest request =
                new com.expensemate.dto.GroupUpdateRequest();

        request.setName("Changed Name");

        assertThrows(
                ForbiddenOperationException.class,
                () -> groupService.updateGroup(
                        1L,
                        "testuser@example.com",
                        request
                )
        );

        verify(expenseGroupRepository, never())
                .save(any());
    }

    @Test
    void getGroupDetails_shouldHideGroupFromNonMember() {
        ExpenseGroup group =
                new ExpenseGroup("Goa Trip", null, creator);

        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.of(secondUser));

        when(expenseGroupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 2L))
                .thenReturn(false);

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> groupService.getGroupDetails(
                                1L,
                                "testuser@example.com"
                        )
                );

        assertEquals(
                "Group not found",
                exception.getMessage()
        );
    }

    @Test
    void removeMember_shouldPreventRemovingCreator() {
        ExpenseGroup group =
                new ExpenseGroup("Goa Trip", null, creator);

        when(userRepository.findByEmail("sree@example.com"))
                .thenReturn(Optional.of(creator));

        when(expenseGroupRepository.findById(1L))
                .thenReturn(Optional.of(group));

        when(groupMemberRepository.existsByGroupIdAndUserId(1L, 1L))
                .thenReturn(true);

        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> groupService.removeMember(
                                1L,
                                1L,
                                "sree@example.com"
                        )
                );

        assertEquals(
                "Group creator cannot be removed",
                exception.getMessage()
        );

        verify(groupMemberRepository, never())
                .delete(any(GroupMember.class));
    }
}