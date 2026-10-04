package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.ExpenseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    @Query("SELECT DISTINCT g FROM ExpenseGroup g JOIN g.members m WHERE m.user.id = :userId")
    List<ExpenseGroup> findAllByMemberUserId(@Param("userId") Long userId);
}
