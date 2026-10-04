package com.example.expensesplitter.repository;

import com.example.expensesplitter.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Expense> findByPaidByUserUsernameAndCreatedAtBetween(
            String username, LocalDateTime start, LocalDateTime end);
}
