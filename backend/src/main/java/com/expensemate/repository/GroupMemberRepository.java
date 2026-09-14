package com.expensemate.repository;

import com.expensemate.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository
        extends JpaRepository<GroupMember, Long> {

    boolean existsByGroupIdAndUserId(
            Long groupId,
            Long userId
    );

    Optional<GroupMember> findByGroupIdAndUserId(
            Long groupId,
            Long userId
    );

    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(
            Long groupId
    );

    long countByGroupId(
            Long groupId
    );

    @Query("""
            SELECT gm
            FROM GroupMember gm
            JOIN FETCH gm.group g
            JOIN FETCH g.createdBy
            WHERE gm.user.id = :userId
            ORDER BY g.createdAt DESC
            """)
    List<GroupMember> findGroupsForUser(
            @Param("userId") Long userId
    );
}