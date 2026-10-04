package com.example.expensesplitter.dto;

import java.math.BigDecimal;

public class BalanceResponse {
    private String name;
    /** Positive = this member is owed money overall. Negative = this member owes money overall. */
    private BigDecimal netBalance;

    public BalanceResponse(String name, BigDecimal netBalance) {
        this.name = name;
        this.netBalance = netBalance;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }
}
