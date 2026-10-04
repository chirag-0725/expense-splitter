package com.example.expensesplitter.dto;

import com.example.expensesplitter.entity.SplitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String paidBy;
    private SplitType splitType;
    private LocalDateTime createdAt;
    private List<ShareDto> shares;

    public ExpenseResponse(Long id, String description, BigDecimal amount, String paidBy,
                            SplitType splitType, LocalDateTime createdAt, List<ShareDto> shares) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.createdAt = createdAt;
        this.shares = shares;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ShareDto> getShares() {
        return shares;
    }
}
