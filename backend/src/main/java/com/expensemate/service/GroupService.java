package com.expensemate.service;

import com.expensemate.dto.GroupCreateRequest;
import com.expensemate.dto.GroupDetailsResponse;
import com.expensemate.dto.GroupMemberAddRequest;
import com.expensemate.dto.GroupMemberResponse;
import com.expensemate.dto.GroupSummaryResponse;
import com.expensemate.dto.GroupUpdateRequest;
import com.expensemate.entity.ExpenseGroup;
import com.expensemate.entity.GroupMember;
import com.expensemate.entity.User;
import com.expensemate.exception.ForbiddenOperationException;
import com.expensemate.exception.InvalidRequestException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.ExpenseGroupRepository;
import com.expensemate.repository.GroupMemberRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository
    ) {
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------

    @Transactional
    public GroupDetailsResponse createGroup(
            String currentUserEmail,
            GroupCreateRequest request
    ) {

        User creator = getUserByEmail(currentUserEmail);

        ExpenseGroup group = new ExpenseGroup(
                request.getName().trim(),
                normalizeDescription(request.getDescription()),
                creator
        );

        ExpenseGroup savedGroup =
                expenseGroupRepository.save(group);

        GroupMember creatorMembership = new GroupMember(
                savedGroup,
                creator
        );

        groupMemberRepository.save(creatorMembership);

        return toDetailsResponse(savedGroup);
    }

    // ---------------------------------------------------------
    // GET ALL GROUPS FOR CURRENT USER
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getGroupsForUser(
            String currentUserEmail
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        List<GroupMember> memberships =
                groupMemberRepository.findGroupsForUser(
                        currentUser.getId()
                );

        return memberships.stream()
                .map(GroupMember::getGroup)
                .map(this::toSummaryResponse)
                .toList();
    }

    // ---------------------------------------------------------
    // GET GROUP DETAILS
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public GroupDetailsResponse getGroupDetails(
            Long groupId,
            String currentUserEmail
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        ExpenseGroup group = getGroupForMember(
                groupId,
                currentUser.getId()
        );

        return toDetailsResponse(group);
    }

    // ---------------------------------------------------------
    // UPDATE GROUP
    // ---------------------------------------------------------

    @Transactional
    public GroupDetailsResponse updateGroup(
            Long groupId,
            String currentUserEmail,
            GroupUpdateRequest request
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        ExpenseGroup group = getGroupForMember(
                groupId,
                currentUser.getId()
        );

        requireCreator(
                group,
                currentUser.getId()
        );

        group.updateDetails(
                request.getName().trim(),
                normalizeDescription(request.getDescription())
        );

        ExpenseGroup updatedGroup =
                expenseGroupRepository.save(group);

        return toDetailsResponse(updatedGroup);
    }

    // ---------------------------------------------------------
    // DELETE GROUP
    // ---------------------------------------------------------

    @Transactional
    public void deleteGroup(
            Long groupId,
            String currentUserEmail
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        ExpenseGroup group = getGroupForMember(
                groupId,
                currentUser.getId()
        );

        requireCreator(
                group,
                currentUser.getId()
        );

        expenseGroupRepository.delete(group);
    }

    // ---------------------------------------------------------
    // ADD MEMBER
    // ---------------------------------------------------------

    @Transactional
    public GroupMemberResponse addMember(
            Long groupId,
            String currentUserEmail,
            GroupMemberAddRequest request
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        ExpenseGroup group = getGroupForMember(
                groupId,
                currentUser.getId()
        );

        requireCreator(
                group,
                currentUser.getId()
        );

        String memberEmail = request.getEmail().trim();

        User memberToAdd = userRepository
                .findByEmail(memberEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        boolean alreadyMember =
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                memberToAdd.getId()
                        );

        if (alreadyMember) {
            throw new InvalidRequestException(
                    "User is already a member of this group"
            );
        }

        GroupMember groupMember = new GroupMember(
                group,
                memberToAdd
        );

        GroupMember savedMembership =
                groupMemberRepository.save(groupMember);

        return toMemberResponse(savedMembership);
    }

    // ---------------------------------------------------------
    // REMOVE MEMBER
    // ---------------------------------------------------------

    @Transactional
    public void removeMember(
            Long groupId,
            Long memberUserId,
            String currentUserEmail
    ) {

        User currentUser = getUserByEmail(currentUserEmail);

        ExpenseGroup group = getGroupForMember(
                groupId,
                currentUser.getId()
        );

        requireCreator(
                group,
                currentUser.getId()
        );

        if (group.getCreatedBy()
                .getId()
                .equals(memberUserId)) {

            throw new InvalidRequestException(
                    "Group creator cannot be removed"
            );
        }

        GroupMember membership =
                groupMemberRepository
                        .findByGroupIdAndUserId(
                                groupId,
                                memberUserId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Group member not found"
                                )
                        );

        groupMemberRepository.delete(membership);
    }

    // ---------------------------------------------------------
    // USER HELPER
    // ---------------------------------------------------------

    private User getUserByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    // ---------------------------------------------------------
    // GROUP HELPER
    // ---------------------------------------------------------

    private ExpenseGroup getGroupById(Long groupId) {

        return expenseGroupRepository
                .findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group not found"
                        )
                );
    }

    // ---------------------------------------------------------
    // MEMBERSHIP PERMISSION
    // ---------------------------------------------------------

    private ExpenseGroup getGroupForMember(
            Long groupId,
            Long userId
    ) {

        ExpenseGroup group = getGroupById(groupId);

        boolean isMember =
                groupMemberRepository
                        .existsByGroupIdAndUserId(
                                groupId,
                                userId
                        );

        if (!isMember) {
            throw new ResourceNotFoundException(
                    "Group not found"
            );
        }

        return group;
    }

    // ---------------------------------------------------------
    // CREATOR PERMISSION
    // ---------------------------------------------------------

    private void requireCreator(
            ExpenseGroup group,
            Long currentUserId
    ) {

        if (!group.getCreatedBy()
                .getId()
                .equals(currentUserId)) {

            throw new ForbiddenOperationException(
                    "Only the group creator can perform this action"
            );
        }
    }

    // ---------------------------------------------------------
    // SUMMARY DTO MAPPING
    // ---------------------------------------------------------

    private GroupSummaryResponse toSummaryResponse(
            ExpenseGroup group
    ) {

        long memberCount =
                groupMemberRepository.countByGroupId(
                        group.getId()
                );

        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedBy().getId(),
                group.getCreatedBy().getName(),
                memberCount,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------
    // DETAILS DTO MAPPING
    // ---------------------------------------------------------

    private GroupDetailsResponse toDetailsResponse(
            ExpenseGroup group
    ) {

        List<GroupMemberResponse> members =
                groupMemberRepository
                        .findByGroupIdOrderByJoinedAtAsc(
                                group.getId()
                        )
                        .stream()
                        .map(this::toMemberResponse)
                        .toList();

        User creator = group.getCreatedBy();

        return new GroupDetailsResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                creator.getId(),
                creator.getName(),
                creator.getEmail(),
                members,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }

    // ---------------------------------------------------------
    // MEMBER DTO MAPPING
    // ---------------------------------------------------------

    private GroupMemberResponse toMemberResponse(
            GroupMember membership
    ) {

        User user = membership.getUser();

        return new GroupMemberResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                membership.getJoinedAt()
        );
    }

    // ---------------------------------------------------------
    // STRING NORMALIZATION
    // ---------------------------------------------------------

    private String normalizeDescription(
            String description
    ) {

        if (description == null) {
            return null;
        }

        String trimmedDescription =
                description.trim();

        return trimmedDescription.isEmpty()
                ? null
                : trimmedDescription;
    }
}