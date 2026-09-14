package com.expensemate.controller;

import com.expensemate.dto.activity.GroupActivityResponse;
import com.expensemate.service.ActivityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(
            ActivityService activityService
    ) {
        this.activityService = activityService;
    }

    @GetMapping
    public List<GroupActivityResponse> getGroupActivity(
            @PathVariable Long groupId,
            Authentication authentication
    ) {

        return activityService.getGroupActivity(
                groupId,
                authentication.getName()
        );
    }
}