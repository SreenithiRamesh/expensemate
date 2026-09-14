package com.expensemate.controller;

import com.expensemate.dto.GroupCreateRequest;
import com.expensemate.dto.GroupDetailsResponse;
import com.expensemate.dto.GroupMemberAddRequest;
import com.expensemate.dto.GroupMemberResponse;
import com.expensemate.dto.GroupSummaryResponse;
import com.expensemate.dto.GroupUpdateRequest;
import com.expensemate.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // ---------------------------------------------------------
    // CREATE GROUP
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<GroupDetailsResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest request,
            Authentication authentication
    ) {

        GroupDetailsResponse response =
                groupService.createGroup(
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ---------------------------------------------------------
    // GET CURRENT USER'S GROUPS
    // ---------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<GroupSummaryResponse>> getMyGroups(
            Authentication authentication
    ) {

        List<GroupSummaryResponse> response =
                groupService.getGroupsForUser(
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // GET GROUP DETAILS
    // ---------------------------------------------------------

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> getGroupDetails(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        GroupDetailsResponse response =
                groupService.getGroupDetails(
                        groupId,
                        authentication.getName()
                );

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // UPDATE GROUP
    // ---------------------------------------------------------

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDetailsResponse> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequest request,
            Authentication authentication
    ) {

        GroupDetailsResponse response =
                groupService.updateGroup(
                        groupId,
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }

    // ---------------------------------------------------------
    // DELETE GROUP
    // ---------------------------------------------------------

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        groupService.deleteGroup(
                groupId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------
    // ADD MEMBER
    // ---------------------------------------------------------

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupMemberAddRequest request,
            Authentication authentication
    ) {

        GroupMemberResponse response =
                groupService.addMember(
                        groupId,
                        authentication.getName(),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ---------------------------------------------------------
    // REMOVE MEMBER
    // ---------------------------------------------------------

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication authentication
    ) {

        groupService.removeMember(
                groupId,
                userId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}