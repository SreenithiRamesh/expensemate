package com.expensemate.repository;

import com.expensemate.entity.MonthlyInsightCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyInsightCacheRepository
        extends JpaRepository<MonthlyInsightCache, Long> {

    Optional<MonthlyInsightCache>
    findByUserIdAndInsightMonth(
            Long userId,
            String insightMonth
    );

    void deleteByUserIdAndInsightMonth(
            Long userId,
            String insightMonth
    );
}