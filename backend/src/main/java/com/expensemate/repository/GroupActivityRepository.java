package com.expensemate.repository;

import com.expensemate.entity.GroupActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupActivityRepository
        extends JpaRepository<GroupActivity, Long> {

    List<GroupActivity>
    findByGroup_IdOrderByCreatedAtDesc(Long groupId);

    @Query("""
            SELECT a
            FROM GroupActivity a
            WHERE EXISTS (
                SELECT gm.id
                FROM GroupMember gm
                WHERE gm.group.id = a.group.id
                  AND gm.user.id = :userId
            )
            ORDER BY a.createdAt DESC
            """)
    List<GroupActivity> findRecentActivityForUser(
            @Param("userId") Long userId,
            Pageable pageable
    );
}