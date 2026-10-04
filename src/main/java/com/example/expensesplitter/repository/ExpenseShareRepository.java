package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.ExpenseShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    List<ExpenseShare> findByMemberUserUsernameAndExpenseCreatedAtBetween(
            String username, LocalDateTime start, LocalDateTime end);
}
