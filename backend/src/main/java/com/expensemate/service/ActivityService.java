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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ActivityService {

    private final GroupActivityRepository groupActivityRepository;
    private final UserRepository userRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ActivityService(
            GroupActivityRepository groupActivityRepository,
            UserRepository userRepository,
            ExpenseGroupRepository expenseGroupRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.groupActivityRepository = groupActivityRepository;
        this.userRepository = userRepository;
        this.expenseGroupRepository = expenseGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public void record(
            ExpenseGroup group,
            User actor,
            ActivityType activityType,
            String description,
            Long referenceId
    ) {

        GroupActivity activity = new GroupActivity(
                group,
                actor,
                activityType,
                description,
                referenceId
        );

        groupActivityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public List<GroupActivityResponse> getGroupActivity(
            Long groupId,
            String email
    ) {

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found")
                );

        ExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Group not found")
                );

        boolean isMember =
                groupMemberRepository.existsByGroupIdAndUserId(
                        groupId,
                        currentUser.getId()
                );

        if (!isMember) {
            throw new ResourceNotFoundException("Group not found");
        }

        return groupActivityRepository
                .findByGroup_IdOrderByCreatedAtDesc(group.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GroupActivityResponse toResponse(
            GroupActivity activity
    ) {

        return new GroupActivityResponse(

                activity.getId(),

                activity.getGroup().getId(),

                activity.getActor().getId(),
                activity.getActor().getName(),

                activity.getActivityType(),

                activity.getDescription(),

                activity.getReferenceId(),

                activity.getCreatedAt()
        );
    }
}