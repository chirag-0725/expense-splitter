package com.example.expensesplitter.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthlyReportResponse {
    /** "yyyy-MM" */
    private String month;
    /** Total amount this user fronted/paid for, across all groups, this month. */
    private BigDecimal totalPaid;
    /** Total amount this user is personally responsible for (their share), across all groups, this month. */
    private BigDecimal totalShare;
    /** totalPaid - totalShare: positive means net owed to them for the month, negative means they net owe. */
    private BigDecimal net;
    private List<GroupReportEntry> groups;

    public MonthlyReportResponse(String month, BigDecimal totalPaid, BigDecimal totalShare,
                                  BigDecimal net, List<GroupReportEntry> groups) {
        this.month = month;
        this.totalPaid = totalPaid;
        this.totalShare = totalShare;
        this.net = net;
        this.groups = groups;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public BigDecimal getTotalShare() {
        return totalShare;
    }

    public BigDecimal getNet() {
        return net;
    }

    public List<GroupReportEntry> getGroups() {
        return groups;
    }
}
