package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

public class Transaction {

    private String id;
    private String accountId;
    private String date;
    private String description;
    private String category;
    private String type;          // DEBIT or CREDIT
    private BigDecimal amount;     // negative for debit, positive for credit
    private BigDecimal balanceAfter;

    public Transaction() {
    }

    public Transaction(String id, String accountId, String date, String description, String category,
                       String type, BigDecimal amount, BigDecimal balanceAfter) {
        this.id = id;
        this.accountId = accountId;
        this.date = date;
        this.description = description;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}
