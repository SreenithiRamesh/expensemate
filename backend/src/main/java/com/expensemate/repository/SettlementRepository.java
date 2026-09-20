package com.expensemate.repository;

import com.expensemate.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository
        extends JpaRepository<Settlement, Long> {

    /*
     * Fetch every relationship required by SettlementResponse.
     *
     * This is important because createSettlement() performs the
     * recovery lookup after the write transaction has completed.
     */
    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.group
            JOIN FETCH settlement.fromUser
            JOIN FETCH settlement.toUser
            JOIN FETCH settlement.createdBy
            WHERE settlement.idempotencyKey = :idempotencyKey
            """)
    Optional<Settlement> findByIdempotencyKey(
            @Param("idempotencyKey")
            String idempotencyKey
    );

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.group
            JOIN FETCH settlement.fromUser
            JOIN FETCH settlement.toUser
            JOIN FETCH settlement.createdBy
            WHERE settlement.group.id = :groupId
            """)
    List<Settlement> findByGroup_Id(
            @Param("groupId")
            Long groupId
    );

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.group
            JOIN FETCH settlement.fromUser
            JOIN FETCH settlement.toUser
            JOIN FETCH settlement.createdBy
            WHERE settlement.group.id = :groupId
            ORDER BY settlement.settledAt DESC
            """)
    List<Settlement> findByGroup_IdOrderBySettledAtDesc(
            @Param("groupId")
            Long groupId
    );

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.group
            JOIN FETCH settlement.fromUser
            JOIN FETCH settlement.toUser
            JOIN FETCH settlement.createdBy
            WHERE settlement.id = :settlementId
              AND settlement.group.id = :groupId
            """)
    Optional<Settlement> findByIdAndGroup_Id(
            @Param("settlementId")
            Long settlementId,
            @Param("groupId")
            Long groupId
    );
}