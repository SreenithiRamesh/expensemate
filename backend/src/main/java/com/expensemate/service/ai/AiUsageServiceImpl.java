package com.expensemate.service.ai;

import com.expensemate.entity.AiUsage;
import com.expensemate.entity.User;
import com.expensemate.exception.AiUsageLimitExceededException;
import com.expensemate.exception.ResourceNotFoundException;
import com.expensemate.repository.AiUsageRepository;
import com.expensemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AiUsageServiceImpl implements AiUsageService {

    private final AiUsageRepository aiUsageRepository;
    private final UserRepository userRepository;
    private final int dailyLimit;

    public AiUsageServiceImpl(
            AiUsageRepository aiUsageRepository,
            UserRepository userRepository,
            @Value("${gemini.daily-limit:10}") int dailyLimit
    ) {
        this.aiUsageRepository = aiUsageRepository;
        this.userRepository = userRepository;
        this.dailyLimit = dailyLimit;
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyLimit(Long userId) {

        int currentUsage =
                aiUsageRepository
                        .findByUserIdAndUsageDate(
                                userId,
                                LocalDate.now()
                        )
                        .map(AiUsage::getRequestCount)
                        .orElse(0);

        if (currentUsage >= dailyLimit) {
            throw new AiUsageLimitExceededException(
                    "Daily AI categorization limit reached"
            );
        }
    }

    @Override
    @Transactional
    public int recordSuccessfulRequest(Long userId) {

        LocalDate today = LocalDate.now();

        AiUsage usage =
                aiUsageRepository
                        .findByUserIdAndUsageDate(userId, today)
                        .orElseGet(() -> createUsage(userId, today));

        usage.increment();
        aiUsageRepository.save(usage);

        return Math.max(
                dailyLimit - usage.getRequestCount(),
                0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public int getRemainingRequests(Long userId) {

        int used =
                aiUsageRepository
                        .findByUserIdAndUsageDate(
                                userId,
                                LocalDate.now()
                        )
                        .map(AiUsage::getRequestCount)
                        .orElse(0);

        return Math.max(dailyLimit - used, 0);
    }

    private AiUsage createUsage(
            Long userId,
            LocalDate date
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );

        LocalDateTime now = LocalDateTime.now();

        return new AiUsage(
                user,
                date,
                0,
                now,
                now
        );
    }
}