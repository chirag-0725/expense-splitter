package com.example.expensesplitter.service;

import com.example.expensesplitter.dto.GroupReportEntry;
import com.example.expensesplitter.dto.MonthlyReportResponse;
import com.example.expensesplitter.entity.Expense;
import com.example.expensesplitter.entity.ExpenseShare;
import com.example.expensesplitter.exception.InvalidExpenseException;
import com.example.expensesplitter.repository.ExpenseRepository;
import com.example.expensesplitter.repository.ExpenseShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final int SCALE = 2;

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;

    public ReportService(ExpenseRepository expenseRepository, ExpenseShareRepository expenseShareRepository) {
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
    }

    /**
     * How much the given (logged-in) user spent in a given calendar month, across
     * every group they belong to: what they fronted (paid), what they're personally
     * responsible for (their share of every expense), and the net of the two.
     * "Spent this month" is based on each expense's creation date.
     */
    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(String monthParam, String currentUsername) {
        YearMonth month = parseMonth(monthParam);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);

        List<Expense> paidExpenses =
                expenseRepository.findByPaidByUserUsernameAndCreatedAtBetween(currentUsername, start, end);
        List<ExpenseShare> myShares =
                expenseShareRepository.findByMemberUserUsernameAndExpenseCreatedAtBetween(currentUsername, start, end);

        Map<Long, GroupReportEntryBuilder> byGroup = new LinkedHashMap<>();
        BigDecimal totalPaid = zero();
        BigDecimal totalShare = zero();

        for (Expense e : paidExpenses) {
            totalPaid = totalPaid.add(e.getAmount());
            byGroup.computeIfAbsent(e.getGroup().getId(), id -> new GroupReportEntryBuilder(e.getGroup().getName()))
                    .paid = byGroup.get(e.getGroup().getId()).paid.add(e.getAmount());
        }

        for (ExpenseShare s : myShares) {
            Long groupId = s.getExpense().getGroup().getId();
            totalShare = totalShare.add(s.getShareAmount());
            byGroup.computeIfAbsent(groupId, id -> new GroupReportEntryBuilder(s.getExpense().getGroup().getName()))
                    .share = byGroup.get(groupId).share.add(s.getShareAmount());
        }

        List<GroupReportEntry> groupEntries = byGroup.entrySet().stream()
                .map(entry -> new GroupReportEntry(
                        entry.getKey(), entry.getValue().name, entry.getValue().paid, entry.getValue().share))
                .collect(java.util.stream.Collectors.toList());

        BigDecimal net = totalPaid.subtract(totalShare).setScale(SCALE, RoundingMode.HALF_UP);

        return new MonthlyReportResponse(month.toString(), totalPaid, totalShare, net, groupEntries);
    }

    private YearMonth parseMonth(String monthParam) {
        if (monthParam == null || monthParam.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(monthParam.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidExpenseException("month must be in yyyy-MM format, e.g. 2026-10");
        }
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static class GroupReportEntryBuilder {
        final String name;
        BigDecimal paid = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal share = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        GroupReportEntryBuilder(String name) {
            this.name = name;
        }
    }
}
