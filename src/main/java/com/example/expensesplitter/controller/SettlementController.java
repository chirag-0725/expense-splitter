package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.BalanceResponse;
import com.example.expensesplitter.dto.SettlementTransaction;
import com.example.expensesplitter.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public ResponseEntity<List<BalanceResponse>> getBalances(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(settlementService.getBalances(groupId, authentication.getName()));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<SettlementTransaction>> getSettlementPlan(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(settlementService.getSettlementPlan(groupId, authentication.getName()));
    }
}
