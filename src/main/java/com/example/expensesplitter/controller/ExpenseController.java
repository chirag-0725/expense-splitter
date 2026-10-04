package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.AddExpenseRequest;
import com.example.expensesplitter.dto.ExpenseResponse;
import com.example.expensesplitter.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> addExpense(@PathVariable Long groupId,
                                                        @Valid @RequestBody AddExpenseRequest request,
                                                        Authentication authentication) {
        ExpenseResponse response = expenseService.addExpense(groupId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(@PathVariable Long groupId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(groupId, authentication.getName()));
    }
}
