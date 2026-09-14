package com.expensemate.repository;

import com.expensemate.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository
        extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByIdempotencyKey(
            String idempotencyKey
    );

    List<Settlement> findByGroup_Id(
            Long groupId
    );

    List<Settlement> findByGroup_IdOrderBySettledAtDesc(
            Long groupId
    );

    Optional<Settlement> findByIdAndGroup_Id(
            Long settlementId,
            Long groupId
    );
}