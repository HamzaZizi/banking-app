package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

public class Account {

    private String id;
    private String nickname;
    private String type;
    private String sortCode;
    private String accountNumberMasked;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private BigDecimal overdraftLimit;
    private String product;

    public Account() {
    }

    public Account(String id, String nickname, String type, String sortCode, String accountNumberMasked,
                   BigDecimal balance, BigDecimal availableBalance, String currency) {
        this(id, nickname, type, sortCode, accountNumberMasked, balance, availableBalance, currency,
                BigDecimal.ZERO, null);
    }

    public Account(String id, String nickname, String type, String sortCode, String accountNumberMasked,
                   BigDecimal balance, BigDecimal availableBalance, String currency,
                   BigDecimal overdraftLimit, String product) {
        this.id = id;
        this.nickname = nickname;
        this.type = type;
        this.sortCode = sortCode;
        this.accountNumberMasked = accountNumberMasked;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.currency = currency;
        this.overdraftLimit = overdraftLimit;
        this.product = product;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = overdraftLimit;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
}
