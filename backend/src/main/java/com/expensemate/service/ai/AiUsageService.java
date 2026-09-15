package com.expensemate.service.ai;

public interface AiUsageService {

    void verifyLimit(Long userId);

    int recordSuccessfulRequest(Long userId);

    int getRemainingRequests(Long userId);
}