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

    public Account() {
    }

    public Account(String id, String nickname, String type, String sortCode, String accountNumberMasked,
                   BigDecimal balance, BigDecimal availableBalance, String currency) {
        this.id = id;
        this.nickname = nickname;
        this.type = type;
        this.sortCode = sortCode;
        this.accountNumberMasked = accountNumberMasked;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.currency = currency;
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
}
