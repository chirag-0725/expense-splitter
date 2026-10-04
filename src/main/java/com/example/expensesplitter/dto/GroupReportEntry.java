package com.example.expensesplitter.dto;

import java.math.BigDecimal;

public class GroupReportEntry {
    private Long groupId;
    private String groupName;
    private BigDecimal paid;
    private BigDecimal share;

    public GroupReportEntry(Long groupId, String groupName, BigDecimal paid, BigDecimal share) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.paid = paid;
        this.share = share;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getShare() {
        return share;
    }
}
