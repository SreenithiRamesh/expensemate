package com.expensemate.repository;

import com.expensemate.entity.GroupActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupActivityRepository
        extends JpaRepository<GroupActivity, Long> {

    List<GroupActivity>
    findByGroup_IdOrderByCreatedAtDesc(Long groupId);
}