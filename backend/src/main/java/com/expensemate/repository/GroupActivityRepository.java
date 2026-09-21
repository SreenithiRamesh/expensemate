package com.expensemate.repository;

import com.expensemate.entity.GroupActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupActivityRepository
        extends JpaRepository<GroupActivity, Long> {

    /*
     * ActivityService maps both group and actor information.
     *
     * Fetch both relationships in the original query so mapping
     * multiple activities does not execute one additional query
     * for every distinct actor.
     */
    @Query("""
            SELECT activity
            FROM GroupActivity activity
            JOIN FETCH activity.group
            JOIN FETCH activity.actor
            WHERE activity.group.id = :groupId
            ORDER BY activity.createdAt DESC
            """)
    List<GroupActivity>
    findByGroup_IdOrderByCreatedAtDesc(
            @Param("groupId")
            Long groupId
    );

    /*
     * Recent dashboard activity also requires group and actor
     * information. Loading both relationships here keeps the
     * query count bounded when the number of activities grows.
     */
    @Query("""
            SELECT activity
            FROM GroupActivity activity
            JOIN FETCH activity.group
            JOIN FETCH activity.actor
            WHERE EXISTS (
                SELECT member.id
                FROM GroupMember member
                WHERE member.group.id = activity.group.id
                  AND member.user.id = :userId
            )
            ORDER BY activity.createdAt DESC
            """)
    List<GroupActivity> findRecentActivityForUser(
            @Param("userId")
            Long userId,

            Pageable pageable
    );
}