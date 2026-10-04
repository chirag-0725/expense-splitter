package com.example.expensesplitter.dto;

import java.math.BigDecimal;

public class SettlementTransaction {
    private String fromName;
    private String toName;
    private BigDecimal amount;

    public SettlementTransaction(String fromName, String toName, BigDecimal amount) {
        this.fromName = fromName;
        this.toName = toName;
        this.amount = amount;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
