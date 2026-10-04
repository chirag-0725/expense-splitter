package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.BalanceResponse;
import com.example.expensesplitter.dto.SettlementTransaction;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.ExpenseShare;
import com.example.expensesplitter.entity.GroupMember;
import com.example.expensesplitter.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private static final BigDecimal EPSILON = new BigDecimal("0.01");
    private static final int SCALE = 2;

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public SettlementService(ExpenseRepository expenseRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalances(Long groupId, String currentUsername) {
        ExpenseGroup group = groupService.getGroupOrThrow(groupId);
        groupService.requireMembership(group, currentUsername);

        Map<GroupMember, BigDecimal> net = computeNetBalances(group, groupId);

        return net.entrySet().stream()
                .map(e -> new BalanceResponse(e.getKey().getDisplayName(), e.getValue()))
                .sorted((a, b) -> b.getNetBalance().compareTo(a.getNetBalance()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SettlementTransaction> getSettlementPlan(Long groupId, String currentUsername) {
        ExpenseGroup group = groupService.getGroupOrThrow(groupId);
        groupService.requireMembership(group, currentUsername);

        Map<GroupMember, BigDecimal> net = computeNetBalances(group, groupId);
        return simplifyDebts(net);
    }

    private Map<GroupMember, BigDecimal> computeNetBalances(ExpenseGroup group, Long groupId) {
        Map<GroupMember, BigDecimal> net = new LinkedHashMap<>();
        for (GroupMember member : group.getMembers()) {
            net.put(member, BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
        }

        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);

        for (Expense expense : expenses) {
            net.merge(expense.getPaidBy(), expense.getAmount(), BigDecimal::add);
            for (ExpenseShare share : expense.getShares()) {
                net.merge(share.getMember(), share.getShareAmount().negate(), BigDecimal::add);
            }
        }

        net.replaceAll((member, bal) -> bal.setScale(SCALE, RoundingMode.HALF_UP));
        return net;
    }

    private List<SettlementTransaction> simplifyDebts(Map<GroupMember, BigDecimal> net) {
        List<Map.Entry<GroupMember, BigDecimal>> creditors = new ArrayList<>();
        List<Map.Entry<GroupMember, BigDecimal>> debtors = new ArrayList<>();

        for (Map.Entry<GroupMember, BigDecimal> entry : net.entrySet()) {
            if (entry.getValue().compareTo(EPSILON) > 0) {
                creditors.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().compareTo(EPSILON.negate()) < 0) {
                debtors.add(new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().abs()));
            }
        }

        creditors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        debtors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<SettlementTransaction> transactions = new ArrayList<>();
        int i = 0, j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<GroupMember, BigDecimal> debtor = debtors.get(i);
            Map.Entry<GroupMember, BigDecimal> creditor = creditors.get(j);

            BigDecimal settleAmount = debtor.getValue().min(creditor.getValue());

            transactions.add(new SettlementTransaction(
                    debtor.getKey().getDisplayName(),
                    creditor.getKey().getDisplayName(),
                    settleAmount.setScale(SCALE, RoundingMode.HALF_UP)
            ));

            debtor.setValue(debtor.getValue().subtract(settleAmount));
            creditor.setValue(creditor.getValue().subtract(settleAmount));

            if (debtor.getValue().compareTo(EPSILON) < 0) i++;
            if (creditor.getValue().compareTo(EPSILON) < 0) j++;
        }

        return transactions;
    }
}
