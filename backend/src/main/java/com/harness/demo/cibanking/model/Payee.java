package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

/**
 * A saved payee the customer can send money to. Dummy demo data only.
 */
public class Payee {

    private String id;
    private String name;
    private String sortCode;
    private String accountNumberMasked;
    private String reference;
    private String lastPaidDate;   // nullable
    private BigDecimal lastAmount; // nullable

    public Payee() {
    }

    public Payee(String id, String name, String sortCode, String accountNumberMasked,
                 String reference, String lastPaidDate, BigDecimal lastAmount) {
        this.id = id;
        this.name = name;
        this.sortCode = sortCode;
        this.accountNumberMasked = accountNumberMasked;
        this.reference = reference;
        this.lastPaidDate = lastPaidDate;
        this.lastAmount = lastAmount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getLastPaidDate() {
        return lastPaidDate;
    }

    public void setLastPaidDate(String lastPaidDate) {
        this.lastPaidDate = lastPaidDate;
    }

    public BigDecimal getLastAmount() {
        return lastAmount;
    }

    public void setLastAmount(BigDecimal lastAmount) {
        this.lastAmount = lastAmount;
    }
}
