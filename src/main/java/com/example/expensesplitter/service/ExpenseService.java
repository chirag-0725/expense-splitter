package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.AddExpenseRequest;
import com.example.expensesplitter.dto.ExpenseResponse;
import com.example.expensesplitter.dto.ShareDto;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseGroup;
import com.example.expensesplitter.entity.ExpenseShare;
import com.example.expensesplitter.entity.GroupMember;
import com.example.expensesplitter.exception.InvalidExpenseException;
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
public class ExpenseService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    @Transactional
    public ExpenseResponse addExpense(Long groupId, AddExpenseRequest request, String currentUsername) {
        ExpenseGroup group = groupService.getGroupOrThrow(groupId);
        groupService.requireMembership(group, currentUsername);

        GroupMember payer = (request.getPaidByName() == null || request.getPaidByName().isBlank())
                ? groupService.resolveMemberForUsername(group, currentUsername)
                : groupService.resolveMemberOrThrow(group, request.getPaidByName());

        BigDecimal amount = request.getAmount().setScale(SCALE, RoundingMode.HALF_UP);

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setDescription(request.getDescription());
        expense.setAmount(amount);
        expense.setPaidBy(payer);
        expense.setSplitType(request.getSplitType());

        Map<GroupMember, BigDecimal> shareMap = computeShares(group, request, amount);

        for (Map.Entry<GroupMember, BigDecimal> entry : shareMap.entrySet()) {
            expense.addShare(new ExpenseShare(entry.getKey(), entry.getValue()));
        }

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId, String currentUsername) {
        ExpenseGroup group = groupService.getGroupOrThrow(groupId);
        groupService.requireMembership(group, currentUsername);

        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Map<GroupMember, BigDecimal> computeShares(ExpenseGroup group, AddExpenseRequest request, BigDecimal amount) {
        switch (request.getSplitType()) {
            case EQUAL:
                return computeEqualShares(group, request.getParticipants(), amount);
            case PERCENTAGE:
                return computePercentageShares(group, request.getPercentages(), amount);
            case CUSTOM:
                return computeCustomShares(group, request.getCustomShares(), amount);
            default:
                throw new InvalidExpenseException("Unsupported split type: " + request.getSplitType());
        }
    }

    private Map<GroupMember, BigDecimal> computeEqualShares(ExpenseGroup group, List<String> participantNames, BigDecimal amount) {
        List<GroupMember> participants;

        if (participantNames == null || participantNames.isEmpty()) {
            participants = new ArrayList<>(group.getMembers());
        } else {
            participants = participantNames.stream()
                    .map(name -> groupService.resolveMemberOrThrow(group, name))
                    .collect(Collectors.toList());
        }

        if (participants.isEmpty()) {
            throw new InvalidExpenseException("At least one participant is required for an equal split");
        }

        int n = participants.size();
        BigDecimal baseShare = amount.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.DOWN);
        BigDecimal distributed = baseShare.multiply(BigDecimal.valueOf(n));
        BigDecimal remainder = amount.subtract(distributed);
        BigDecimal cent = BigDecimal.valueOf(0.01).setScale(SCALE, RoundingMode.HALF_UP);

        Map<GroupMember, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            BigDecimal share = baseShare;
            if (remainder.signum() > 0) {
                share = share.add(cent);
                remainder = remainder.subtract(cent);
            }
            shares.put(participants.get(i), share);
        }
        return shares;
    }

    private Map<GroupMember, BigDecimal> computePercentageShares(ExpenseGroup group, Map<String, BigDecimal> percentages, BigDecimal amount) {
        if (percentages == null || percentages.isEmpty()) {
            throw new InvalidExpenseException("percentages map is required for a percentage split");
        }

        BigDecimal totalPercentage = percentages.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(HUNDRED) != 0) {
            throw new InvalidExpenseException("Percentages must sum to 100, got: " + totalPercentage);
        }

        Map<GroupMember, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(percentages.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, BigDecimal> entry = entries.get(i);
            GroupMember member = groupService.resolveMemberOrThrow(group, entry.getKey());

            BigDecimal share;
            if (i == entries.size() - 1) {
                share = amount.subtract(runningTotal).setScale(SCALE, RoundingMode.HALF_UP);
            } else {
                share = amount.multiply(entry.getValue())
                        .divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
                runningTotal = runningTotal.add(share);
            }
            shares.put(member, share);
        }
        return shares;
    }

    private Map<GroupMember, BigDecimal> computeCustomShares(ExpenseGroup group, Map<String, BigDecimal> customShares, BigDecimal amount) {
        if (customShares == null || customShares.isEmpty()) {
            throw new InvalidExpenseException("customShares map is required for a custom split");
        }

        BigDecimal total = customShares.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        if (total.compareTo(amount) != 0) {
            throw new InvalidExpenseException("Custom shares (" + total + ") must sum to the expense amount (" + amount + ")");
        }

        Map<GroupMember, BigDecimal> shares = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : customShares.entrySet()) {
            GroupMember member = groupService.resolveMemberOrThrow(group, entry.getKey());
            shares.put(member, entry.getValue().setScale(SCALE, RoundingMode.HALF_UP));
        }
        return shares;
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ShareDto> shareDtos = expense.getShares().stream()
                .map(s -> new ShareDto(s.getMember().getDisplayName(), s.getShareAmount()))
                .collect(Collectors.toList());

        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getPaidBy().getDisplayName(),
                expense.getSplitType(),
                expense.getCreatedAt(),
                shareDtos
        );
    }
}
