package com.example.expensesplitter.dto;

import java.math.BigDecimal;

public class ShareDto {
    private String name;
    private BigDecimal amount;

    public ShareDto(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
