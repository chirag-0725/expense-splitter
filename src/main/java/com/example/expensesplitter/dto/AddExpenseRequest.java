package com.example.expensesplitter.dto;

import com.example.expensesplitter.entity.SplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpenseRequest {

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    /** Optional: a member's display name. Defaults to the authenticated user if omitted. */
    private String paidByName;

    @NotNull(message = "splitType is required")
    private SplitType splitType;

    /** Used for EQUAL split: member names to split between. Defaults to all group members if empty. */
    private List<String> participants = new ArrayList<>();

    /** Used for CUSTOM split: member name -> exact amount. Must sum to `amount`. */
    private Map<String, BigDecimal> customShares = new HashMap<>();

    /** Used for PERCENTAGE split: member name -> percentage. Must sum to 100. */
    private Map<String, BigDecimal> percentages = new HashMap<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public Map<String, BigDecimal> getCustomShares() {
        return customShares;
    }

    public void setCustomShares(Map<String, BigDecimal> customShares) {
        this.customShares = customShares;
    }

    public Map<String, BigDecimal> getPercentages() {
        return percentages;
    }

    public void setPercentages(Map<String, BigDecimal> percentages) {
        this.percentages = percentages;
    }
}
